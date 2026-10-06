package io.github.alexandrupascu.maze.render;

import io.github.alexandrupascu.maze.bench.LearningCurves;
import io.github.alexandrupascu.maze.bench.LearningCurves.Curve;
import io.github.alexandrupascu.maze.bench.LearningCurves.FamilyResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Draws one maze type's learning curves as a standalone SVG line chart with a logarithmic step axis.
 * Series colours are the first slots of a categorical palette in a fixed order, each with a light-
 * and a dark-theme value (the sixth slot uses one for both) checked for colour-vision deficiency
 * and contrast; the theme follows the viewer's colour scheme. The learning report's tables carry the same numbers.
 */
public final class LearningChart {
  private static final int WIDTH = 760;
  private static final int HEIGHT = 470;
  private static final int LEFT = 64;
  private static final int RIGHT = 32;
  private static final int TOP = 122;
  private static final int BOTTOM = 58;
  private static final String[] LIGHT = {"#2a78d6", "#eb6834", "#1baf7a", "#eda100", "#e87ba4", "#008300"};
  private static final String[] DARK = {"#3987e5", "#d95926", "#199e70", "#c98500", "#d55181", "#008300"};

  private LearningChart() {}

  public static String render(LearningCurves.Result result, LearningCurves.Family family) {
    FamilyResult data = result.families().stream()
        .filter(candidate -> candidate.family().equals(family))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("no learning curves for " + family));
    int cells = result.settings().cells();
    int runs = result.settings().runs();
    List<Curve> curves = data.curves();
    if (curves.size() > LIGHT.length) {
      throw new IllegalArgumentException("the palette has " + LIGHT.length + " series colours");
    }
    double low = data.shortest();
    double high = data.shortest();
    for (Curve curve : curves) {
      for (double steps : curve.meanSteps()) {
        low = Math.min(low, steps);
        high = Math.max(high, steps);
      }
    }
    List<Double> ticks = ticks(low, high);
    double bottomValue = ticks.get(0);
    double topValue = ticks.get(ticks.size() - 1);
    Scale scale = new Scale(runs, bottomValue, topValue);

    String title = format("Learning curves: %d×%d %s mazes with %d%% loops", cells, cells, layoutName(family),
        Math.round(family.loops() * 100));
    String subtitle = format("Mean steps per run over %d mazes, log scale. Each robot keeps what it learned between runs.",
        result.settings().mazes());
    StringBuilder svg = new StringBuilder();
    svg.append(format("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\" "
        + "role=\"img\" aria-labelledby=\"chart-title chart-desc\">%n", WIDTH, HEIGHT, WIDTH, HEIGHT));
    svg.append("<title id=\"chart-title\">").append(escape(title)).append("</title>\n");
    svg.append("<desc id=\"chart-desc\">").append(escape(description(data, runs))).append("</desc>\n");
    svg.append(style());
    svg.append(format("<rect class=\"surface\" width=\"%d\" height=\"%d\" rx=\"12\"/>%n", WIDTH, HEIGHT));
    svg.append(format("<text class=\"ink1 title\" x=\"24\" y=\"34\">%s</text>%n", escape(title)));
    svg.append(format("<text class=\"ink2 note\" x=\"24\" y=\"56\">%s</text>%n", escape(subtitle)));
    legend(svg, curves);

    for (double tick : ticks) {
      double y = scale.y(tick);
      svg.append(format("<line class=\"grid\" x1=\"%d\" x2=\"%d\" y1=\"%.1f\" y2=\"%.1f\"/>%n", LEFT, WIDTH - RIGHT, y, y));
      svg.append(format("<text class=\"muted tick\" x=\"%d\" y=\"%.1f\" text-anchor=\"end\" dominant-baseline=\"middle\">%,.0f</text>%n",
          LEFT - 10, y, tick));
    }
    int baseline = HEIGHT - BOTTOM;
    svg.append(format("<line class=\"axis\" x1=\"%d\" x2=\"%d\" y1=\"%d\" y2=\"%d\"/>%n", LEFT, WIDTH - RIGHT, baseline, baseline));
    for (int run : runTicks(runs)) {
      svg.append(format("<text class=\"muted tick\" x=\"%.1f\" y=\"%d\" text-anchor=\"middle\">%d</text>%n",
          scale.x(run), baseline + 20, run));
    }
    svg.append(format("<text class=\"ink2 note\" x=\"%.1f\" y=\"%d\" text-anchor=\"middle\">Run</text>%n",
        (LEFT + WIDTH - RIGHT) / 2.0, baseline + 44));

