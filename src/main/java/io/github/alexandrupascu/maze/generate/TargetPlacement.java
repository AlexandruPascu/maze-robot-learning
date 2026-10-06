package io.github.alexandrupascu.maze.generate;

import java.util.Locale;

/** Where the target goes; the start is always the top-left cell. */
public enum TargetPlacement {
  /** The bottom-right cell, the farthest corner from the start. */
  CORNER,
  /** A uniformly random cell other than the start. */
  RANDOM;

  public String id() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static TargetPlacement parse(String id) {
    for (TargetPlacement placement : values()) {
      if (placement.id().equals(id)) {
        return placement;
      }
    }
    throw new IllegalArgumentException("unknown target '" + id + "' (use corner or random)");
  }
}
