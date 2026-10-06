package io.github.alexandrupascu.maze.render;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Draws a trial for the terminal, two characters per tile so the maze looks square. The last run's
 * route is split against the shortest route that overlaps it most: where they agree, where the last
 * run detoured, and where it missed the shortest route. Each tile shows the first that applies:
 * start or target, wall, the last run on the shortest route, a detour, a missed part of the shortest
 * route, a tile visited on run 1, open floor. Colour restyles the same characters and changes nothing
 * else, so plain and coloured drawings carry the same information.
 */
public final class AsciiRenderer {
  private static final String RESET = "\u001b[0m";

  private AsciiRenderer() {}

  /** Characters, 256-colour ANSI style and legend label for each kind of tile. */
  enum Mark {
    // Fixed palette entries rather than the theme's base colours, which some themes make invisible.
    // Labels name the last run as %d.
    WALL("##", "38;5;244;48;5;244", "wall"),
    VISITED("..", "38;5;172", "visited on run 1"),
    ROUTE("**", "1;38;5;33", "run %d on a shortest route"),
    DETOUR("~~", "1;38;5;203", "run %d detour"),
    MISSED("++", "1;38;5;170", "shortest route run %d missed"),
    START("S ", "1;38;5;16;48;5;34", "start"),
    TARGET("T ", "1;38;5;231;48;5;160", "target"),
    FLOOR("  ", null, null);

    private final String text;
    private final String style;
    private final String label;

    Mark(String text, String style, String label) {
      this.text = text;
      this.style = style;
      this.label = label;
    }
  }

  public static String render(Trial trial, boolean colour) {
    Maze maze = trial.maze();
    Set<Position> visited = new HashSet<>(trial.first().trail());
    Set<Position> route = new HashSet<>(trial.last().trail());
    Set<Position> shortest = new HashSet<>(trial.shortestRoute());
    Line line = new Line(colour);
    for (int y = 0; y < maze.height(); y++) {
      for (int x = 0; x < maze.width(); x++) {
        Position tile = new Position(x, y);
        Mark mark;
        if (tile.equals(maze.start())) {
          mark = Mark.START;
        } else if (tile.equals(maze.target())) {
          mark = Mark.TARGET;
        } else if (!maze.isOpen(tile)) {
          mark = Mark.WALL;
        } else if (route.contains(tile)) {
          mark = shortest.contains(tile) ? Mark.ROUTE : Mark.DETOUR;
        } else if (shortest.contains(tile)) {
          mark = Mark.MISSED;
        } else if (visited.contains(tile)) {
          mark = Mark.VISITED;
        } else {
          mark = Mark.FLOOR;
        }
        line.add(mark.text, mark.style);
      }
      line.end();
    }
    return line.toString();
  }

  /** A two-line key to the drawing, styled the same way, for a trial whose last run is {@code lastRun}. */
  public static String legend(boolean colour, int lastRun) {
    Line line = new Line(colour);
    for (List<Mark> row : List.of(
        List.of(Mark.WALL, Mark.VISITED, Mark.ROUTE),
        List.of(Mark.DETOUR, Mark.MISSED, Mark.START, Mark.TARGET))) {
      for (int i = 0; i < row.size(); i++) {
        Mark mark = row.get(i);
        line.add(mark.text, mark.style);
        line.add(" " + mark.label.replace("%d", String.valueOf(lastRun)) + (i < row.size() - 1 ? "   " : ""), null);
      }
      line.end();
    }
    return line.toString();
  }

  // Emits a style change only where the style changes, and resets before every line break.
  private static final class Line {
    private final StringBuilder out = new StringBuilder();
    private final boolean colour;
    private String active;

    Line(boolean colour) {
      this.colour = colour;
    }

    void add(String text, String style) {
      if (colour && !Objects.equals(style, active)) {
        if (active != null) {
          out.append(RESET);
        }
        if (style != null) {
          out.append("\u001b[").append(style).append('m');
        }
        active = style;
      }
      out.append(text);
    }

    void end() {
      if (active != null) {
        out.append(RESET);
        active = null;
      }
      out.append('\n');
    }

    @Override
    public String toString() {
      return out.toString();
    }
  }
}
