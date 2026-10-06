package io.github.alexandrupascu.maze.learning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.TestMazes;
import io.github.alexandrupascu.maze.bench.Benchmark;
import io.github.alexandrupascu.maze.bench.Seeds;
import io.github.alexandrupascu.maze.generate.Layout;
import io.github.alexandrupascu.maze.generate.MazeSpec;
import io.github.alexandrupascu.maze.generate.TargetPlacement;
import io.github.alexandrupascu.maze.sim.MazeEnvironment;
import io.github.alexandrupascu.maze.sim.RunResult;
import io.github.alexandrupascu.maze.sim.Runner;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExplorationTest {
  private static final CrossEntropyMethod.Settings TINY = new CrossEntropyMethod.Settings(3, 6, 2, 2, 6, 5);

  @Test
  void policiesRoundTripThroughTheirTextFormat() {
    ExplorationPolicy policy = ExplorationPolicy.of(1, 2.5, -0.75, 0.125, 0.0625, -3);
    assertEquals(policy, ExplorationPolicy.parse("# comment\n" + policy.format()));
    assertEquals(ExplorationPolicy.freespace(), ExplorationPolicy.parse(ExplorationPolicy.freespace().format()));
  }

  @Test
  void malformedPoliciesAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> ExplorationPolicy.of(2, 1, 0, 0, 0, 0), "travel weight must be 1");
    assertThrows(IllegalArgumentException.class, () -> ExplorationPolicy.of(1, 1, 0));
    assertThrows(IllegalArgumentException.class, () -> ExplorationPolicy.of(1, Double.NaN, 0, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> ExplorationPolicy.of(1, 1e308, 0, 0, 0, 0), "scores would overflow");
    assertThrows(IllegalArgumentException.class, () -> ExplorationPolicy.parse("remaining 1\ntravel 1\n"));
    assertThrows(IllegalArgumentException.class, () -> ExplorationPolicy.parse("travel 1\n"));
  }

  @Test
  void theShippedPolicyIsTrainedRatherThanTheFreespaceRule() {
    assertNotEquals(ExplorationPolicy.freespace(), ExplorationPolicy.learned());
  }

  @Test
  void bothPoliciesFinishSafelyAndThenRetraceAKnownShortestRoute() {
    for (ExplorationPolicy policy : List.of(ExplorationPolicy.freespace(), ExplorationPolicy.learned())) {
      for (Layout layout : Layout.values()) {
        for (double loops : new double[] {0, 0.25}) {
          for (long seed = 0; seed < 10; seed++) {
            Maze maze = new MazeSpec(layout, 9, loops, TargetPlacement.RANDOM).generate(seed);
            MazeEnvironment environment = new MazeEnvironment(maze);
            FrontierExplorer robot = new FrontierExplorer(policy);
            robot.beginMaze(environment.info(seed));
            RunResult first = Runner.run(environment, robot, Runner.defaultStepLimit(environment), false);
            RunResult second = Runner.run(environment, robot, Runner.defaultStepLimit(environment), false);
            assertTrue(first.reachedTarget() && second.reachedTarget());
            assertEquals(0, first.collisions() + second.collisions());
            assertTrue(second.steps() <= first.steps(), "run 2 follows the best route run 1 found");
            if (loops == 0) {
              assertEquals(TestMazes.distance(maze, maze.start(), maze.target()), second.steps());
            }
          }
        }
      }
    }
  }

  @Test
  void trainingIsDeterministicAndRecordsEveryGeneration() {
    CrossEntropyMethod.Result first = CrossEntropyMethod.train(TINY);
    CrossEntropyMethod.Result second = CrossEntropyMethod.train(TINY);
    assertEquals(first.policy(), second.policy());
    assertEquals(first.history(), second.history());
    assertEquals(TINY.generations(), first.history().size());
    assertTrue(first.cost() > 0 && first.history().get(0).meanCost() > 0);
  }

  @Test
  void trainingMazesNeverOverlapTheBenchmark() {
    CrossEntropyMethod.Settings settings = CrossEntropyMethod.Settings.defaults();
    List<Maze> training = CrossEntropyMethod.trainingMazes(settings);
    assertEquals(6 * settings.mazesPerType(), training.size());
    Set<Maze> benchmark = new HashSet<>();
    for (Layout layout : Benchmark.LAYOUTS) {
      for (double loops : Benchmark.LOOPS) {
        for (int i = 0; i < Benchmark.DEFAULT_MAZES; i++) {
          benchmark.add(new MazeSpec(layout, settings.cells(), loops)
              .generate(Seeds.mix(Benchmark.DEFAULT_SEED, layout.ordinal(), Math.round(loops * 1000), settings.cells(), i)));
        }
      }
    }
    for (Maze maze : training) {
      assertFalse(benchmark.contains(maze), "a training maze also appears in the benchmark");
    }
  }
}
