package io.github.alexandrupascu.maze.coursework;

import io.github.alexandrupascu.maze.Direction;
import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.sim.MazeInfo;
import io.github.alexandrupascu.maze.sim.Observation;
import io.github.alexandrupascu.maze.sim.Robot;
import java.awt.Point;
import java.util.Random;

/** Runs the 2022 coursework controller, unchanged apart from its random source, in the simulator. */
public final class CourseworkRobot implements Robot {
  private GrandFinale controller;

  @Override
  public void beginMaze(MazeInfo info) {
    controller = new GrandFinale(new Random(info.seed()));
    controller.reset();
  }

  @Override
  public Direction act(Observation observation) {
    View view = new View(observation);
    controller.controlRobot(view);
    return observation.heading().directionTo(view.heading);
  }

  /** One step's view of the robot. face() turns it here; the simulator then moves it. */
  private static final class View implements IRobot {
    private final Observation observation;
    private Heading heading;

    View(Observation observation) {
      this.observation = observation;
      this.heading = observation.heading();
    }

    @Override
    public int look(int direction) {
      return switch (observation.sight(heading.turn(direction(direction)))) {
        case WALL -> WALL;
        case PASSAGE -> PASSAGE;
        case BEEN_BEFORE -> BEENBEFORE;
      };
    }

    @Override
    public void face(int direction) {
      heading = heading.turn(direction(direction));
    }

    @Override
    public int getHeading() {
      return NORTH + heading.ordinal();
    }

    @Override
    public Point getLocation() {
      return point(observation.position());
    }

    @Override
    public Point getTargetLocation() {
      return point(observation.target());
    }

    @Override
    public int getRuns() {
      return observation.run();
    }

    private static Direction direction(int code) {
      if (code < AHEAD || code > LEFT) {
        throw new IllegalArgumentException("not a relative direction: " + code);
      }
      return Direction.ofQuarterTurns(code - AHEAD);
    }

    private static Point point(Position position) {
      return new Point(position.x(), position.y());
    }
  }
}
