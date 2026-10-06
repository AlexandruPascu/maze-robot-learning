package io.github.alexandrupascu.maze.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.TestMazes;
import io.github.alexandrupascu.maze.generate.Layout;
import io.github.alexandrupascu.maze.generate.MazeSpec;
import io.github.alexandrupascu.maze.generate.TargetPlacement;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShortestPathTest {

  @Test
  void bothSearchesFindBreadthFirstDistancesAlongValidRoutes() {
    for (Layout layout : Layout.values()) {
      for (double loops : new double[] {0, 0.2, 0.6}) {
        for (long seed = 0; seed < 20; seed++) {
          Maze maze = new MazeSpec(layout, 11, loops, TargetPlacement.RANDOM).generate(seed);
          int expected = TestMazes.distance(maze, maze.start(), maze.target());
          for (ShortestPath.Result result : List.of(
              ShortestPath.aStar(maze, maze.start(), maze.target()),
              ShortestPath.dijkstra(maze, maze.start(), maze.target()))) {
            assertEquals(expected, result.length());
            assertValidRoute(maze, result.path());
          }
        }
      }
    }
  }

  @Test
  void aStarNeverExpandsMoreTilesThanDijkstra() {
    for (long seed = 0; seed < 50; seed++) {
      Maze maze = new MazeSpec(Layout.BACKTRACKER, 15, 0.3).generate(seed);
      int aStar = ShortestPath.aStar(maze, maze.start(), maze.target()).expanded();
      int dijkstra = ShortestPath.dijkstra(maze, maze.start(), maze.target()).expanded();
      assertTrue(aStar <= dijkstra, "A* expanded " + aStar + " tiles, Dijkstra " + dijkstra);
    }
  }

  @Test
  void unreachableTargetsAreReported() {
    Maze sealed = TestMazes.parse(
        "#####",
        "#S#T#",
        "#####");
    assertThrows(IllegalArgumentException.class, () -> ShortestPath.aStar(sealed, sealed.start(), sealed.target()));
  }

  private static void assertValidRoute(Maze maze, List<Position> path) {
    assertEquals(maze.start(), path.get(0));
    assertEquals(maze.target(), path.get(path.size() - 1));
    for (int i = 1; i < path.size(); i++) {
      assertTrue(maze.isOpen(path.get(i)));
      Heading.between(path.get(i - 1), path.get(i));
    }
  }
}
