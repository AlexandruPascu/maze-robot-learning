package io.github.alexandrupascu.maze.bench;

/** Derives independent seeds from structured inputs with the SplitMix64 finaliser. */
public final class Seeds {
  private Seeds() {}

  public static long mix(long... values) {
    long hash = 0x9E3779B97F4A7C15L;
    for (long value : values) {
      hash = splitMix(hash ^ value);
    }
    return hash;
  }

  private static long splitMix(long value) {
    long z = value + 0x9E3779B97F4A7C15L;
    z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
    z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
    return z ^ (z >>> 31);
  }
}
