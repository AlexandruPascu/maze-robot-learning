package io.github.alexandrupascu.maze.search;

import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

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

  /**
   * The shortest route that shares the most tiles with {@code preferred}, so that drawings mark
   * only where a longer route really strayed. When {@code preferred} contains a whole shortest
   * route, that route is the one returned.
   */
  public static List<Position> closestShortestRoute(Maze maze, Position from, Position to, Set<Position> preferred) {
    int width = maze.width();
    Layers fromStart = breadthFirst(maze, from);
    Layers toTarget = breadthFirst(maze, to);
    int start = from.y() * width + from.x();
    int goal = to.y() * width + to.x();
    int length = fromStart.distance()[goal];
    if (length < 0) {
      throw new IllegalArgumentException("the target cannot be reached from " + from);
    }
    // Breadth-first order visits every tile after all tiles one step closer to the start, so each
    // tile on a shortest route can take the best score among its predecessors.
    int[] score = new int[fromStart.distance().length];
    int[] parent = new int[score.length];
    Arrays.fill(score, -1);
    for (int i = 0; i < fromStart.count(); i++) {
      int tile = fromStart.order()[i];
      int distance = fromStart.distance()[tile];
      if (toTarget.distance()[tile] < 0 || distance + toTarget.distance()[tile] != length) {
        continue;
      }
      int x = tile % width;
      int y = tile / width;
      int best = tile == start ? 0 : -1;
      parent[tile] = tile;
      for (Heading heading : Heading.values()) {
        int previous = (y + heading.dy()) * width + x + heading.dx();
        if (maze.isOpen(x + heading.dx(), y + heading.dy())
            && fromStart.distance()[previous] == distance - 1
            && score[previous] > best) {
          best = score[previous];
          parent[tile] = previous;
        }
      }
      score[tile] = best + (preferred.contains(new Position(x, y)) ? 1 : 0);
    }
    return path(parent, start, goal, width);
  }

  private record Layers(int[] distance, int[] order, int count) {}

  private static Layers breadthFirst(Maze maze, Position source) {
    int width = maze.width();
    int[] distance = new int[width * maze.height()];
    int[] order = new int[distance.length];
    Arrays.fill(distance, -1);
    int count = 0;
    int first = source.y() * width + source.x();
    distance[first] = 0;
    order[count++] = first;
    for (int head = 0; head < count; head++) {
      int tile = order[head];
      for (Heading heading : Heading.values()) {
        int nx = tile % width + heading.dx();
        int ny = tile / width + heading.dy();
        int next = ny * width + nx;
        if (maze.isOpen(nx, ny) && distance[next] < 0) {
          distance[next] = distance[tile] + 1;
          order[count++] = next;
        }
      }
    }
    return new Layers(distance, order, count);
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
