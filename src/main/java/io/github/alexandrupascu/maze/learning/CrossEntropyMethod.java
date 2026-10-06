package io.github.alexandrupascu.maze.learning;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.bench.Seeds;
import io.github.alexandrupascu.maze.generate.Layout;
import io.github.alexandrupascu.maze.generate.MazeSpec;
import io.github.alexandrupascu.maze.sim.MazeEnvironment;
import io.github.alexandrupascu.maze.sim.Robot;
import io.github.alexandrupascu.maze.sim.Runner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Trains an {@link ExplorationPolicy} with the cross-entropy method: sample weight vectors from a
 * Gaussian, keep the cheapest, refit the Gaussian to them, and repeat. A policy's cost is its mean
 * first-run steps on the training mazes relative to the freespace rule on the same mazes, so every
 * maze type counts equally whatever its size, and the freespace rule costs exactly 1.
 *
 * <p>The training mazes come from their own seed, never from the benchmark's. Evaluation runs in
 * parallel but sums costs in a fixed order, so training gives the same weights on every machine.
 */
public final class CrossEntropyMethod {
  /** Deliberately different from the benchmark's seed, so trained policies are tested on unseen mazes. */
  public static final long TRAINING_SEED = 77;
  private static final double[] LOOPS = {0, 0.1, 0.25};
  private static final double MIN_SPREAD = 0.02;

  private CrossEntropyMethod() {}

  public record Settings(int generations, int population, int elites, int mazesPerType, int cells, long seed) {
    public Settings {
      if (generations < 1 || elites < 1 || population <= elites || mazesPerType < 1) {
        throw new IllegalArgumentException("need generations >= 1, mazes >= 1 and population > elites >= 1");
      }
    }

    public static Settings defaults() {
      return new Settings(25, 40, 8, 50, 15, TRAINING_SEED);
    }
  }

  /** One generation: the cheapest sample, the mean cost of all samples, and the refitted mean weights. */
  public record Generation(int number, double bestCost, double meanCost, List<Double> meanWeights) {}

  public record Result(Settings settings, ExplorationPolicy policy, double cost, List<Generation> history) {}

  public static Result train(Settings settings) {
    List<Maze> mazes = trainingMazes(settings);
    long[] freespace = steps(ExplorationPolicy.freespace(), mazes);
    int dimensions = ExplorationPolicy.FEATURES.size();
    double[] mean = ExplorationPolicy.freespace().weights();
    double[] spread = {0, 0.5, 0.5, 1, 0.05, 1};
    Random random = new Random(settings.seed());
    List<Generation> history = new ArrayList<>();
    for (int generation = 1; generation <= settings.generations(); generation++) {
      double[][] samples = new double[settings.population()][dimensions];
      for (double[] sample : samples) {
        for (int d = 0; d < dimensions; d++) {
          sample[d] = mean[d] + spread[d] * random.nextGaussian();
        }
      }
      double[] costs = new double[samples.length];
      for (int k = 0; k < samples.length; k++) {
        costs[k] = cost(ExplorationPolicy.of(samples[k]), mazes, freespace);
      }
      Integer[] order = new Integer[samples.length];
      for (int k = 0; k < order.length; k++) {
        order[k] = k;
      }
      Arrays.sort(order, (a, b) -> costs[a] != costs[b] ? Double.compare(costs[a], costs[b]) : Integer.compare(a, b));
      for (int d = 1; d < dimensions; d++) {
        double sum = 0;
        for (int e = 0; e < settings.elites(); e++) {
          sum += samples[order[e]][d];
        }
        mean[d] = sum / settings.elites();
        double variance = 0;
        for (int e = 0; e < settings.elites(); e++) {
          double deviation = samples[order[e]][d] - mean[d];
          variance += deviation * deviation;
        }
        // A floor on the spread keeps the search from collapsing onto one point too early.
        spread[d] = Math.sqrt(variance / settings.elites()) + MIN_SPREAD;
      }
      double meanCost = 0;
      for (double cost : costs) {
        meanCost += cost;
      }
      history.add(new Generation(generation, costs[order[0]], meanCost / costs.length, toList(mean)));
    }
    ExplorationPolicy policy = ExplorationPolicy.of(mean);
    return new Result(settings, policy, cost(policy, mazes, freespace), List.copyOf(history));
  }

  /** The training mazes: {@code mazesPerType} of each layout and loop share, from the training seed. */
  public static List<Maze> trainingMazes(Settings settings) {
    List<Maze> mazes = new ArrayList<>();
    for (Layout layout : Layout.values()) {
      for (double loops : LOOPS) {
        MazeSpec spec = new MazeSpec(layout, settings.cells(), loops);
        for (int i = 0; i < settings.mazesPerType(); i++) {
          mazes.add(spec.generate(Seeds.mix(settings.seed(), layout.ordinal(), Math.round(loops * 1000), settings.cells(), i)));
        }
      }
    }
    return List.copyOf(mazes);
  }

  private static double cost(ExplorationPolicy policy, List<Maze> mazes, long[] freespace) {
    long[] taken = steps(policy, mazes);
    double total = 0;
    for (int i = 0; i < taken.length; i++) {
      total += (double) taken[i] / freespace[i];
    }
    return total / taken.length;
  }

  // First-run steps on every maze, computed in parallel into fixed slots.
  private static long[] steps(ExplorationPolicy policy, List<Maze> mazes) {
    return IntStream.range(0, mazes.size()).parallel().mapToLong(i -> firstRun(policy, mazes.get(i))).toArray();
  }

  public static int firstRun(ExplorationPolicy policy, Maze maze) {
    MazeEnvironment environment = new MazeEnvironment(maze);
    Robot robot = new FrontierExplorer(policy);
    robot.beginMaze(environment.info(0));
    return Runner.run(environment, robot, Runner.defaultStepLimit(environment), false).steps();
  }

  private static List<Double> toList(double[] values) {
    List<Double> list = new ArrayList<>();
    for (double value : values) {
      list.add(value);
    }
    return List.copyOf(list);
  }
}
