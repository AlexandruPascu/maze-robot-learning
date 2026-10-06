package io.github.alexandrupascu.maze.agents;

import io.github.alexandrupascu.maze.Direction;
import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.Sight;
import io.github.alexandrupascu.maze.sim.MazeInfo;
import io.github.alexandrupascu.maze.sim.Observation;
import io.github.alexandrupascu.maze.sim.Robot;
import java.util.Arrays;

/**
 * Builds a map from what it senses and plans shortest routes on it, keeping the map between runs.
 *
 * <p>The first run explores optimistically: unknown tiles are assumed open, and the robot replans
 * whenever it discovers a wall on its planned route (planning under the freespace assumption).
 * Later runs exploit what was learned and only plan through tiles already seen to be open, so the
 * second run never wanders into unexplored territory.
 */
public final class MapPlanner implements Robot {
  private static final byte UNKNOWN = 0;
  private static final byte OPEN = 1;
  private static final byte WALL = 2;
  private static final Heading[] HEADINGS = Heading.values();

  private int width;
  private int height;
  private int goal;
  private byte[] known = new byte[0];
  private int[] parent = new int[0];
  private int[] queue = new int[0];
  private int[] plan = new int[0];
  private boolean[] onPlan = new boolean[0];
  private int planLength;
  private int planIndex;
  private boolean exploit;

  @Override
  public void beginMaze(MazeInfo info) {
    width = info.width();
    height = info.height();
    goal = index(info.target());
    int tiles = width * height;
    known = new byte[tiles];
    parent = new int[tiles];
    queue = new int[tiles];
    plan = new int[tiles];
    onPlan = new boolean[tiles];
    planLength = 0;
    planIndex = 0;
  }

  @Override
  public void beginRun(int run) {
    exploit = run > 0;
    clearPlan();
  }

  @Override
  public Direction act(Observation observation) {
    Position here = observation.position();
    int current = index(here);
    known[current] = OPEN;
    boolean blocked = false;
    for (Heading heading : HEADINGS) {
      Position next = here.step(heading);
      if (next.x() < 0 || next.y() < 0 || next.x() >= width || next.y() >= height) {
        continue;
      }
      int tile = index(next);
      byte seen = observation.sight(heading) == Sight.WALL ? WALL : OPEN;
      if (known[tile] != seen) {
        known[tile] = seen;
        blocked |= seen == WALL && onPlan[tile];
      }
    }
    if (blocked || planIndex >= planLength) {
      replan(current);
    }
    int next = plan[planIndex++];
    onPlan[next] = false;
    Heading toward = Heading.between(here, new Position(next % width, next / width));
    return observation.heading().directionTo(toward);
  }

  private void replan(int from) {
    clearPlan();
    if (!search(from, exploit) && !(exploit && search(from, false))) {
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
      if (current == goal) {
        storePlan(from);
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

  private void storePlan(int from) {
    planLength = 0;
    for (int tile = goal; tile != from; tile = parent[tile]) {
      plan[planLength++] = tile;
    }
    for (int i = 0, j = planLength - 1; i < j; i++, j--) {
      int swap = plan[i];
      plan[i] = plan[j];
      plan[j] = swap;
    }
    for (int i = 0; i < planLength; i++) {
      onPlan[plan[i]] = true;
    }
    planIndex = 0;
  }

  private void clearPlan() {
    for (int i = planIndex; i < planLength; i++) {
      onPlan[plan[i]] = false;
    }
    planLength = 0;
    planIndex = 0;
  }

  private int index(Position position) {
    return position.y() * width + position.x();
  }
}
