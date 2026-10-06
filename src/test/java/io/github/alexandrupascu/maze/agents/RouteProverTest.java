package io.github.alexandrupascu.maze.agents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.TestMazes;
import io.github.alexandrupascu.maze.generate.Layout;
import io.github.alexandrupascu.maze.generate.MazeSpec;
import io.github.alexandrupascu.maze.generate.TargetPlacement;
import io.github.alexandrupascu.maze.sim.MazeEnvironment;
import io.github.alexandrupascu.maze.sim.RunResult;
import io.github.alexandrupascu.maze.sim.Runner;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RouteProverTest {

  @ParameterizedTest
  @EnumSource(Layout.class)
  void provesTheShortestRouteBeforeReachingTheTarget(Layout layout) {
    for (double loops : new double[] {0, 0.1, 0.25, 0.5}) {
      for (TargetPlacement target : TargetPlacement.values()) {
        for (int cells : new int[] {5, 11}) {
          for (long seed = 0; seed < 12; seed++) {
            Maze maze = new MazeSpec(layout, cells, loops, target).generate(seed);
            RouteProver prover = new RouteProver();
            MazeEnvironment environment = new MazeEnvironment(maze);
            prover.beginMaze(environment.info(0));
            int limit = Runner.defaultStepLimit(environment);
            RunResult first = Runner.run(environment, prover, limit, false);
            // Checked before run 2: entering the target ends run 1, so the proof must already hold.
            assertTrue(first.reachedTarget() && prover.proven(), "the target is entered only once the route is proven");
            RunResult second = Runner.run(environment, prover, limit, false);
            assertEquals(0, first.collisions() + second.collisions());
            assertEquals(TestMazes.distance(maze, maze.start(), maze.target()), second.steps(),
                layout.id() + " loops " + loops + " seed " + seed);
          }
        }
      }
    }
  }

  // On depth-first mazes every side branch is sealed by walls already seen, so nothing needs proving.
  @Test
  void firstRunIsAlreadyShortestOnDepthFirstMazes() {
    for (long seed = 0; seed < 40; seed++) {
      Maze maze = new MazeSpec(Layout.BACKTRACKER, 12, 0, TargetPlacement.RANDOM).generate(seed);
      assertEquals(TestMazes.distance(maze, maze.start(), maze.target()), twoRuns(new RouteProver(), maze).get(0).steps());
    }
  }

  @Test
  void needsAMazeOfCells() {
    Maze evenWidth = TestMazes.parse(
        "######",
        "#S  T#",
        "######");
    MazeEnvironment environment = new MazeEnvironment(evenWidth);
    assertThrows(IllegalArgumentException.class, () -> new RouteProver().beginMaze(environment.info(0)));
  }

  @Test
  void countsItsSearchWorkPerMaze() {
    Maze maze = new MazeSpec(Layout.PRIM, 8, 0.2).generate(1);
    RouteProver prover = new RouteProver();
    twoRuns(prover, maze);
    long work = prover.expansions();
    assertTrue(work > 0);
    twoRuns(prover, maze);
    assertEquals(work, prover.expansions(), "a new maze starts the count again and the robot is deterministic");
  }

  private static List<RunResult> twoRuns(RouteProver prover, Maze maze) {
    MazeEnvironment environment = new MazeEnvironment(maze);
    prover.beginMaze(environment.info(0));
    int limit = Runner.defaultStepLimit(environment);
    return List.of(Runner.run(environment, prover, limit, true), Runner.run(environment, prover, limit, true));
  }
}
