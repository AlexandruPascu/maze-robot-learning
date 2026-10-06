package io.github.alexandrupascu.maze.agents;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.coursework.CourseworkRobot;
import io.github.alexandrupascu.maze.learning.ExplorationPolicy;
import io.github.alexandrupascu.maze.learning.FrontierExplorer;
import io.github.alexandrupascu.maze.learning.LrtaStar;
import io.github.alexandrupascu.maze.learning.QLearner;
import io.github.alexandrupascu.maze.sim.Robot;

/** The robots available to the command line, the benchmark and the learning curves. */
public enum Agent {
  COURSEWORK("coursework", "Coursework explorer (2022)"),
  MAP_PLANNER("map-planner", "Map planner"),
  ROUTE_PROVER("route-prover", "Route prover"),
  A_STAR("a-star", "A* oracle"),
  Q_LEARNING("q-learning", "Q-learning"),
  DYNA_Q("dyna-q", "Dyna-Q"),
  LRTA_STAR("lrta-star", "LRTA*"),
  FRONTIER_EXPLORER("frontier-explorer", "Frontier explorer"),
  LEARNED_EXPLORER("learned-explorer", "Learned explorer");

  /** Replayed moves per real step for Dyna-Q, as in Sutton and Barto's maze experiments. */
  public static final int DYNA_PLANNING_STEPS = 50;

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
      case Q_LEARNING -> QLearner.qLearning();
      case DYNA_Q -> QLearner.dynaQ(DYNA_PLANNING_STEPS);
      case LRTA_STAR -> new LrtaStar();
      case FRONTIER_EXPLORER -> new FrontierExplorer(ExplorationPolicy.freespace());
      case LEARNED_EXPLORER -> new FrontierExplorer(ExplorationPolicy.learned());
    };
  }

  public static Agent parse(String id) {
    for (Agent agent : values()) {
      if (agent.id.equals(id)) {
        return agent;
      }
    }
    throw new IllegalArgumentException("unknown agent '" + id
        + "' (use coursework, map-planner, route-prover, a-star, q-learning, dyna-q, lrta-star, "
        + "frontier-explorer or learned-explorer)");
  }
}
