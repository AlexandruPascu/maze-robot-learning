package io.github.alexandrupascu.maze.bench;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.agents.MapPlanner;
import io.github.alexandrupascu.maze.agents.RouteProver;
import io.github.alexandrupascu.maze.generate.Layout;
import io.github.alexandrupascu.maze.learning.LrtaStar;
import io.github.alexandrupascu.maze.learning.QLearner;
import io.github.alexandrupascu.maze.search.ShortestPath;
import io.github.alexandrupascu.maze.sim.MazeEnvironment;
import io.github.alexandrupascu.maze.sim.Robot;
import io.github.alexandrupascu.maze.sim.Runner;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Runs every robot many times on the same maze, keeping what it learned between runs, for many
 * mazes of each type: learning curves. The mazes are the benchmark's own for the chosen size, and
 * every number is a deterministic function of the settings.
 */
public final class LearningCurves {
  public static final int DEFAULT_MAZES = 100;
  public static final int DEFAULT_RUNS = 30;
  public static final int DEFAULT_CELLS = 15;

  private LearningCurves() {}

  /** The robots compared. Dyna-Q appears twice to show what more replay buys. */
  public enum Learner {
    Q_LEARNING("q-learning", "Q-learning"),
    DYNA_Q_5("dyna-q-5", "Dyna-Q, 5 replays"),
    DYNA_Q_50("dyna-q-50", "Dyna-Q, 50 replays"),
    LRTA_STAR("lrta-star", "LRTA*"),
    MAP_PLANNER("map-planner", "Map planner"),
    ROUTE_PROVER("route-prover", "Route prover");

    private final String id;
    private final String title;

    Learner(String id, String title) {
      this.id = id;
      this.title = title;
    }

    public String id() {
      return id;
    }

    public String title() {
      return title;
    }

    Robot create() {
      return switch (this) {
        case Q_LEARNING -> QLearner.qLearning();
        case DYNA_Q_5 -> QLearner.dynaQ(5);
        case DYNA_Q_50 -> QLearner.dynaQ(50);
        case LRTA_STAR -> new LrtaStar();
        case MAP_PLANNER -> new MapPlanner();
        case ROUTE_PROVER -> new RouteProver();
      };
    }
  }

  public record Settings(int mazes, int runs, int cells, long seed) {
    public Settings {
      if (mazes < 1 || runs < 2) {
        throw new IllegalArgumentException("learning curves need at least one maze and two runs");
      }
    }

    public static Settings defaults() {
      return new Settings(DEFAULT_MAZES, DEFAULT_RUNS, DEFAULT_CELLS, Benchmark.DEFAULT_SEED);
    }
  }

  public record Family(Layout layout, double loops) {
    public String label(int cells) {
      return String.format(Locale.ROOT, "%s %dx%d, %d%% loops", layout.id(), cells, cells, Math.round(loops * 100));
    }
  }

  /**
   * One robot on one maze type. {@code meanSteps} holds the mean steps of each run. A maze has
   * converged when every run from some point to the last was a shortest route; {@code
   * shortestFromRun} is the mean of that first run over the converged mazes.
   */
  public record Curve(Learner learner, List<Double> meanSteps, int converged, double shortestFromRun, double totalSteps) {}

  public record FamilyResult(Family family, double shortest, List<Curve> curves) {}

  public record Result(Settings settings, List<FamilyResult> families) {}

  public static List<Family> families() {
    List<Family> families = new ArrayList<>();
    for (double loops : Benchmark.LOOPS) {
      for (Layout layout : Benchmark.LAYOUTS) {
        families.add(new Family(layout, loops));
      }
    }
    return families;
  }

  public static Result run(Settings settings) {
    List<FamilyResult> results = new ArrayList<>();
    for (Family family : families()) {
      Benchmark.Config config = new Benchmark.Config(family.layout(), family.loops(), settings.cells());
      Learner[] learners = Learner.values();
      double[][] steps = new double[learners.length][settings.runs()];
      int[] converged = new int[learners.length];
      long[] convergedFrom = new long[learners.length];
      long[] total = new long[learners.length];
      double shortest = 0;
      for (int i = 0; i < settings.mazes(); i++) {
        long mazeSeed = config.mazeSeed(settings.seed(), i);
        Maze maze = config.spec().generate(mazeSeed);
        int best = ShortestPath.aStar(maze, maze.start(), maze.target()).length();
        shortest += best;
        for (Learner learner : learners) {
          MazeEnvironment environment = new MazeEnvironment(maze);
          Robot robot = learner.create();
          robot.beginMaze(environment.info(Seeds.mix(mazeSeed, learner.ordinal())));
          int limit = Runner.defaultStepLimit(environment);
          int lastLonger = 0;
          for (int run = 0; run < settings.runs(); run++) {
            int taken = Runner.run(environment, robot, limit, false).steps();
            steps[learner.ordinal()][run] += taken;
            total[learner.ordinal()] += taken;
            if (taken != best) {
              lastLonger = run + 1;
            }
          }
          if (lastLonger < settings.runs()) {
            converged[learner.ordinal()]++;
            convergedFrom[learner.ordinal()] += lastLonger + 1;
          }
        }
      }
      List<Curve> curves = new ArrayList<>();
      for (Learner learner : learners) {
        int k = learner.ordinal();
        List<Double> means = new ArrayList<>();
        for (double sum : steps[k]) {
          means.add(sum / settings.mazes());
        }
        curves.add(new Curve(learner, List.copyOf(means), converged[k],
            converged[k] == 0 ? Double.NaN : (double) convergedFrom[k] / converged[k],
            (double) total[k] / settings.mazes()));
      }
      results.add(new FamilyResult(family, shortest / settings.mazes(), List.copyOf(curves)));
    }
    return new Result(settings, List.copyOf(results));
  }
}
