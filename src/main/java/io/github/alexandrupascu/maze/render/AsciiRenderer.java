package io.github.alexandrupascu.maze.render;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import java.util.HashSet;
import java.util.Set;

/** Draws a trial for the terminal: {@code #} wall, {@code .} visited on run 1, {@code *} run-2 route. */
public final class AsciiRenderer {
  private AsciiRenderer() {}

  public static String render(Trial trial) {
    Maze maze = trial.maze();
    Set<Position> explored = new HashSet<>(trial.first().trail());
    Set<Position> route = new HashSet<>(trial.second().trail());
    StringBuilder out = new StringBuilder();
    for (int y = 0; y < maze.height(); y++) {
      for (int x = 0; x < maze.width(); x++) {
        Position tile = new Position(x, y);
        char symbol;
        if (tile.equals(maze.start())) {
          symbol = 'S';
        } else if (tile.equals(maze.target())) {
          symbol = 'T';
        } else if (!maze.isOpen(tile)) {
          symbol = '#';
        } else if (route.contains(tile)) {
          symbol = '*';
        } else if (explored.contains(tile)) {
          symbol = '.';
        } else {
          symbol = ' ';
        }
        out.append(symbol);
      }
      out.append('\n');
    }
    return out.toString();
  }
}
