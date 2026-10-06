package io.github.alexandrupascu.maze.agents;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.coursework.CourseworkRobot;
import io.github.alexandrupascu.maze.sim.Robot;

/** The robots available to the benchmark and the command line. */
public enum Agent {
  COURSEWORK("coursework", "Coursework explorer (2022)"),
  MAP_PLANNER("map-planner", "Map planner"),
  ROUTE_PROVER("route-prover", "Route prover"),
  A_STAR("a-star", "A* oracle");

  private final String id;
  private final String title;

  Agent(String id, String title) {
    this.id = id;
    this.title = title;
  }

  public String id() {
    return id;
  }

  public String title() {
    return title;
  }

  /** A fresh robot. Only the oracle uses {@code maze}; the others discover it by moving. */
  public Robot create(Maze maze) {
    return switch (this) {
      case COURSEWORK -> new CourseworkRobot();
      case MAP_PLANNER -> new MapPlanner();
      case ROUTE_PROVER -> new RouteProver();
      case A_STAR -> new AStarOracle(maze);
    };
  }

  public static Agent parse(String id) {
    for (Agent agent : values()) {
      if (agent.id.equals(id)) {
        return agent;
      }
    }
    throw new IllegalArgumentException("unknown agent '" + id + "' (use coursework, map-planner, route-prover or a-star)");
  }
}
