package io.github.alexandrupascu.maze.learning;

import io.github.alexandrupascu.maze.Direction;
import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.Sight;
import io.github.alexandrupascu.maze.sim.MazeInfo;
import io.github.alexandrupascu.maze.sim.Observation;
import io.github.alexandrupascu.maze.sim.Robot;
import java.util.Arrays;

/**
 * Explores a maze of cells by repeatedly walking to the frontier cell with the lowest score under an
 * {@link ExplorationPolicy}, looking at its unknown walls, and choosing again. The target counts as a
 * candidate once a known route reaches it, scored by its travel alone. Later runs take the shortest
 * route through known passages, like the map planner. Like the route prover, it knows the maze is a
 * grid of cells on odd tiles with fixed pillars between them.
 */
public final class FrontierExplorer implements Robot {
  private static final byte UNKNOWN = 0;
  private static final byte OPEN = 1;
  private static final byte WALL = 2;
  private static final Heading[] HEADINGS = Heading.values();
  private static final int FAR = Integer.MAX_VALUE / 4;

  private final ExplorationPolicy policy;
  private int columns;
  private int goal;
  private int[] neighbours = new int[0];
  private byte[] walls = new byte[0];
  private int[] toTarget = new int[0];
  private int[] travel = new int[0];
  private int[] parent = new int[0];
  private int[] queue = new int[0];
  private int[] frontierSince = new int[0];
  private int decisions;
  private boolean exploring;
  private Heading crossing;

  public FrontierExplorer(ExplorationPolicy policy) {
    this.policy = policy;
  }

  @Override
  public void beginMaze(MazeInfo info) {
    if (info.width() % 2 == 0 || info.height() % 2 == 0 || !isCell(info.start()) || !isCell(info.target())) {
      throw new IllegalArgumentException("the frontier explorer needs a maze of cells on odd tiles");
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
    goal = cellOf(info.target());
    walls = new byte[cells * 4];
    toTarget = new int[cells];
    travel = new int[cells];
    parent = new int[cells];
    queue = new int[cells];
    frontierSince = new int[cells];
    Arrays.fill(frontierSince, -1);
    decisions = 0;
  }

  @Override
  public void beginRun(int run) {
    exploring = run == 0;
  }

  @Override
  public Direction act(Observation observation) {
    Position here = observation.position();
    if (!isCell(here)) {
      // Halfway between two cells: the pillars beside it are always wall, so keep going.
      return observation.heading().directionTo(crossing);
    }
    int cell = cellOf(here);
    for (Heading heading : HEADINGS) {
      int next = neighbours[cell * 4 + heading.ordinal()];
      if (next >= 0 && walls[cell * 4 + heading.ordinal()] == UNKNOWN) {
        byte state = observation.sight(heading) == Sight.WALL ? WALL : OPEN;
        walls[cell * 4 + heading.ordinal()] = state;
        walls[next * 4 + (heading.ordinal() + 2) % 4] = state;
      }
    }
    int next = exploring ? explore(cell) : retrace(cell);
    for (int h = 0; h < 4; h++) {
      if (neighbours[cell * 4 + h] == next) {
        crossing = HEADINGS[h];
        return observation.heading().directionTo(crossing);
      }
    }
    throw new IllegalStateException("cells " + cell + " and " + next + " are not adjacent");
  }

  // The next cell towards the best-scoring frontier, or towards the target once it scores best.
  private int explore(int cell) {
    decisions++;
    search(toTarget, goal, false);
    search(travel, cell, true);
    int targetX = goal % columns;
    int targetY = goal / columns;
    int best = -1;
    double bestScore = Double.POSITIVE_INFINITY;
    for (int candidate = 0; candidate < travel.length; candidate++) {
      if (travel[candidate] >= FAR) {
        continue;
      }
      double score;
      if (candidate == goal) {
        score = travel[candidate];
      } else {
        int unknown = 0;
        int towards = 0;
        int x = candidate % columns;
        int y = candidate / columns;
        int manhattan = Math.abs(x - targetX) + Math.abs(y - targetY);
        for (int h = 0; h < 4; h++) {
          int next = neighbours[candidate * 4 + h];
          if (next >= 0 && walls[candidate * 4 + h] == UNKNOWN) {
            unknown++;
            if (Math.abs(next % columns - targetX) + Math.abs(next / columns - targetY) < manhattan) {
              towards++;
            }
          }
        }
        if (unknown == 0) {
          continue;
        }
        if (frontierSince[candidate] < 0) {
          frontierSince[candidate] = decisions;
        }
        score = policy.score(travel[candidate], toTarget[candidate], manhattan, unknown,
            decisions - frontierSince[candidate], towards);
      }
      if (score < bestScore) {
        bestScore = score;
        best = candidate;
      }
    }
    if (best < 0) {
      throw new IllegalStateException("nothing left to explore and no route to the target");
    }
    int next = best;
    while (parent[next] != cell) {
      next = parent[next];
    }
    return next;
  }

  // Later runs: one step along a shortest route through known passages.
  private int retrace(int cell) {
    search(travel, goal, true);
    for (int h = 0; h < 4; h++) {
      int next = neighbours[cell * 4 + h];
      if (next >= 0 && walls[cell * 4 + h] == OPEN && travel[next] == travel[cell] - 1) {
        return next;
      }
    }
    throw new IllegalStateException("no known route from cell " + cell);
  }

  // Breadth-first distances and parents from `source`, through open edges or also unknown ones.
  private void search(int[] distance, int source, boolean knownOnly) {
    Arrays.fill(distance, FAR);
    int head = 0;
    int tail = 0;
    distance[source] = 0;
    parent[source] = source;
    queue[tail++] = source;
    while (head < tail) {
      int cell = queue[head++];
      for (int h = 0; h < 4; h++) {
        int next = neighbours[cell * 4 + h];
        if (next < 0 || distance[next] < FAR) {
          continue;
        }
        byte state = walls[cell * 4 + h];
        if (state == WALL || knownOnly && state != OPEN) {
          continue;
        }
        distance[next] = distance[cell] + 1;
        parent[next] = cell;
        queue[tail++] = next;
      }
    }
  }

  private int cellOf(Position tile) {
    return (tile.y() - 1) / 2 * columns + (tile.x() - 1) / 2;
  }

  private static boolean isCell(Position tile) {
    return tile.x() % 2 == 1 && tile.y() % 2 == 1;
  }
}
