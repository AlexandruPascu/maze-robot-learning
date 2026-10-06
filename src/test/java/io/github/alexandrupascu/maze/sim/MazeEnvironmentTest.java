package io.github.alexandrupascu.maze.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexandrupascu.maze.Direction;
import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.Sight;
import io.github.alexandrupascu.maze.TestMazes;
import java.util.List;
import org.junit.jupiter.api.Test;

class MazeEnvironmentTest {
  private final Maze corridor = TestMazes.parse(
      "#####",
      "#S T#",
      "#####");

  @Test
  void runsStartOnTheStartTileFacingEast() {
    Observation start = new MazeEnvironment(corridor).startRun();
    assertEquals(new Position(1, 1), start.position());
    assertEquals(Heading.EAST, start.heading());
    assertEquals(0, start.run());
    assertEquals(Sight.PASSAGE, start.look(Direction.AHEAD));
    assertEquals(Sight.WALL, start.look(Direction.BEHIND));
    assertEquals(Sight.WALL, start.look(Direction.LEFT));
    assertEquals(Sight.WALL, start.sight(Heading.SOUTH));
  }

  @Test
  void blockedMovesCostAStepAndCountAsCollisions() {
    MazeEnvironment environment = new MazeEnvironment(corridor);
    environment.startRun();
    StepResult result = environment.step(Direction.LEFT);
    assertTrue(result.collided());
    assertEquals(new Position(1, 1), result.observation().position());
    assertEquals(Heading.NORTH, result.observation().heading());
    assertEquals(1, environment.steps());
    assertEquals(1, environment.collisions());
  }

  @Test
  void visitedTilesReadAsBeenBefore() {
    MazeEnvironment environment = new MazeEnvironment(corridor);
    environment.startRun();
    StepResult first = environment.step(Direction.AHEAD);
    assertEquals(Sight.BEEN_BEFORE, first.observation().look(Direction.BEHIND));
    assertFalse(first.reachedTarget());
    assertTrue(environment.step(Direction.AHEAD).reachedTarget());
    assertThrows(IllegalStateException.class, () -> environment.step(Direction.AHEAD));
  }

  @Test
  void everyRunStartsAfresh() {
    MazeEnvironment environment = new MazeEnvironment(corridor);
    environment.startRun();
    environment.step(Direction.AHEAD);
    environment.step(Direction.AHEAD);
    Observation second = environment.startRun();
    assertEquals(1, second.run());
    assertEquals(0, environment.steps());
    assertEquals(Sight.PASSAGE, second.look(Direction.AHEAD), "visited marks are cleared");
  }

  @Test
  void stepsNeedAStartedRun() {
    assertThrows(IllegalStateException.class, () -> new MazeEnvironment(corridor).step(Direction.AHEAD));
  }

  @Test
  void runnerRecordsTheTrailAndHonoursTheStepLimit() {
    MazeEnvironment environment = new MazeEnvironment(corridor);
    RunResult straight = Runner.run(environment, fixed(Direction.AHEAD), 10, true);
    assertTrue(straight.reachedTarget());
    assertEquals(2, straight.steps());
    assertEquals(List.of(new Position(1, 1), new Position(2, 1), new Position(3, 1)), straight.trail());

    RunResult stuck = Runner.run(environment, fixed(Direction.LEFT), 5, true);
    assertFalse(stuck.reachedTarget());
    assertEquals(5, stuck.steps());
    assertEquals(4, stuck.collisions(), "north, west and south are walls, east moves, then north again");
  }

  private static Robot fixed(Direction direction) {
    return new Robot() {
      @Override
      public void beginMaze(MazeInfo info) {}

      @Override
      public Direction act(Observation observation) {
        return direction;
      }
    };
  }
}
