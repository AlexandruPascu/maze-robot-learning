package io.github.alexandrupascu.maze.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.TestMazes;
import io.github.alexandrupascu.maze.generate.Layout;
import io.github.alexandrupascu.maze.generate.MazeSpec;
import io.github.alexandrupascu.maze.generate.TargetPlacement;
import io.github.alexandrupascu.maze.sim.MazeEnvironment;
import io.github.alexandrupascu.maze.sim.Robot;
import io.github.alexandrupascu.maze.sim.RunResult;
import io.github.alexandrupascu.maze.sim.Runner;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class LearnerTest {

  @Test
  void everyLearnerFinishesEveryRunWithoutWalkingIntoWalls() {
    for (Supplier<Robot> learner : List.<Supplier<Robot>>of(QLearner::qLearning, () -> QLearner.dynaQ(10), LrtaStar::new)) {
      for (double loops : new double[] {0, 0.3}) {
        for (long seed = 0; seed < 8; seed++) {
          Maze maze = new MazeSpec(Layout.PRIM, 8, loops, TargetPlacement.RANDOM).generate(seed);
          for (RunResult run : runs(learner.get(), maze, seed, 5)) {
            assertTrue(run.reachedTarget());
            assertEquals(0, run.collisions(), "moves into seen walls are never chosen");
          }
        }
      }
    }
  }

  @Test
  void dynaQLearnsAShortestRoute() {
    for (Layout layout : Layout.values()) {
      for (long seed = 0; seed < 15; seed++) {
        Maze maze = new MazeSpec(layout, 7, 0.2, TargetPlacement.RANDOM).generate(seed);
        List<RunResult> runs = runs(QLearner.dynaQ(50), maze, seed, 40);
        assertEquals(shortest(maze), runs.get(runs.size() - 1).steps(), layout.id() + " seed " + seed);
      }
    }
  }

  @Test
  void qLearningGetsThereWithMoreRuns() {
    for (long seed = 0; seed < 10; seed++) {
      Maze maze = new MazeSpec(Layout.PRIM, 4, 0.2).generate(seed);
      List<RunResult> runs = runs(QLearner.qLearning(), maze, seed, 120);
      assertEquals(shortest(maze), runs.get(runs.size() - 1).steps(), "seed " + seed);
    }
  }

  @Test
  void replayingSpendsFewerStepsInTotal() {
    long plain = 0;
    long replaying = 0;
    for (long seed = 0; seed < 10; seed++) {
      Maze maze = new MazeSpec(Layout.PRIM, 8, 0.1).generate(seed);
      plain += total(runs(QLearner.qLearning(), maze, seed, 20));
      replaying += total(runs(QLearner.dynaQ(50), maze, seed, 20));
    }
    assertTrue(replaying < plain, "Dyna-Q " + replaying + " steps, Q-learning " + plain);
  }

  // Korf's guarantee rests on this: starting from an admissible estimate, updates never overshoot.
  @Test
  void lrtaStarEstimatesNeverExceedTheTrueDistance() {
    for (long seed = 0; seed < 10; seed++) {
      Maze maze = new MazeSpec(Layout.BACKTRACKER, 7, 0.25, TargetPlacement.RANDOM).generate(seed);
      LrtaStar robot = new LrtaStar();
      MazeEnvironment environment = new MazeEnvironment(maze);
      robot.beginMaze(environment.info(seed));
      for (int run = 0; run < 25; run++) {
        RunResult result = Runner.run(environment, robot, Runner.defaultStepLimit(environment), true);
        for (Position tile : result.trail()) {
          assertTrue(robot.estimate(tile) <= TestMazes.distance(maze, tile, maze.target()), "seed " + seed + " at " + tile);
        }
      }
    }
  }

  // Convergence is guaranteed eventually, not within a fixed number of runs; these mazes need up to 35.
  @Test
  void lrtaStarSettlesOnAShortestRoute() {
    for (long seed = 0; seed < 10; seed++) {
      Maze maze = new MazeSpec(Layout.BACKTRACKER, 7, 0.25, TargetPlacement.RANDOM).generate(seed);
      List<RunResult> runs = runs(new LrtaStar(), maze, seed, 100);
      for (RunResult run : runs.subList(90, 100)) {
        assertEquals(shortest(maze), run.steps(), "seed " + seed + ", run " + (run.run() + 1));
      }
    }
  }

  @Test
  void runsAreReproducibleForASeedAndTiesDependOnIt() {
    Maze maze = new MazeSpec(Layout.PRIM, 8, 0.2).generate(3);
    assertEquals(runs(QLearner.dynaQ(5), maze, 1, 3), runs(QLearner.dynaQ(5), maze, 1, 3));
    assertEquals(runs(new LrtaStar(), maze, 1, 3), runs(new LrtaStar(), maze, 1, 3));
    assertNotEquals(runs(QLearner.qLearning(), maze, 1, 1), runs(QLearner.qLearning(), maze, 2, 1));
  }

  @Test
  void dynaQNeedsAtLeastOneReplay() {
    assertThrows(IllegalArgumentException.class, () -> QLearner.dynaQ(0));
  }

  private static List<RunResult> runs(Robot robot, Maze maze, long seed, int count) {
    MazeEnvironment environment = new MazeEnvironment(maze);
    robot.beginMaze(environment.info(seed));
    List<RunResult> results = new ArrayList<>();
    for (int run = 0; run < count; run++) {
      results.add(Runner.run(environment, robot, Runner.defaultStepLimit(environment), true));
    }
    return results;
  }

  private static int shortest(Maze maze) {
    return TestMazes.distance(maze, maze.start(), maze.target());
  }

  private static long total(List<RunResult> runs) {
    return runs.stream().mapToLong(RunResult::steps).sum();
  }
}
