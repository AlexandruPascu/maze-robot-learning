package io.github.alexandrupascu.maze.render;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.agents.Agent;
import io.github.alexandrupascu.maze.bench.Seeds;
import io.github.alexandrupascu.maze.search.ShortestPath;
import io.github.alexandrupascu.maze.sim.MazeEnvironment;
import io.github.alexandrupascu.maze.sim.Robot;
import io.github.alexandrupascu.maze.sim.RunResult;
import io.github.alexandrupascu.maze.sim.Runner;
import java.util.HashSet;
import java.util.List;

/**
 * A robot's first and last recorded runs on a maze, plus the shortest route that overlaps the last
 * run the most, which shows exactly where that run strayed.
 */
public record Trial(Agent agent, Maze maze, RunResult first, RunResult last, List<Position> shortestRoute) {

  /** Runs {@code agent} twice on {@code maze}, recording every tile it visits. */
  public static Trial run(Maze maze, Agent agent, long mazeSeed) {
    return run(maze, agent, mazeSeed, 2);
  }

  /** Runs {@code agent} {@code runs} times on {@code maze}, keeping the first and last runs. */
  public static Trial run(Maze maze, Agent agent, long mazeSeed, int runs) {
    if (runs < 2) {
      throw new IllegalArgumentException("a trial needs at least two runs");
    }
    MazeEnvironment environment = new MazeEnvironment(maze);
    Robot robot = agent.create(maze);
    robot.beginMaze(environment.info(Seeds.mix(mazeSeed, agent.ordinal())));
    int limit = Runner.defaultStepLimit(environment);
    RunResult first = Runner.run(environment, robot, limit, true);
    RunResult last = first;
    for (int run = 1; run < runs; run++) {
      last = Runner.run(environment, robot, limit, run == runs - 1);
    }
    List<Position> shortestRoute =
        ShortestPath.closestShortestRoute(maze, maze.start(), maze.target(), new HashSet<>(last.trail()));
    return new Trial(agent, maze, first, last, shortestRoute);
  }

  public int shortest() {
    return shortestRoute.size() - 1;
  }

  /** The number of the last run, counting from 1. */
  public int lastRun() {
    return last.run() + 1;
  }

  public String summary() {
    return "run 1: " + first.steps() + " steps · run " + lastRun() + ": " + last.steps() + " steps · shortest: "
        + shortest();
  }
}