    for (int i = 0; i < curves.size(); i++) {
      Curve curve = curves.get(i);
      StringBuilder points = new StringBuilder();
      for (int run = 1; run <= runs; run++) {
        points.append(run == 1 ? "" : " ").append(format("%.1f,%.1f", scale.x(run), scale.y(curve.meanSteps().get(run - 1))));
      }
      svg.append(format("<polyline class=\"line s%d\" points=\"%s\"><title>%s</title></polyline>%n", i + 1, points,
          escape(format("%s: %,.0f steps on run 1, %,.1f on run %d", curve.learner().title(), curve.meanSteps().get(0),
              curve.meanSteps().get(runs - 1), runs))));
    }
    // Drawn over the curves, so it stays visible where a robot reaches the shortest route.
    double shortestY = scale.y(data.shortest());
    svg.append(format("<line class=\"reference\" x1=\"%d\" x2=\"%d\" y1=\"%.1f\" y2=\"%.1f\"><title>%s</title></line>%n",
        LEFT, WIDTH - RIGHT, shortestY, shortestY, escape(format("Shortest route: %.1f steps", data.shortest()))));
    for (int i = 0; i < curves.size(); i++) {
      double last = curves.get(i).meanSteps().get(runs - 1);
      svg.append(format("<circle class=\"dot f%d\" cx=\"%.1f\" cy=\"%.1f\" r=\"4\"/>%n", i + 1, scale.x(runs), scale.y(last)));
    }
    return svg.append("</svg>\n").toString();
  }

  private static void legend(StringBuilder svg, List<Curve> curves) {
    List<String> labels = new ArrayList<>();
    for (Curve curve : curves) {
      labels.add(curve.learner().title());
    }
    labels.add("Shortest route");
    for (int i = 0; i < labels.size(); i++) {
      int x = 24 + (i % 4) * 182;
      int y = 84 + (i / 4) * 22;
      if (i < curves.size()) {
        svg.append(format("<line class=\"line s%d\" x1=\"%d\" x2=\"%d\" y1=\"%d\" y2=\"%d\"/>%n", i + 1, x, x + 18, y, y));
        svg.append(format("<circle class=\"dot f%d\" cx=\"%d\" cy=\"%d\" r=\"4\"/>%n", i + 1, x + 9, y));
      } else {
        svg.append(format("<line class=\"reference\" x1=\"%d\" x2=\"%d\" y1=\"%d\" y2=\"%d\"/>%n", x, x + 18, y, y));
      }
      svg.append(format("<text class=\"ink1 note\" x=\"%d\" y=\"%d\" dominant-baseline=\"middle\">%s</text>%n",
          x + 26, y, escape(labels.get(i))));
    }
  }

  private static String style() {
    StringBuilder css = new StringBuilder("<style>\n");
    css.append("text{font-family:system-ui,-apple-system,'Segoe UI',Helvetica,Arial,sans-serif}\n");
    css.append(".title{font-size:15px;font-weight:600}.note{font-size:12px}");
    css.append(".tick{font-size:11px;font-variant-numeric:tabular-nums}\n");
    css.append(".line{fill:none;stroke-width:2;stroke-linecap:round;stroke-linejoin:round}");
    css.append(".reference{stroke-width:1.5;stroke-dasharray:5 4}.grid,.axis{stroke-width:1}.dot{stroke-width:2}\n");
    css.append(theme(".surface{fill:#fcfcfb}.dot{stroke:#fcfcfb}.ink1{fill:#0b0b0b}.ink2{fill:#52514e}.muted{fill:#898781}"
        + ".grid{stroke:#e1e0d9}.axis{stroke:#c3c2b7}.reference{stroke:#52514e}", LIGHT));
    css.append("@media (prefers-color-scheme:dark){\n");
    css.append(theme(".surface{fill:#1a1a19}.dot{stroke:#1a1a19}.ink1{fill:#ffffff}.ink2{fill:#c3c2b7}.muted{fill:#898781}"
        + ".grid{stroke:#2c2c2a}.axis{stroke:#383835}.reference{stroke:#c3c2b7}", DARK));
    css.append("}\n</style>\n");
    return css.toString();
  }

  private static String theme(String chrome, String[] colours) {
    StringBuilder css = new StringBuilder(chrome).append('\n');
    for (int i = 0; i < colours.length; i++) {
      css.append(format(".s%d{stroke:%s}.f%d{fill:%s}", i + 1, colours[i], i + 1, colours[i]));
    }
    return css.append('\n').toString();
  }

  private static String description(FamilyResult data, int runs) {
    StringBuilder text = new StringBuilder(format("Shortest route %.1f steps.", data.shortest()));
    for (Curve curve : data.curves()) {
      text.append(format(" %s: run 1 %.1f, run %d %.1f.", curve.learner().title(), curve.meanSteps().get(0), runs,
          curve.meanSteps().get(runs - 1)));
    }
    return text.toString();
  }

  // Clean 1-2-5 values from the largest at or below `low` to the smallest at or above `high`.
  static List<Double> ticks(double low, double high) {
    List<Double> ticks = new ArrayList<>();
    double decade = StrictMath.pow(10, StrictMath.floor(StrictMath.log10(low)));
    double[] steps = {1, 2, 5};
    double first = decade;
    for (double step : steps) {
      if (step * decade <= low) {
        first = step * decade;
      }
    }
    for (double base = decade; ; base *= 10) {
      for (double step : steps) {
        double value = step * base;
        if (value >= first) {
          ticks.add(value);
          if (value >= high) {
            return ticks;
          }
        }
      }
    }
  }

  private static List<Integer> runTicks(int runs) {
    List<Integer> ticks = new ArrayList<>(List.of(1));
    int step = runs <= 10 ? 1 : 5;
    for (int run = step == 1 ? 2 : 5; run <= runs; run += step) {
      ticks.add(run);
    }
    if (ticks.get(ticks.size() - 1) != runs) {
      ticks.add(runs);
    }
    return ticks;
  }

  private static String layoutName(LearningCurves.Family family) {
    return switch (family.layout()) {
      case PRIM -> "Prim";
      case BACKTRACKER -> "backtracker";
    };
  }

  private static String escape(String text) {
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }

  private static String format(String pattern, Object... values) {
    return String.format(Locale.ROOT, pattern, values).replace("\r\n", "\n");
  }

  private record Scale(int runs, double bottom, double top) {
    double x(double run) {
      return LEFT + (run - 1) / (runs - 1) * (WIDTH - LEFT - RIGHT);
    }

    double y(double steps) {
      // StrictMath is bit-for-bit identical on every platform, so the drawing is too.
      double fraction = (StrictMath.log10(steps) - StrictMath.log10(bottom))
          / (StrictMath.log10(top) - StrictMath.log10(bottom));
      return HEIGHT - BOTTOM - fraction * (HEIGHT - TOP - BOTTOM);
    }
  }
}
