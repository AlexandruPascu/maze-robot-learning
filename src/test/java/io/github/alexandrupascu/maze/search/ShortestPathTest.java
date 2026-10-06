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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
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
  void closestShortestRouteIsShortestAndFollowsThePreferredTilesBest() {
    for (long seed = 0; seed < 40; seed++) {
      Maze maze = new MazeSpec(Layout.PRIM, 10, 0.4, TargetPlacement.RANDOM).generate(seed);
      List<Position> aStar = ShortestPath.aStar(maze, maze.start(), maze.target()).path();
      assertEquals(aStar, ShortestPath.closestShortestRoute(maze, maze.start(), maze.target(), Set.copyOf(aStar)),
          "a preferred shortest route is returned unchanged");

      Set<Position> preferred = new HashSet<>();
      for (int i = 0; i < maze.width() * maze.height(); i += 3) {
        preferred.add(new Position(i % maze.width(), i / maze.width()));
      }
      List<Position> closest = ShortestPath.closestShortestRoute(maze, maze.start(), maze.target(), preferred);
      assertEquals(aStar.size(), closest.size());
      assertValidRoute(maze, closest);
      assertTrue(overlap(closest, preferred) >= overlap(aStar, preferred));
    }
  }

  // Checks the dynamic programme against every shortest route, enumerated on small mazes.
  @Test
  void closestShortestRouteOverlapsMoreThanAnyOtherShortestRoute() {
    Random random = new Random(3);
    for (long seed = 0; seed < 60; seed++) {
      Maze maze = new MazeSpec(Layout.PRIM, 4, 0.6, TargetPlacement.RANDOM).generate(seed);
      Set<Position> preferred = new HashSet<>();
      for (int y = 0; y < maze.height(); y++) {
        for (int x = 0; x < maze.width(); x++) {
          if (maze.isOpen(x, y) && random.nextBoolean()) {
            preferred.add(new Position(x, y));
          }
        }
      }
      int length = TestMazes.distance(maze, maze.start(), maze.target());
      long best = bestOverlap(maze, maze.start(), maze.target(), length, preferred, new HashSet<>());
      List<Position> closest = ShortestPath.closestShortestRoute(maze, maze.start(), maze.target(), preferred);
      assertEquals(length, closest.size() - 1);
      assertEquals(best, overlap(closest, preferred), "seed " + seed);
    }
  }

  // Exhaustive search over routes of exactly `left` steps, which are the shortest ones.
  private static long bestOverlap(Maze maze, Position at, Position to, int left, Set<Position> preferred, Set<Position> used) {
    if (TestMazes.distance(maze, at, to) != left) {
      return -1;
    }
    long here = preferred.contains(at) ? 1 : 0;
    if (left == 0) {
      return here;
    }
    used.add(at);
    long best = -1;
    for (Heading heading : Heading.values()) {
      Position next = at.step(heading);
      if (maze.isOpen(next) && !used.contains(next)) {
        best = Math.max(best, bestOverlap(maze, next, to, left - 1, preferred, used));
      }
    }
    used.remove(at);
    return best < 0 ? -1 : best + here;
  }

  // The queue packs costs into fixed bits; mazes too big for them are refused rather than mis-ordered.
  @Test
  void refusesMazesTooLargeForTheQueueEncoding() {
    int size = 1449;
    boolean[] open = new boolean[size * size];
    for (int x = 1; x < size - 1; x++) {
      open[size + x] = true;
    }
    Maze corridor = new Maze(size, size, open, new Position(1, 1), new Position(size - 2, 1));
    assertThrows(IllegalArgumentException.class, () -> ShortestPath.aStar(corridor, corridor.start(), corridor.target()));
    assertThrows(IllegalArgumentException.class, () -> new MazeSpec(Layout.PRIM, 701, 0));
  }

  @Test
  void unreachableTargetsAreReported() {
    Maze sealed = TestMazes.parse(
        "#####",
        "#S#T#",
        "#####");
    assertThrows(IllegalArgumentException.class, () -> ShortestPath.aStar(sealed, sealed.start(), sealed.target()));
  }

  private static long overlap(List<Position> route, Set<Position> tiles) {
    return route.stream().filter(tiles::contains).count();
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
