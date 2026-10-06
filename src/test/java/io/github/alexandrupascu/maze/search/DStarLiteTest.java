package io.github.alexandrupascu.maze.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.generate.Layout;
import io.github.alexandrupascu.maze.generate.MazeSpec;
import io.github.alexandrupascu.maze.generate.TargetPlacement;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class DStarLiteTest {

  // Drives a robot through real mazes, revealing the walls beside it at every step, and checks the
  // repaired distance and chosen step against a fresh breadth-first search each time.
  @Test
  void agreesWithSearchingAgainAfterEveryDiscovery() {
    for (Layout layout : Layout.values()) {
      for (double loops : new double[] {0, 0.25}) {
        for (long seed = 0; seed < 15; seed++) {
          Maze maze = new MazeSpec(layout, 10, loops, TargetPlacement.RANDOM).generate(seed);
          boolean[] wall = new boolean[maze.width() * maze.height()];
          Position robot = maze.start();
          DStarLite planner = new DStarLite(maze.width(), maze.height(), robot, maze.target());
          int steps = 0;
          while (!robot.equals(maze.target())) {
            planner.moveTo(robot);
            for (Heading heading : Heading.values()) {
              Position next = robot.step(heading);
              if (!maze.isOpen(next) && inside(maze, next)) {
                wall[next.y() * maze.width() + next.x()] = true;
                planner.block(next);
              }
            }
            int expected = distance(maze.width(), maze.height(), wall, robot, maze.target());
            assertEquals(expected, planner.distance(), "seed " + seed + " step " + steps);
            Position step = planner.next();
            Heading.between(robot, step);
            assertEquals(expected - 1, distance(maze.width(), maze.height(), wall, step, maze.target()));
            robot = step;
            steps++;
          }
        }
      }
    }
  }

  // Walls appearing anywhere, in batches, while the start moves along its route.
  @Test
  void repairsCorrectlyWhenWallsAppearAnywhere() {
    Random random = new Random(7);
    for (int trial = 0; trial < 40; trial++) {
      int width = 21;
      int height = 15;
      boolean[] wall = new boolean[width * height];
      Position goal = new Position(19, 13);
      Position robot = new Position(1, 1);
      DStarLite planner = new DStarLite(width, height, robot, goal);
      for (int round = 0; round < 25 && !robot.equals(goal); round++) {
        planner.moveTo(robot);
        for (int i = 0; i < 6; i++) {
          Position tile = new Position(random.nextInt(width), random.nextInt(height));
          if (!tile.equals(goal) && !tile.equals(robot)
              && distanceAfterBlocking(width, height, wall, tile, robot, goal) < Integer.MAX_VALUE) {
            wall[tile.y() * width + tile.x()] = true;
            planner.block(tile);
          }
        }
        assertEquals(distance(width, height, wall, robot, goal), planner.distance(), "trial " + trial);
        robot = planner.next();
      }
    }
  }

  @Test
  void rejectsBlockingTheGoalAndReportsUnreachableGoals() {
    DStarLite planner = new DStarLite(5, 3, new Position(0, 1), new Position(4, 1));
    assertThrows(IllegalArgumentException.class, () -> planner.block(new Position(4, 1)));
    for (int y = 0; y < 3; y++) {
      planner.block(new Position(2, y));
    }
    assertThrows(IllegalStateException.class, planner::next);
  }

  @Test
  void movingAlongTheRouteNeedsNoFurtherSearch() {
    DStarLite planner = new DStarLite(9, 1, new Position(0, 0), new Position(8, 0));
    assertEquals(8, planner.distance());
    long work = planner.expansions();
    Position robot = new Position(0, 0);
    for (int step = 0; step < 8; step++) {
      robot = planner.next();
      planner.moveTo(robot);
    }
    assertEquals(new Position(8, 0), robot);
    assertEquals(0, planner.distance());
    assertEquals(work, planner.expansions());
  }

  private static boolean inside(Maze maze, Position tile) {
    return tile.x() >= 0 && tile.y() >= 0 && tile.x() < maze.width() && tile.y() < maze.height();
  }

  // Keeps the random walls from sealing the goal off completely.
  private static int distanceAfterBlocking(int width, int height, boolean[] wall, Position tile, Position from, Position to) {
    boolean[] trial = wall.clone();
    trial[tile.y() * width + tile.x()] = true;
    int result = distance(width, height, trial, from, to);
    return result < 0 ? Integer.MAX_VALUE : result;
  }

  private static int distance(int width, int height, boolean[] wall, Position from, Position to) {
    int[] distance = new int[width * height];
    Arrays.fill(distance, -1);
    ArrayDeque<Position> queue = new ArrayDeque<>();
    queue.add(from);
    distance[from.y() * width + from.x()] = 0;
    while (!queue.isEmpty()) {
      Position current = queue.poll();
      if (current.equals(to)) {
        return distance[to.y() * width + to.x()];
      }
      for (Heading heading : Heading.values()) {
        Position next = current.step(heading);
        int index = next.y() * width + next.x();
        if (next.x() >= 0 && next.y() >= 0 && next.x() < width && next.y() < height && !wall[index] && distance[index] < 0) {
          distance[index] = distance[current.y() * width + current.x()] + 1;
          queue.add(next);
        }
      }
    }
    return -1;
  }

}
