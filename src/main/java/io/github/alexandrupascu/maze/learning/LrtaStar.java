package io.github.alexandrupascu.maze.learning;

import io.github.alexandrupascu.maze.Direction;
import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.Sight;
import io.github.alexandrupascu.maze.sim.MazeInfo;
import io.github.alexandrupascu.maze.sim.Observation;
import io.github.alexandrupascu.maze.sim.Robot;
import java.util.Random;

/**
 * Learning Real-Time A* (Korf, 1990). It keeps an estimate of every tile's distance to the target,
 * starting from the Manhattan distance, which never overestimates. Each step it moves to the open
 * neighbour with the lowest one-step-plus-estimate and raises its own tile's estimate to that
 * value. Estimates only ever grow towards the true distances, and because they are kept between
 * runs, repeated runs converge on a shortest route. Ties are broken at random.
 */
public final class LrtaStar implements Robot {
  private static final Heading[] HEADINGS = Heading.values();

  private int width;
  private int[] estimate = new int[0];
  private Random random = new Random(0);

  @Override
  public void beginMaze(MazeInfo info) {
    width = info.width();
    estimate = new int[width * info.height()];
    for (int tile = 0; tile < estimate.length; tile++) {
      estimate[tile] = new Position(tile % width, tile / width).manhattanDistance(info.target());
    }
    random = new Random(info.seed());
  }

  @Override
  public Direction act(Observation observation) {
    Position here = observation.position();
    int best = Integer.MAX_VALUE;
    int choices = 0;
    Heading move = null;
    for (Heading heading : HEADINGS) {
      if (observation.sight(heading) == Sight.WALL) {
        continue;
      }
      int cost = 1 + estimate[index(here.step(heading))];
      if (cost < best) {
        best = cost;
        choices = 1;
        move = heading;
      } else if (cost == best && random.nextInt(++choices) == 0) {
        move = heading;
      }
    }
    if (move == null) {
      throw new IllegalStateException("walled in at " + here);
    }
    int tile = index(here);
    estimate[tile] = Math.max(estimate[tile], best);
    return observation.heading().directionTo(move);
  }

  /** The current estimate of a tile's distance to the target. */
  int estimate(Position tile) {
    return estimate[index(tile)];
  }

  private int index(Position tile) {
    return tile.y() * width + tile.x();
  }
}
