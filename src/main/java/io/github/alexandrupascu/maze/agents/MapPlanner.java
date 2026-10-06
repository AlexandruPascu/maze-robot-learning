package io.github.alexandrupascu.maze.agents;

import io.github.alexandrupascu.maze.Direction;
import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.Sight;
import io.github.alexandrupascu.maze.search.DStarLite;
import io.github.alexandrupascu.maze.sim.MazeInfo;
import io.github.alexandrupascu.maze.sim.Observation;
import io.github.alexandrupascu.maze.sim.Robot;
import io.github.alexandrupascu.maze.sim.SearchWork;
import java.util.Arrays;

/**
 * Builds a map from what it senses and plans shortest routes on it, keeping the map between runs.
 *
 * <p>The first run explores optimistically: unknown tiles are assumed open (planning under the
 * freespace assumption), and D* Lite repairs the route incrementally whenever a newly seen wall
 * changes it. Later runs exploit what was learned and only plan through tiles already seen to be
 * open, so the second run never wanders into unexplored territory.
 */
public final class MapPlanner implements Robot, SearchWork {
  private static final byte UNKNOWN = 0;
  private static final byte OPEN = 1;
  private static final byte WALL = 2;
  private static final Heading[] HEADINGS = Heading.values();

  private final boolean incremental;
  private int width;
  private int height;
  private Position target;
  private int goal;
  private byte[] known = new byte[0];
  private int[] parent = new int[0];
  private int[] queue = new int[0];
  private int[] route = new int[0];
  private boolean[] onRoute = new boolean[0];
  private int routeLength;
  private int routeIndex;
  private DStarLite planner;
  private long plannerExpansions;
  private long expansions;
  private boolean exploring;

  public MapPlanner() {
    this(true);
  }

  private MapPlanner(boolean incremental) {
    this.incremental = incremental;
  }

  /**
   * The same robot without D* Lite: it searches again from scratch, breadth first, whenever a wall
   * blocks its route. Kept as the reference for measuring how much search work D* Lite saves.
   */
  public static MapPlanner replanningFromScratch() {
    return new MapPlanner(false);
  }

  @Override
  public void beginMaze(MazeInfo info) {
    width = info.width();
    height = info.height();
    target = info.target();
    goal = index(target);
    int tiles = width * height;
    known = new byte[tiles];
    parent = new int[tiles];
    queue = new int[tiles];
    route = new int[tiles];
    onRoute = new boolean[tiles];
    routeLength = 0;
    routeIndex = 0;
    planner = null;
    plannerExpansions = 0;
    expansions = 0;
  }

  @Override
  public void beginRun(int run) {
    exploring = run == 0;
    clearRoute();
    if (planner != null) {
      plannerExpansions += planner.expansions();
      planner = null;
    }
  }

  @Override
  public long expansions() {
    return expansions + plannerExpansions + (planner == null ? 0 : planner.expansions());
  }

  @Override
  public Direction act(Observation observation) {
    Position here = observation.position();
    int current = index(here);
    boolean incrementalRun = exploring && incremental;
    if (incrementalRun) {
      if (planner == null) {
        planner = new DStarLite(width, height, here, target);
      }
      planner.moveTo(here);
    }
    known[current] = OPEN;
    boolean blocked = false;
    for (Heading heading : HEADINGS) {
      Position next = here.step(heading);
      if (next.x() < 0 || next.y() < 0 || next.x() >= width || next.y() >= height) {
        continue;
      }
      int tile = index(next);
      if (known[tile] != UNKNOWN) {
        continue;
      }
      boolean wall = observation.sight(heading) == Sight.WALL;
      known[tile] = wall ? WALL : OPEN;
      if (wall && incrementalRun) {
        planner.block(next);
      }
      blocked |= wall && onRoute[tile];
    }
    Position step;
    if (incrementalRun) {
      step = planner.next();
    } else {
      if (blocked || routeIndex >= routeLength) {
        replan(current);
      }
      int next = route[routeIndex++];
      onRoute[next] = false;
      step = new Position(next % width, next / width);
    }
    return observation.heading().directionTo(Heading.between(here, step));
  }

  // Exploring assumes unknown tiles are open; later runs use only tiles seen to be open.
  private void replan(int from) {
    clearRoute();
    if (!search(from, !exploring) && !(!exploring && search(from, false))) {
      throw new IllegalStateException("no route to the target is consistent with the map");
    }
  }

  // Breadth-first search over passable tiles; neighbours are tried in N, E, S, W order.
  private boolean search(int from, boolean knownOnly) {
    Arrays.fill(parent, -1);
    int head = 0;
    int tail = 0;
    queue[tail++] = from;
    parent[from] = from;
    while (head < tail) {
      int current = queue[head++];
      expansions++;
      if (current == goal) {
        storeRoute(from);
        return true;
      }
      int x = current % width;
      int y = current / width;
      for (Heading heading : HEADINGS) {
        int nx = x + heading.dx();
        int ny = y + heading.dy();
        if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
          continue;
        }
        int next = ny * width + nx;
        boolean passable = knownOnly ? known[next] == OPEN : known[next] != WALL;
        if (parent[next] == -1 && passable) {
          parent[next] = current;
          queue[tail++] = next;
        }
      }
    }
    return false;
  }

  private void storeRoute(int from) {
    routeLength = 0;
    for (int tile = goal; tile != from; tile = parent[tile]) {
      route[routeLength++] = tile;
    }
    for (int i = 0, j = routeLength - 1; i < j; i++, j--) {
      int swap = route[i];
      route[i] = route[j];
      route[j] = swap;
    }
    for (int i = 0; i < routeLength; i++) {
      onRoute[route[i]] = true;
    }
    routeIndex = 0;
  }

  private void clearRoute() {
    for (int i = routeIndex; i < routeLength; i++) {
      onRoute[route[i]] = false;
    }
    routeLength = 0;
    routeIndex = 0;
  }

  private int index(Position position) {
    return position.y() * width + position.x();
  }
}
