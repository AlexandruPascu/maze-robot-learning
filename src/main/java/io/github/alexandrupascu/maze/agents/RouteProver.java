package io.github.alexandrupascu.maze.agents;

import io.github.alexandrupascu.maze.Direction;
import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.Sight;
import io.github.alexandrupascu.maze.sim.MazeInfo;
import io.github.alexandrupascu.maze.sim.Observation;
import io.github.alexandrupascu.maze.sim.Robot;
import io.github.alexandrupascu.maze.sim.SearchWork;
import java.util.Arrays;

/**
 * Explores until it can prove which route is shortest, then takes it, in the spirit of Micromouse
 * robots that search a maze before racing it. It knows the maze is a grid of cells on odd tiles
 * with fixed pillars between them, so it reasons only about the walls between cells.
 *
 * <p>Treating unknown walls as open gives a lower bound on the shortest route from the start; using
 * only passages seen to be open gives an upper bound. When they meet, the known route is provably
 * shortest. Until then the robot inspects the nearest unknown wall that lies on some route as short
 * as the lower bound, preferring walls nearer the target, and it never steps onto the target. Every
 * run after a completed first run is therefore a shortest route.
 */
public final class RouteProver implements Robot, SearchWork {
  private static final byte UNKNOWN = 0;
  private static final byte OPEN = 1;
  private static final byte WALL = 2;
  private static final Heading[] HEADINGS = Heading.values();
  private static final int FAR = Integer.MAX_VALUE / 4;

  private int columns;
  private int start;
  private int goal;
  private int[] neighbours = new int[0];
  private byte[] walls = new byte[0];
  private int[] fromStart = new int[0];
  private int[] toTarget = new int[0];
  private int[] distance = new int[0];
  private int[] parent = new int[0];
  private int[] queue = new int[0];
  private boolean boundsStale;
  private boolean knownStale;
  private int lowerBound;
  private int upperBound;
  private Heading crossing;
  private long expansions;

  @Override
  public void beginMaze(MazeInfo info) {
    if (info.width() % 2 == 0 || info.height() % 2 == 0 || !isCell(info.start()) || !isCell(info.target())) {
      throw new IllegalArgumentException("the route prover needs a maze of cells on odd tiles");
    }
    columns = (info.width() - 1) / 2;
    int rows = (info.height() - 1) / 2;
    int cells = columns * rows;
    neighbours = new int[cells * 4];
    for (int cell = 0; cell < cells; cell++) {
      for (Heading heading : HEADINGS) {
        int x = cell % columns + heading.dx();
        int y = cell / columns + heading.dy();
        neighbours[cell * 4 + heading.ordinal()] = x < 0 || y < 0 || x >= columns || y >= rows ? -1 : y * columns + x;
      }
    }
    start = cellOf(info.start());
    goal = cellOf(info.target());
    walls = new byte[cells * 4];
    fromStart = new int[cells];
    toTarget = new int[cells];
    distance = new int[cells];
    parent = new int[cells];
    queue = new int[cells];
    boundsStale = true;
    knownStale = true;
    expansions = 0;
  }

  /** Cells taken off a breadth-first search queue since the maze began. */
  @Override
  public long expansions() {
    return expansions;
  }

  /** Whether the robot has proved that its known route is a shortest route. */
  public boolean proven() {
    return !boundsStale && !knownStale && upperBound == lowerBound;
  }

  @Override
  public Direction act(Observation observation) {
    Position here = observation.position();
    if (!isCell(here)) {
      // Halfway between two cells: the pillars beside it are always wall, so keep going.
      return observation.heading().directionTo(crossing);
    }
    int cell = cellOf(here);
    observe(cell, observation);
    crossing = HEADINGS[decide(cell)];
    return observation.heading().directionTo(crossing);
  }

