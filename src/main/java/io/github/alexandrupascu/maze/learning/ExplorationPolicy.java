package io.github.alexandrupascu.maze.learning;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Linear weights that score frontier cells for the {@link FrontierExplorer}; the lowest score is
 * explored first. The features of a frontier cell, a reachable cell with walls not yet seen, are:
 *
 * <ul>
 *   <li>{@code travel}: cells to walk there through known passages;
 *   <li>{@code remaining}: cells from there to the target, assuming unknown walls are open;
 *   <li>{@code manhattan}: the Manhattan distance in cells from there to the target, ignoring walls;
 *   <li>{@code unknown-walls}: how many of its walls are still unknown;
 *   <li>{@code age}: decisions since it first became a frontier;
 *   <li>{@code towards-target}: how many of its unknown walls face the target.
 * </ul>
 *
 * <p>The travel weight is fixed at 1: scaling every weight together changes no decision. With
 * weights (1, 1, 0, 0, 0, 0) the explorer follows the freespace rule of the map planner.
 */
public final class ExplorationPolicy {
  public static final List<String> FEATURES =
      List.of("travel", "remaining", "manhattan", "unknown-walls", "age", "towards-target");
  /** The trained weights, next to this class on the class path and under models/ in the repository. */
  private static final String RESOURCE = "explorer.weights";
  /** Far beyond any trained weight, and small enough that no score can overflow. */
  private static final double MAX_WEIGHT = 1e6;

  private final double[] weights;

  private ExplorationPolicy(double[] weights) {
    if (weights.length != FEATURES.size()) {
      throw new IllegalArgumentException("a policy needs " + FEATURES.size() + " weights");
    }
    if (weights[0] != 1) {
      throw new IllegalArgumentException("the travel weight must be 1");
    }
    for (double weight : weights) {
      if (!(Math.abs(weight) <= MAX_WEIGHT)) {
        throw new IllegalArgumentException("weights must be numbers between -1e6 and 1e6");
      }
    }
    this.weights = weights.clone();
  }

  public static ExplorationPolicy of(double... weights) {
    return new ExplorationPolicy(weights);
  }

  /** The map planner's rule: walk to the frontier that starts the shortest optimistic route. */
  public static ExplorationPolicy freespace() {
    return of(1, 1, 0, 0, 0, 0);
  }

  /** The weights trained by {@code ./gradlew run --args="train"}, shipped with the program. */
  public static ExplorationPolicy learned() {
    try (InputStream in = ExplorationPolicy.class.getResourceAsStream(RESOURCE)) {
      if (in == null) {
        throw new IllegalStateException("the bundled " + RESOURCE + " is missing; rebuild the program");
      }
      return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IllegalArgumentException error) {
      throw new IllegalStateException("the bundled " + RESOURCE + " is corrupt: " + error.getMessage(), error);
    } catch (IOException error) {
      throw new UncheckedIOException(error);
    }
  }

  public double[] weights() {
    return weights.clone();
  }

  public double score(int travel, int remaining, int manhattan, int unknownWalls, int age, int towardsTarget) {
    return weights[0] * travel + weights[1] * remaining + weights[2] * manhattan + weights[3] * unknownWalls
        + weights[4] * age + weights[5] * towardsTarget;
  }

  /** One {@code feature weight} line per feature, in order; lines starting with '#' are comments. */
  public static ExplorationPolicy parse(String text) {
    double[] parsed = new double[FEATURES.size()];
    int count = 0;
    for (String line : text.split("\n")) {
      String trimmed = line.strip();
      if (trimmed.isEmpty() || trimmed.startsWith("#")) {
        continue;
      }
      String[] parts = trimmed.split("\\s+");
      if (parts.length != 2 || count >= parsed.length || !parts[0].equals(FEATURES.get(count))) {
        throw new IllegalArgumentException("expected '" + FEATURES.get(Math.min(count, parsed.length - 1))
            + " <weight>' but found '" + trimmed + "'");
      }
      parsed[count++] = Double.parseDouble(parts[1]);
    }
    if (count != parsed.length) {
      throw new IllegalArgumentException("expected " + parsed.length + " weights, found " + count);
    }
    return of(parsed);
  }

  public String format() {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < weights.length; i++) {
      out.append(String.format(Locale.ROOT, "%s %.6f%n", FEATURES.get(i), weights[i]).replace("\r\n", "\n"));
    }
    return out.toString();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ExplorationPolicy policy && Arrays.equals(weights, policy.weights);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(weights);
  }

  @Override
  public String toString() {
    return Arrays.toString(weights);
  }
}
