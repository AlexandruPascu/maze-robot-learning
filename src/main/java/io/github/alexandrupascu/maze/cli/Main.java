package io.github.alexandrupascu.maze.cli;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.agents.Agent;
import io.github.alexandrupascu.maze.bench.Benchmark;
import io.github.alexandrupascu.maze.bench.LearningCurves;
import io.github.alexandrupascu.maze.bench.LearningReport;
import io.github.alexandrupascu.maze.bench.ReportWriter;
import io.github.alexandrupascu.maze.bench.TrainingReport;
import io.github.alexandrupascu.maze.generate.Layout;
import io.github.alexandrupascu.maze.generate.MazeSpec;
import io.github.alexandrupascu.maze.generate.TargetPlacement;
import io.github.alexandrupascu.maze.learning.CrossEntropyMethod;
import io.github.alexandrupascu.maze.render.AsciiRenderer;
import io.github.alexandrupascu.maze.render.LearningChart;
import io.github.alexandrupascu.maze.render.SvgRenderer;
import io.github.alexandrupascu.maze.render.Trial;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Command-line entry point: {@code show}, {@code benchmark}, {@code learn}, {@code train} and {@code render}. */
public final class Main {
  static final String USAGE = """
      Usage: maze-robot <command> [options]

      Commands:
        show        Draw a maze in the terminal with what a robot did on its first and
                    last runs, a legend, and colour when the terminal shows it
                      --agent coursework|map-planner|route-prover|a-star|q-learning|
                              dyna-q|lrta-star|frontier-explorer|learned-explorer
                              (map-planner; dyna-q replays 50 moves per step)
                      --layout prim|backtracker (prim)  --cells N (10)  --loops F (0)
                      --target corner|random (corner)  --seed S (1)  --runs N (2)
                      --color auto|always|never (auto; NO_COLOR=1 and FORCE_COLOR=1 also work)
        benchmark   Run every robot on seeded mazes and write CSV and Markdown reports
                      --mazes N (300)  --seed S (2022)  --sizes LIST (7,15,30)  --out DIR (reports)
        learn       Run every learner many times on each maze; write learning curves and a chart
                      --mazes N (100)  --runs N (30)  --cells N (15)  --seed S (2022)
                      --out DIR (reports)  --chart FILE (docs/learning.svg)
        train       Train the learned explorer's weights with the cross-entropy method
                      --generations N (25)  --population N (40)  --elites N (8)
                      --mazes N per maze type (50)  --cells N (15)  --seed S (77)
                      --model FILE (models/explorer.weights)  --out DIR (reports)
        render      Draw the coursework robot, map planner and route prover on one maze as SVG
                      --layout prim|backtracker (prim)  --cells N (15)  --loops F (0.1)
                      --target corner|random (corner)  --seed S (1)  --out FILE (docs/maze.svg)
        help        Show this message
      """;

  /** The maze type drawn in the learning chart: Prim mazes with a few loops separate the robots well. */
  private static final LearningCurves.Family CHART_FAMILY = new LearningCurves.Family(Layout.PRIM, 0.1);

  private Main() {}

  public static void main(String[] args) {
    int status = run(args, System.out, System.err, Terminal.detect());
    if (status != 0) {
      System.exit(status);
    }
  }

  static int run(String[] args, PrintStream out, PrintStream err, Terminal terminal) {
    if (args.length == 0 || Set.of("help", "--help", "-h").contains(args[0])) {
      printLines(out, USAGE);
      return 0;
    }
    try {
      switch (args[0]) {
        case "show" -> show(options(args, Set.of("agent", "layout", "cells", "loops", "target", "seed", "runs", "color", "colour")),
            out, terminal);
        case "benchmark" -> benchmark(options(args, Set.of("mazes", "seed", "sizes", "out")), out);
        case "learn" -> learn(options(args, Set.of("mazes", "runs", "cells", "seed", "out", "chart")), out);
        case "train" -> train(options(args, Set.of("generations", "population", "elites", "mazes", "cells", "seed",
            "model", "out")), out);
        case "render" -> render(options(args, Set.of("layout", "cells", "loops", "target", "seed", "out")), out);
        default -> throw new IllegalArgumentException("unknown command '" + args[0] + "'");
      }
      return 0;
    } catch (IllegalArgumentException error) {
      err.println("error: " + error.getMessage());
      err.println("Run 'maze-robot help' for usage.");
      return 2;
    } catch (IOException error) {
      err.println("error: " + error.getMessage());
      return 1;
    }
  }

