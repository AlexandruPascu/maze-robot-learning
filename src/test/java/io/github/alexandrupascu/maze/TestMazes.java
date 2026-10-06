package io.github.alexandrupascu.maze;

import java.util.ArrayDeque;
import java.util.Arrays;

/** Independent helpers for tests: plain breadth-first search and text mazes. */
public final class TestMazes {
  private TestMazes() {}

  /** Breadth-first distance between two tiles, or -1 when the target is unreachable. */
  public static int distance(Maze maze, Position from, Position to) {
    int[] distance = new int[maze.width() * maze.height()];
    Arrays.fill(distance, -1);
    ArrayDeque<Position> queue = new ArrayDeque<>();
    queue.add(from);
    distance[from.y() * maze.width() + from.x()] = 0;
    while (!queue.isEmpty()) {
      Position current = queue.poll();
      int steps = distance[current.y() * maze.width() + current.x()];
      if (current.equals(to)) {
        return steps;
      }
      for (Heading heading : Heading.values()) {
        Position next = current.step(heading);
        if (maze.isOpen(next) && distance[next.y() * maze.width() + next.x()] < 0) {
          distance[next.y() * maze.width() + next.x()] = steps + 1;
          queue.add(next);
        }
      }
    }
    return -1;
  }

  /** How many open tiles can be reached from the start. */
  public static int reachable(Maze maze) {
    int count = 0;
    for (int y = 0; y < maze.height(); y++) {
      for (int x = 0; x < maze.width(); x++) {
        if (maze.isOpen(x, y) && distance(maze, maze.start(), new Position(x, y)) >= 0) {
          count++;
        }
      }
    }
    return count;
  }

  /** Draws a maze as text: {@code #} wall, space open, {@code S} start, {@code T} target. */
  public static String draw(Maze maze) {
    StringBuilder text = new StringBuilder();
    for (int y = 0; y < maze.height(); y++) {
      for (int x = 0; x < maze.width(); x++) {
        Position tile = new Position(x, y);
        text.append(tile.equals(maze.start()) ? 'S'
            : tile.equals(maze.target()) ? 'T'
            : maze.isOpen(tile) ? ' ' : '#');
      }
      text.append('\n');
    }
    return text.toString();
  }

  /** Reads a maze drawn with {@link #draw}. */
  public static Maze parse(String... rows) {
    int width = rows[0].length();
    boolean[] open = new boolean[width * rows.length];
    Position start = null;
    Position target = null;
    for (int y = 0; y < rows.length; y++) {
      for (int x = 0; x < width; x++) {
        char tile = rows[y].charAt(x);
        open[y * width + x] = tile != '#';
        if (tile == 'S') {
          start = new Position(x, y);
        } else if (tile == 'T') {
          target = new Position(x, y);
        }
      }
    }
    return new Maze(width, rows.length, open, start, target);
  }
}
