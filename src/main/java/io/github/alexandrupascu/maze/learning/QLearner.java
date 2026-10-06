package io.github.alexandrupascu.maze.learning;

import io.github.alexandrupascu.maze.Direction;
import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.Sight;
import io.github.alexandrupascu.maze.sim.MazeInfo;
import io.github.alexandrupascu.maze.sim.Observation;
import io.github.alexandrupascu.maze.sim.Robot;
import io.github.alexandrupascu.maze.sim.StepResult;
import java.util.Arrays;
import java.util.Random;

/**
 * Tabular Q-learning over tiles and the four compass moves, keeping what it learned between runs
 * on the same maze. It is never told where the target is: it only learns from the cost of its own
 * steps.
 *
 * <p>Every step is worth -1 and reaching the target ends the run, so a move's value is minus the
 * steps it leads to. The maze is deterministic, so a learning rate of 1 is exact. Untried moves
 * start at 0, above any tried move, so the robot keeps trying new moves until none look better:
 * exploration by optimistic initialisation, without randomness. Ties are broken at random.
 *
 * <p>With planning steps it becomes Dyna-Q (Sutton, 1990): it also remembers where each move led
 * and, after every real step, replays that many remembered moves to spread what it learned.
 */
public final class QLearner implements Robot {
  private static final Heading[] HEADINGS = Heading.values();

  private final int planningSteps;
  private int width;
  private int target;
  private int[] value = new int[0];
  private int[] leadsTo = new int[0];
  private boolean[] wall = new boolean[0];
  private int[] experience = new int[0];
  private int experienced;
  private Random random = new Random(0);
  private int lastMove = -1;

  private QLearner(int planningSteps) {
    this.planningSteps = planningSteps;
  }

  /** Plain Q-learning: learns only from the steps it takes. */
  public static QLearner qLearning() {
    return new QLearner(0);
  }

  /** Dyna-Q: also replays {@code planningSteps} remembered moves after every real step. */
  public static QLearner dynaQ(int planningSteps) {
    if (planningSteps < 1) {
      throw new IllegalArgumentException("Dyna-Q needs at least one planning step");
    }
    return new QLearner(planningSteps);
  }

  @Override
  public void beginMaze(MazeInfo info) {
    width = info.width();
    target = index(info.target());
    int moves = width * info.height() * 4;
    value = new int[moves];
    leadsTo = new int[moves];
    wall = new boolean[moves];
    experience = new int[moves];
    Arrays.fill(leadsTo, -1);
    experienced = 0;
    random = new Random(info.seed());
  }

  @Override
  public Direction act(Observation observation) {
    int tile = index(observation.position());
    see(tile, observation);
    int best = Integer.MIN_VALUE;
    int choices = 0;
    int move = -1;
    for (Heading heading : HEADINGS) {
      int candidate = tile * 4 + heading.ordinal();
      if (wall[candidate]) {
        continue;
      }
      if (value[candidate] > best) {
        best = value[candidate];
        choices = 1;
        move = candidate;
      } else if (value[candidate] == best && random.nextInt(++choices) == 0) {
        move = candidate;
      }
    }
    if (move < 0) {
      throw new IllegalStateException("walled in at " + observation.position());
    }
    lastMove = move;
    return observation.heading().directionTo(HEADINGS[move % 4]);
  }

  @Override
  public void afterStep(StepResult result) {
    int next = index(result.observation().position());
    see(next, result.observation());
    if (leadsTo[lastMove] < 0) {
      leadsTo[lastMove] = next;
      experience[experienced++] = lastMove;
    }
    value[lastMove] = backup(next);
    for (int i = 0; i < planningSteps; i++) {
      int move = experience[random.nextInt(experienced)];
      value[move] = backup(leadsTo[move]);
    }
  }

  // One step's cost plus the best value from where the move leads; the target is worth nothing more.
  private int backup(int next) {
    if (next == target) {
      return -1;
    }
    int best = Integer.MIN_VALUE;
    for (int h = 0; h < 4; h++) {
      if (!wall[next * 4 + h]) {
        best = Math.max(best, value[next * 4 + h]);
      }
    }
    return best == Integer.MIN_VALUE ? best : best - 1;
  }

  private void see(int tile, Observation observation) {
    for (Heading heading : HEADINGS) {
      wall[tile * 4 + heading.ordinal()] = observation.sight(heading) == Sight.WALL;
    }
  }

  private int index(Position tile) {
    return tile.y() * width + tile.x();
  }
}
