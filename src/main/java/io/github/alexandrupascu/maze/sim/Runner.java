package io.github.alexandrupascu.maze.sim;

import io.github.alexandrupascu.maze.Position;
import java.util.ArrayList;
import java.util.List;

/** Drives a robot through runs on a maze. */
public final class Runner {
  private Runner() {}

  /** A step limit generous enough for a random walk to finish on the mazes used here. */
  public static int defaultStepLimit(MazeEnvironment environment) {
    return 200 * environment.maze().width() * environment.maze().height();
  }

  /** Runs from the start until the robot reaches the target or the step limit is used up. */
  public static RunResult run(MazeEnvironment environment, Robot robot, int stepLimit, boolean recordTrail) {
    Observation observation = environment.startRun();
    robot.beginRun(observation.run());
    List<Position> trail = new ArrayList<>();
    if (recordTrail) {
      trail.add(observation.position());
    }
    while (!environment.atTarget() && environment.steps() < stepLimit) {
      StepResult result = environment.step(robot.act(observation));
      robot.afterStep(result);
      observation = result.observation();
      if (recordTrail && !result.collided()) {
        trail.add(observation.position());
      }
    }
    return new RunResult(
        observation.run(),
        environment.steps(),
        environment.collisions(),
        environment.atTarget(),
        List.copyOf(trail));
  }
}