  private static void show(Map<String, String> options, PrintStream out, Terminal terminal) {
    Agent agent = Agent.parse(options.getOrDefault("agent", Agent.MAP_PLANNER.id()));
    MazeSpec spec = spec(options, 10, 0);
    long seed = number(options, "seed", 1);
    boolean colour = terminal.colour(ColourMode.parse(options.getOrDefault("color", options.getOrDefault("colour", "auto"))));
    Trial trial = Trial.run(spec.generate(seed), agent, seed, whole(options, "runs", 2));
    out.printf(Locale.ROOT, "%s on a %s %dx%d maze, %d%% loops, seed %d%n", agent.title(),
        spec.layout().id(), spec.cells(), spec.cells(), Math.round(spec.loops() * 100), seed);
    out.println(trial.summary());
    printLines(out, AsciiRenderer.legend(colour, trial.lastRun()));
    out.println();
    printLines(out, AsciiRenderer.render(trial, colour));
  }

  private static void benchmark(Map<String, String> options, PrintStream out) throws IOException {
    Benchmark.Settings defaults = Benchmark.Settings.defaults();
    List<Integer> sizes = defaults.sizes();
    if (options.containsKey("sizes")) {
      sizes = new ArrayList<>();
      for (String size : options.get("sizes").split(",")) {
        sizes.add(integer("sizes", size.trim()));
      }
    }
    Benchmark.Settings settings = new Benchmark.Settings(
        whole(options, "mazes", defaults.mazes()), number(options, "seed", defaults.seed()), sizes);
    Benchmark.Report report = Benchmark.run(settings);
    Path directory = Path.of(options.getOrDefault("out", "reports"));
    ReportWriter.write(report, directory);
    printLines(out, ReportWriter.markdown(report));
    out.println();
    out.println("Wrote summary.csv, search.csv and summary.md to " + directory);
  }

  private static void learn(Map<String, String> options, PrintStream out) throws IOException {
    LearningCurves.Settings defaults = LearningCurves.Settings.defaults();
    LearningCurves.Settings settings = new LearningCurves.Settings(
        whole(options, "mazes", defaults.mazes()), whole(options, "runs", defaults.runs()),
        whole(options, "cells", defaults.cells()), number(options, "seed", defaults.seed()));
    LearningCurves.Result result = LearningCurves.run(settings);
    Path directory = Path.of(options.getOrDefault("out", "reports"));
    LearningReport.write(result, directory);
    Path chart = Path.of(options.getOrDefault("chart", "docs/learning.svg"));
    if (chart.getParent() != null) {
      Files.createDirectories(chart.getParent());
    }
    Files.writeString(chart, LearningChart.render(result, CHART_FAMILY), StandardCharsets.UTF_8);
    printLines(out, LearningReport.markdown(result));
    out.println();
    out.println("Wrote learning.csv, learning-summary.csv and learning.md to " + directory + ", and " + chart);
  }

