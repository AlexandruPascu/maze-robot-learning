package io.github.alexandrupascu.maze.generate;

import java.util.Locale;

/** The algorithm that carves the perfect maze before any loops are added. */
public enum Layout {
  /** Randomized Prim's algorithm: a bushy tree with many short dead ends. */
  PRIM,
  /** Recursive backtracker (randomized depth-first search): long, winding corridors. */
  BACKTRACKER;

  public String id() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static Layout parse(String id) {
    for (Layout layout : values()) {
      if (layout.id().equals(id)) {
        return layout;
      }
    }
    throw new IllegalArgumentException("unknown layout '" + id + "' (use prim or backtracker)");
  }
}
