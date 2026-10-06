package io.github.alexandrupascu.maze.sim;

import io.github.alexandrupascu.maze.Direction;
import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.Sight;
import java.util.Arrays;

/**
 * Steps a robot through one maze. Every step turns the robot and then moves it one tile forward
 * unless a wall is in the way; a blocked move still costs a step and counts as a collision. Tiles
 * the robot has stood on during the current run read as {@link Sight#BEEN_BEFORE}.
 */
public final class MazeEnvironment {
  /** Every run starts on the start tile facing east. */
  public static final Heading START_HEADING = Heading.EAST;

  private static final Heading[] HEADINGS = Heading.values();

  private final Maze maze;
  private final boolean[] visited;
  private Position position;
  private Heading heading;
  private int run = -1;
  private int steps;
  private int collisions;

  public MazeEnvironment(Maze maze) {
    this.maze = maze;
    this.visited = new boolean[maze.width() * maze.height()];
  }

  public Maze maze() {
    return maze;
  }

  /** What a robot may know about this maze before its first run. */
  public MazeInfo info(long seed) {
    return new MazeInfo(maze.width(), maze.height(), maze.start(), maze.target(), seed);
  }

  /** Starts the next run from the start tile and returns the first observation. */
  public Observation startRun() {
    run++;
    Arrays.fill(visited, false);
    position = maze.start();
    heading = START_HEADING;
    steps = 0;
    collisions = 0;
    visited[index(position)] = true;
    return observe();
  }

  /** Turns the robot in {@code direction}, then moves it forward one tile if it can. */
  public StepResult step(Direction direction) {
    if (run < 0) {
      throw new IllegalStateException("startRun() must be called before step()");
    }
    if (atTarget()) {
      throw new IllegalStateException("this run has already reached the target");
    }
    heading = heading.turn(direction);
    Position next = position.step(heading);
    boolean collided = !maze.isOpen(next);
    if (collided) {
      collisions++;
    } else {
      position = next;
      visited[index(next)] = true;
    }
    steps++;
    return new StepResult(observe(), collided, atTarget());
  }

  public Observation observe() {
    Sight[] around = new Sight[4];
    for (Heading side : HEADINGS) {
      Position next = position.step(side);
      around[side.ordinal()] =
          !maze.isOpen(next) ? Sight.WALL : visited[index(next)] ? Sight.BEEN_BEFORE : Sight.PASSAGE;
    }
    return new Observation(position, heading, maze.target(), run, around);
  }

  public boolean atTarget() {
    return position.equals(maze.target());
  }

  public int steps() {
    return steps;
  }

  public int collisions() {
    return collisions;
  }

  private int index(Position tile) {
    return tile.y() * maze.width() + tile.x();
  }
}
