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
 * One robot's two recorded runs on a maze, plus the shortest route that overlaps the second run
 * the most, which shows exactly where that run strayed.
 */
public record Trial(Agent agent, Maze maze, RunResult first, RunResult second, List<Position> shortestRoute) {

  /** Runs {@code agent} twice on {@code maze}, recording every tile it visits. */
  public static Trial run(Maze maze, Agent agent, long mazeSeed) {
    MazeEnvironment environment = new MazeEnvironment(maze);
    Robot robot = agent.create(maze);
    robot.beginMaze(environment.info(Seeds.mix(mazeSeed, agent.ordinal())));
    int limit = Runner.defaultStepLimit(environment);
    RunResult first = Runner.run(environment, robot, limit, true);
    RunResult second = Runner.run(environment, robot, limit, true);
    List<Position> shortestRoute =
        ShortestPath.closestShortestRoute(maze, maze.start(), maze.target(), new HashSet<>(second.trail()));
    return new Trial(agent, maze, first, second, shortestRoute);
  }

  public int shortest() {
    return shortestRoute.size() - 1;
  }

  public String summary() {
    return "run 1: " + first.steps() + " steps · run 2: " + second.steps() + " steps · shortest: " + shortest();
  }
}
