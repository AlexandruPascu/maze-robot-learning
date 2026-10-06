package io.github.alexandrupascu.maze.sim;

import io.github.alexandrupascu.maze.Direction;
import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.Sight;

/**
 * Everything a robot senses before a step: its tile, heading, the target tile, the run number,
 * and the four adjacent tiles. It never sees further than one tile.
 */
public final class Observation {
  private final Position position;
  private final Heading heading;
  private final Position target;
  private final int run;
  private final Sight[] around;

  Observation(Position position, Heading heading, Position target, int run, Sight[] around) {
    this.position = position;
    this.heading = heading;
    this.target = target;
    this.run = run;
    this.around = around;
  }

  public Position position() {
    return position;
  }

  public Heading heading() {
    return heading;
  }

  public Position target() {
    return target;
  }

  /** The run on the current maze, counting from 0. */
  public int run() {
    return run;
  }

  /** The adjacent tile in an absolute heading. */
  public Sight sight(Heading side) {
    return around[side.ordinal()];
  }

  /** The adjacent tile in a direction relative to the robot's heading. */
  public Sight look(Direction direction) {
    return sight(heading.turn(direction));
  }
}
