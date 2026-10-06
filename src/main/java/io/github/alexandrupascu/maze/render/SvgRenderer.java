package io.github.alexandrupascu.maze.render;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Draws trials side by side as a standalone SVG: tiles visited on run 1 are shaded and the run-2
 * route is a line. The image carries its own light background so it reads the same in any theme.
 */
public final class SvgRenderer {
  private static final int TILE = 12;
  private static final int MARGIN = 24;
  private static final int GAP = 32;
  private static final int HEADER = 60;
  private static final int FOOTER = 36;

  private SvgRenderer() {}

  public static String render(String description, List<Trial> trials) {
    if (trials.isEmpty()) {
      throw new IllegalArgumentException("nothing to draw");
    }
    Maze first = trials.get(0).maze();
    int mazeWidth = first.width() * TILE;
    int mazeHeight = first.height() * TILE;
    int width = 2 * MARGIN + trials.size() * mazeWidth + (trials.size() - 1) * GAP;
    int height = HEADER + mazeHeight + FOOTER;
    StringBuilder svg = new StringBuilder();
    svg.append(format("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\" role=\"img\" aria-label=\"%s\">%n",
        width, height, width, height, escape(description)));
    svg.append("<title>").append(escape(description)).append("</title>\n");
    svg.append("<style>text{font-family:system-ui,-apple-system,'Segoe UI',Helvetica,Arial,sans-serif;fill:#1f2937}"
        + ".title{font-size:15px;font-weight:600}.note{font-size:12px;fill:#4b5563}</style>\n");
    svg.append(format("<rect width=\"%d\" height=\"%d\" rx=\"12\" fill=\"#ffffff\"/>%n", width, height));
    for (int i = 0; i < trials.size(); i++) {
      panel(svg, trials.get(i), MARGIN + i * (mazeWidth + GAP));
    }
    legend(svg, height - 14);
    return svg.append("</svg>\n").toString();
  }

  private static void panel(StringBuilder svg, Trial trial, int left) {
    Maze maze = trial.maze();
    svg.append(format("<text class=\"title\" x=\"%d\" y=\"24\">%s</text>%n", left, escape(trial.agent().title())));
    svg.append(format("<text class=\"note\" x=\"%d\" y=\"44\">%s</text>%n", left, escape(trial.summary())));
    svg.append(format("<g transform=\"translate(%d %d)\">%n", left, HEADER));
    svg.append(format("<rect width=\"%d\" height=\"%d\" fill=\"#f8fafc\"/>%n", maze.width() * TILE, maze.height() * TILE));
    Set<Position> explored = new LinkedHashSet<>(trial.first().trail());
    svg.append("<path fill=\"#fbbf24\" fill-opacity=\"0.45\" d=\"");
    for (Position tile : explored) {
      svg.append(format("M%d %dh%dv%dh-%dz", tile.x() * TILE, tile.y() * TILE, TILE, TILE, TILE));
    }
    svg.append("\"/>\n<path fill=\"#1e293b\" d=\"");
    for (int y = 0; y < maze.height(); y++) {
      for (int x = 0; x < maze.width(); x++) {
        if (!maze.isOpen(x, y)) {
          svg.append(format("M%d %dh%dv%dh-%dz", x * TILE, y * TILE, TILE, TILE, TILE));
        }
      }
    }
    svg.append("\"/>\n<polyline fill=\"none\" stroke=\"#2563eb\" stroke-width=\"4\" stroke-linecap=\"round\" stroke-linejoin=\"round\" points=\"");
    List<Position> route = trial.second().trail();
    for (int i = 0; i < route.size(); i++) {
      svg.append(i == 0 ? "" : " ").append(format("%d,%d", centre(route.get(i).x()), centre(route.get(i).y())));
    }
    svg.append("\"/>\n");
    svg.append(format("<circle cx=\"%d\" cy=\"%d\" r=\"5\" fill=\"#16a34a\"/>%n", centre(maze.start().x()), centre(maze.start().y())));
    svg.append(format("<rect x=\"%d\" y=\"%d\" width=\"10\" height=\"10\" fill=\"#dc2626\"/>%n",
        centre(maze.target().x()) - 5, centre(maze.target().y()) - 5));
    svg.append("</g>\n");
  }

  private static void legend(StringBuilder svg, int baseline) {
    int x = MARGIN;
    svg.append(format("<rect x=\"%d\" y=\"%d\" width=\"12\" height=\"12\" fill=\"#fbbf24\" fill-opacity=\"0.45\"/>%n", x, baseline - 10));
    svg.append(format("<text class=\"note\" x=\"%d\" y=\"%d\">tiles visited on run 1</text>%n", x + 18, baseline));
    x += 160;
    svg.append(format("<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#2563eb\" stroke-width=\"4\" stroke-linecap=\"round\"/>%n",
        x, baseline - 4, x + 18, baseline - 4));
    svg.append(format("<text class=\"note\" x=\"%d\" y=\"%d\">route on run 2</text>%n", x + 26, baseline));
    x += 136;
    svg.append(format("<circle cx=\"%d\" cy=\"%d\" r=\"5\" fill=\"#16a34a\"/>%n", x + 5, baseline - 4));
    svg.append(format("<text class=\"note\" x=\"%d\" y=\"%d\">start</text>%n", x + 16, baseline));
    x += 64;
    svg.append(format("<rect x=\"%d\" y=\"%d\" width=\"10\" height=\"10\" fill=\"#dc2626\"/>%n", x, baseline - 9));
    svg.append(format("<text class=\"note\" x=\"%d\" y=\"%d\">target</text>%n", x + 16, baseline));
  }

  private static int centre(int tile) {
    return tile * TILE + TILE / 2;
  }

  private static String escape(String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }

  private static String format(String pattern, Object... values) {
    return String.format(Locale.ROOT, pattern, values).replace("\r\n", "\n");
  }
}