  private void observe(int cell, Observation observation) {
    for (Heading heading : HEADINGS) {
      int next = neighbours[cell * 4 + heading.ordinal()];
      boolean open = observation.sight(heading) != Sight.WALL;
      if (next < 0) {
        if (open) {
          throw new IllegalStateException("the outer wall has a gap: not a maze of cells");
        }
        continue;
      }
      int edge = cell * 4 + heading.ordinal();
      if (walls[edge] != UNKNOWN) {
        continue;
      }
      byte state = open ? OPEN : WALL;
      walls[edge] = state;
      walls[next * 4 + (heading.ordinal() + 2) % 4] = state;
      if (open) {
        knownStale = true;
      } else if (!boundsStale && onShortestCandidate(cell, next)) {
        // A wall on no candidate route changes neither the lower bound nor which routes are candidates.
        boundsStale = true;
      }
    }
  }

  // Returns the heading to leave this cell by.
  private int decide(int cell) {
    if (boundsStale) {
      search(fromStart, start, false, -1, false);
      search(toTarget, goal, false, -1, false);
      lowerBound = fromStart[goal];
      boundsStale = false;
    }
    if (knownStale) {
      search(distance, start, true, -1, false);
      upperBound = distance[goal];
      knownStale = false;
    }
    if (upperBound == lowerBound) {
      search(distance, goal, true, -1, false);
      for (int h = 0; h < 4; h++) {
        int next = neighbours[cell * 4 + h];
        if (next >= 0 && walls[cell * 4 + h] == OPEN && distance[next] == distance[cell] - 1) {
          return h;
        }
      }
      throw new IllegalStateException("the proven route is not reachable from cell " + cell);
    }
    int inspect = search(distance, cell, false, goal, true);
    if (inspect < 0) {
      throw new IllegalStateException("no wall left to inspect, yet the route is not proven");
    }
    int next = inspect;
    while (parent[next] != cell) {
      next = parent[next];
    }
    for (int h = 0; h < 4; h++) {
      if (neighbours[cell * 4 + h] == next) {
        return h;
      }
    }
    throw new IllegalStateException("cells " + cell + " and " + next + " are not adjacent");
  }

  /**
   * Breadth-first distances and parents from {@code source} through open edges, or also unknown ones
   * unless {@code knownOnly}, never passing through {@code avoid}. With {@code findInspection} it
   * stops at the nearest cell bordering an unknown wall on a candidate route and returns it (nearer
   * the target wins ties), or -1 if there is none; otherwise it returns -1 after a full search.
   */
  private int search(int[] distances, int source, boolean knownOnly, int avoid, boolean findInspection) {
    Arrays.fill(distances, FAR);
    int head = 0;
    int tail = 0;
    int found = -1;
    distances[source] = 0;
    parent[source] = source;
    queue[tail++] = source;
    while (head < tail) {
      int cell = queue[head++];
      expansions++;
      if (findInspection) {
        if (found >= 0 && distances[cell] > distances[found]) {
          break;
        }
        if (cell != avoid && bordersCandidate(cell)
            && (found < 0 || toTarget[cell] < toTarget[found] || toTarget[cell] == toTarget[found] && cell < found)) {
          found = cell;
        }
      }
      if (cell == avoid) {
        continue;
      }
      for (int h = 0; h < 4; h++) {
        int next = neighbours[cell * 4 + h];
        if (next < 0 || distances[next] < FAR) {
          continue;
        }
        byte state = walls[cell * 4 + h];
        if (state == WALL || knownOnly && state != OPEN) {
          continue;
        }
        distances[next] = distances[cell] + 1;
        parent[next] = cell;
        queue[tail++] = next;
      }
    }
    return found;
  }

  private boolean bordersCandidate(int cell) {
    for (int h = 0; h < 4; h++) {
      int next = neighbours[cell * 4 + h];
      if (next >= 0 && walls[cell * 4 + h] == UNKNOWN && onShortestCandidate(cell, next)) {
        return true;
      }
    }
    return false;
  }

  // Whether the wall between a and b lies on a start-to-target route as short as the lower bound.
  private boolean onShortestCandidate(int a, int b) {
    return fromStart[a] + 1 + toTarget[b] == lowerBound || fromStart[b] + 1 + toTarget[a] == lowerBound;
  }

  private int cellOf(Position tile) {
    return (tile.y() - 1) / 2 * columns + (tile.x() - 1) / 2;
  }

  private static boolean isCell(Position tile) {
    return tile.x() % 2 == 1 && tile.y() % 2 == 1;
  }
}
