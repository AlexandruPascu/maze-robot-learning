package io.github.alexandrupascu.maze.cli;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.agents.Agent;
import io.github.alexandrupascu.maze.bench.Benchmark;
import io.github.alexandrupascu.maze.bench.ReportWriter;
import io.github.alexandrupascu.maze.generate.Layout;
import io.github.alexandrupascu.maze.generate.MazeSpec;
import io.github.alexandrupascu.maze.generate.TargetPlacement;
import io.github.alexandrupascu.maze.render.AsciiRenderer;
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

/** Command-line entry point: {@code benchmark}, {@code show} and {@code render}. */
public final class Main {
  static final String USAGE = """
      Usage: maze-robot <command> [options]

      Commands:
        show        Print a maze with a robot's run-1 coverage (.) and run-2 route (*)
                      --agent coursework|map-planner|a-star  (default map-planner)
                      --layout prim|backtracker  --cells N (10)  --loops F (0)
                      --target corner|random  --seed S (1)
        benchmark   Run every robot on seeded mazes and write CSV and Markdown reports
                      --mazes N (300)  --seed S (2022)  --sizes 7,15,30  --out DIR (reports)
        render      Draw the coursework robot and the map planner on one maze as SVG
                      --layout prim|backtracker  --cells N (15)  --loops F (0.1)
                      --target corner|random  --seed S (1)  --out FILE (docs/maze.svg)
        help        Show this message
      """;

  private Main() {}

  public static void main(String[] args) {
    int status = run(args, System.out, System.err);
    if (status != 0) {
      System.exit(status);
    }
  }

  static int run(String[] args, PrintStream out, PrintStream err) {
    if (args.length == 0 || Set.of("help", "--help", "-h").contains(args[0])) {
      out.print(USAGE);
      return 0;
    }
    try {
      switch (args[0]) {
        case "show" -> show(options(args, Set.of("agent", "layout", "cells", "loops", "target", "seed")), out);
        case "benchmark" -> benchmark(options(args, Set.of("mazes", "seed", "sizes", "out")), out);
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

  private static void show(Map<String, String> options, PrintStream out) {
    Agent agent = Agent.parse(options.getOrDefault("agent", Agent.MAP_PLANNER.id()));
    MazeSpec spec = spec(options, 10, 0);
    long seed = number(options, "seed", 1);
    Trial trial = Trial.run(spec.generate(seed), agent, seed);
    out.printf(Locale.ROOT, "%s on a %s %dx%d maze, %d%% loops, seed %d%n", agent.title(),
        spec.layout().id(), spec.cells(), spec.cells(), Math.round(spec.loops() * 100), seed);
    out.println(trial.summary());
    out.print(AsciiRenderer.render(trial));
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
        (int) number(options, "mazes", defaults.mazes()), number(options, "seed", defaults.seed()), sizes);
    Benchmark.Report report = Benchmark.run(settings);
    Path directory = Path.of(options.getOrDefault("out", "reports"));
    ReportWriter.write(report, directory);
    out.print(ReportWriter.markdown(report));
    out.println();
    out.println("Wrote summary.csv, search.csv and summary.md to " + directory);
  }

  private static void render(Map<String, String> options, PrintStream out) throws IOException {
    MazeSpec spec = spec(options, 15, 0.1);
    long seed = number(options, "seed", 1);
    Maze maze = spec.generate(seed);
    List<Trial> trials = List.of(Trial.run(maze, Agent.COURSEWORK, seed), Trial.run(maze, Agent.MAP_PLANNER, seed));
    String description = String.format(Locale.ROOT,
        "The coursework robot and the map planner on the same %s %dx%d maze with %d%% loops (seed %d)",
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

  private static MazeSpec spec(Map<String, String> options, int cells, double loops) {
    return new MazeSpec(
        Layout.parse(options.getOrDefault("layout", Layout.PRIM.id())),
        (int) number(options, "cells", cells),
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
