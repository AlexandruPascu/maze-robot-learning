package io.github.alexandrupascu.maze.sim;

import io.github.alexandrupascu.maze.Direction;

/**
 * A robot controller. For each maze it gets {@link #beginMaze}, then one or more runs from the
 * start to the target. Anything it remembers between runs on the same maze is what it learned.
 */
public interface Robot {

  /** A new maze: forget everything learned about the previous one. */
  void beginMaze(MazeInfo info);

  /** A run on the current maze is starting; runs count from 0. */
  default void beginRun(int run) {}

  /** Chooses which way to face; the simulator then moves the robot one tile if no wall is in the way. */
  Direction act(Observation observation);

  /** Called after every step, for robots that learn from feedback. */
  default void afterStep(StepResult result) {}
}