  private static void train(Map<String, String> options, PrintStream out) throws IOException {
    CrossEntropyMethod.Settings defaults = CrossEntropyMethod.Settings.defaults();
    CrossEntropyMethod.Settings settings = new CrossEntropyMethod.Settings(
        whole(options, "generations", defaults.generations()),
        whole(options, "population", defaults.population()), whole(options, "elites", defaults.elites()),
        whole(options, "mazes", defaults.mazesPerType()), whole(options, "cells", defaults.cells()),
        number(options, "seed", defaults.seed()));
    CrossEntropyMethod.Result result = CrossEntropyMethod.train(settings);
    Path model = Path.of(options.getOrDefault("model", "models/explorer.weights"));
    Path directory = Path.of(options.getOrDefault("out", "reports"));
    TrainingReport.write(result, directory, model);
    out.printf(Locale.ROOT, "Training cost %.4f (the freespace rule costs 1). Weights: %s%n", result.cost(),
        result.policy());
    out.println("Wrote " + model + ", and training.csv and training.md to " + directory);
  }

  private static void render(Map<String, String> options, PrintStream out) throws IOException {
    MazeSpec spec = spec(options, 15, 0.1);
    long seed = number(options, "seed", 1);
    Maze maze = spec.generate(seed);
    List<Trial> trials = List.of(Trial.run(maze, Agent.COURSEWORK, seed), Trial.run(maze, Agent.MAP_PLANNER, seed),
        Trial.run(maze, Agent.ROUTE_PROVER, seed));
    String description = String.format(Locale.ROOT,
        "The coursework robot, the map planner and the route prover on the same %s %dx%d maze with %d%% loops (seed %d)",
        spec.layout().id(), spec.cells(), spec.cells(), Math.round(spec.loops() * 100), seed);
    Path file = Path.of(options.getOrDefault("out", "docs/maze.svg"));
    if (file.getParent() != null) {
      Files.createDirectories(file.getParent());
    }
    Files.writeString(file, SvgRenderer.render(description, trials), StandardCharsets.UTF_8);
    for (Trial trial : trials) {
      out.println(trial.agent().title() + ": " + trial.summary());
    }
    out.println("Wrote " + file);
  }

  // Text is built with '\n' breaks; println writes each line with the platform's own line ending.
  private static void printLines(PrintStream out, String text) {
    int start = 0;
    while (start < text.length()) {
      int end = text.indexOf('\n', start);
      if (end < 0) {
        out.print(text.substring(start));
        return;
      }
      out.println(text.substring(start, end));
      start = end + 1;
    }
  }

  private static MazeSpec spec(Map<String, String> options, int cells, double loops) {
    return new MazeSpec(
        Layout.parse(options.getOrDefault("layout", Layout.PRIM.id())),
        whole(options, "cells", cells),
        options.containsKey("loops") ? fraction(options.get("loops")) : loops,
        TargetPlacement.parse(options.getOrDefault("target", TargetPlacement.CORNER.id())));
  }

  private static Map<String, String> options(String[] args, Set<String> allowed) {
    Map<String, String> options = new HashMap<>();
    for (int i = 1; i < args.length; i += 2) {
      String key = args[i].startsWith("--") ? args[i].substring(2) : "";
      if (!allowed.contains(key)) {
        throw new IllegalArgumentException("unknown option '" + args[i] + "' for " + args[0]);
      }
      if (i + 1 >= args.length) {
        throw new IllegalArgumentException(args[i] + " needs a value");
      }
      options.put(key, args[i + 1]);
    }
    return options;
  }

  private static long number(Map<String, String> options, String key, long fallback) {
    String value = options.get(key);
    if (value == null) {
      return fallback;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException error) {
      throw new IllegalArgumentException("--" + key + " must be a whole number, not '" + value + "'");
    }
  }

  // Options that must fit an int: checked rather than silently truncated.
  private static int whole(Map<String, String> options, String key, int fallback) {
    long value = number(options, key, fallback);
    if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("--" + key + " is too large: " + value);
    }
    return (int) value;
  }

  private static int integer(String key, String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException error) {
      throw new IllegalArgumentException("--" + key + " must list whole numbers, not '" + value + "'");
    }
  }

  private static double fraction(String value) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException error) {
      throw new IllegalArgumentException("--loops must be a fraction such as 0.1, not '" + value + "'");
    }
  }
}
