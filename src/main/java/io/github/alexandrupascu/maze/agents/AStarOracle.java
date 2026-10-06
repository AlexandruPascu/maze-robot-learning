package io.github.alexandrupascu.maze.agents;

import io.github.alexandrupascu.maze.Direction;
import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.search.ShortestPath;
import io.github.alexandrupascu.maze.sim.MazeInfo;
import io.github.alexandrupascu.maze.sim.Observation;
import io.github.alexandrupascu.maze.sim.Robot;
import io.github.alexandrupascu.maze.sim.SearchWork;
import java.util.List;

/** Is handed the whole maze and follows an A* route: the lower bound every other robot is measured against. */
public final class AStarOracle implements Robot, SearchWork {
  private final Maze maze;
  private List<Position> route = List.of();
  private int next;
  private long expansions;

  public AStarOracle(Maze maze) {
    this.maze = maze;
  }

  @Override
  public void beginMaze(MazeInfo info) {
    ShortestPath.Result search = ShortestPath.aStar(maze, info.start(), info.target());
    route = search.path();
    expansions = search.expanded();
  }

  @Override
  public long expansions() {
    return expansions;
  }

  @Override
  public void beginRun(int run) {
    next = 1;
  }

  @Override
  public Direction act(Observation observation) {
    Heading toward = Heading.between(observation.position(), route.get(next++));
    return observation.heading().directionTo(toward);
  }
}
