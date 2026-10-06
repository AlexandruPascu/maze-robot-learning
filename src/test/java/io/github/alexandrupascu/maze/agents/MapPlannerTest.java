package io.github.alexandrupascu.maze.agents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.TestMazes;
import io.github.alexandrupascu.maze.generate.Layout;
import io.github.alexandrupascu.maze.generate.MazeSpec;
import io.github.alexandrupascu.maze.generate.TargetPlacement;
import io.github.alexandrupascu.maze.sim.MazeEnvironment;
import io.github.alexandrupascu.maze.sim.Robot;
import io.github.alexandrupascu.maze.sim.RunResult;
import io.github.alexandrupascu.maze.sim.Runner;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MapPlannerTest {

  @ParameterizedTest
  @EnumSource(Layout.class)
  void reachesTheTargetWithoutCollisions(Layout layout) {
    for (double loops : new double[] {0, 0.1, 0.25, 0.5}) {
      for (TargetPlacement target : TargetPlacement.values()) {
        for (long seed = 0; seed < 15; seed++) {
          Maze maze = new MazeSpec(layout, 10, loops, target).generate(seed);
          List<RunResult> runs = twoRuns(new MapPlanner(), maze);
          int shortest = TestMazes.distance(maze, maze.start(), maze.target());
          assertTrue(runs.get(0).reachedTarget() && runs.get(1).reachedTarget());
          assertEquals(0, runs.get(0).collisions() + runs.get(1).collisions());
          assertTrue(runs.get(1).steps() >= shortest);
          assertTrue(runs.get(1).steps() <= runs.get(0).steps(), "run 2 plans on what run 1 learned");
        }
      }
    }
  }

  @ParameterizedTest
  @EnumSource(Layout.class)
  void secondRunIsShortestOnPerfectMazes(Layout layout) {
    for (long seed = 0; seed < 40; seed++) {
      Maze maze = new MazeSpec(layout, 12, 0, TargetPlacement.RANDOM).generate(seed);
      assertEquals(TestMazes.distance(maze, maze.start(), maze.target()), twoRuns(new MapPlanner(), maze).get(1).steps());
    }
  }

  // Depth-first carving from the start leaves walls only between a cell and its ancestors, so every
  // side branch is fenced in by walls already seen on the way in and the optimistic plan never enters it.
  @Test
  void firstRunIsAlreadyShortestOnDepthFirstMazes() {
    for (int cells : new int[] {5, 12, 25}) {
      for (long seed = 0; seed < 40; seed++) {
        Maze maze = new MazeSpec(Layout.BACKTRACKER, cells, 0, TargetPlacement.RANDOM).generate(seed);
        assertEquals(TestMazes.distance(maze, maze.start(), maze.target()), twoRuns(new MapPlanner(), maze).get(0).steps());
      }
    }
  }

  @Test
  void startsEveryMazeWithAnEmptyMap() {
    Maze first = new MazeSpec(Layout.PRIM, 10, 0.2).generate(1);
    Maze second = new MazeSpec(Layout.BACKTRACKER, 10, 0.2).generate(2);
    MapPlanner reused = new MapPlanner();
    twoRuns(reused, first);
    assertEquals(twoRuns(new MapPlanner(), second), twoRuns(reused, second));
  }

  @Test
  void oracleFollowsTheShortestRouteOnEveryRun() {
    for (long seed = 0; seed < 20; seed++) {
      Maze maze = new MazeSpec(Layout.PRIM, 10, 0.3, TargetPlacement.RANDOM).generate(seed);
      int shortest = TestMazes.distance(maze, maze.start(), maze.target());
      for (RunResult run : twoRuns(new AStarOracle(maze), maze)) {
        assertEquals(shortest, run.steps());
        assertEquals(0, run.collisions());
      }
    }
  }

  private static List<RunResult> twoRuns(Robot robot, Maze maze) {
    MazeEnvironment environment = new MazeEnvironment(maze);
    robot.beginMaze(environment.info(0));
    int limit = Runner.defaultStepLimit(environment);
    return List.of(Runner.run(environment, robot, limit, true), Runner.run(environment, robot, limit, true));
  }
}
