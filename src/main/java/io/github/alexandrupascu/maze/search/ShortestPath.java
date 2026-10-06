package io.github.alexandrupascu.maze.search;

import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/** Shortest routes on a fully known maze, where every move costs one step. */
public final class ShortestPath {
  private static final int INDEX_BITS = 22;
  private static final int COST_BITS = 20;
  private static final int MAX_COST = (1 << COST_BITS) - 1;

  private ShortestPath() {}

  /** A shortest route from start to target (inclusive) and how many tiles the search expanded. */
  public record Result(List<Position> path, int expanded) {
    public int length() {
      return path.size() - 1;
    }
  }

  /** A* with the Manhattan distance, which never overestimates when moves are four-way. */
  public static Result aStar(Maze maze, Position from, Position to) {
    return search(maze, from, to, true);
  }

  /** Dijkstra's algorithm: the same search without a heuristic. */
  public static Result dijkstra(Maze maze, Position from, Position to) {
    return search(maze, from, to, false);
  }

  private static Result search(Maze maze, Position from, Position to, boolean heuristic) {
    int width = maze.width();
    int tiles = width * maze.height();
    if (tiles > 1 << INDEX_BITS) {
      throw new IllegalArgumentException("maze too large for the search queue encoding");
    }
    int[] cost = new int[tiles];
    int[] parent = new int[tiles];
    boolean[] closed = new boolean[tiles];
    Arrays.fill(cost, Integer.MAX_VALUE);
    int start = from.y() * width + from.x();
    int goal = to.y() * width + to.x();
    cost[start] = 0;
    parent[start] = start;
    PriorityQueue<Long> queue = new PriorityQueue<>();
    queue.add(key(heuristic ? from.manhattanDistance(to) : 0, 0, start));
    int expanded = 0;
    while (!queue.isEmpty()) {
      int current = (int) (queue.poll() & ((1L << INDEX_BITS) - 1));
      if (closed[current]) {
        continue;
      }
      closed[current] = true;
      expanded++;
      if (current == goal) {
        return new Result(path(parent, start, goal, width), expanded);
      }
      int x = current % width;
      int y = current / width;
      for (Heading heading : Heading.values()) {
        int nx = x + heading.dx();
        int ny = y + heading.dy();
        int next = ny * width + nx;
        if (!maze.isOpen(nx, ny) || cost[current] + 1 >= cost[next]) {
          continue;
        }
        cost[next] = cost[current] + 1;
        parent[next] = current;
        int estimate = heuristic ? Math.abs(nx - to.x()) + Math.abs(ny - to.y()) : 0;
        queue.add(key(cost[next] + estimate, cost[next], next));
      }
    }
    throw new IllegalArgumentException("the target cannot be reached from " + from);
  }

  // Orders by estimated total, then prefers the deeper tile, then the lower index for determinism.
  private static long key(int estimate, int cost, int index) {
    return (long) estimate << (COST_BITS + INDEX_BITS)
        | (long) (MAX_COST - cost) << INDEX_BITS
        | index;
  }

  private static List<Position> path(int[] parent, int start, int goal, int width) {
    List<Position> path = new ArrayList<>();
    for (int tile = goal; ; tile = parent[tile]) {
      path.add(new Position(tile % width, tile / width));
      if (tile == start) {
        break;
      }
    }
    Collections.reverse(path);
    return List.copyOf(path);
  }
}
