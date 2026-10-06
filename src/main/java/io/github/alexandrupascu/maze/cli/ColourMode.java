package io.github.alexandrupascu.maze.cli;

import java.util.Locale;

/** The {@code --color} choice: decide from the environment, or force colour on or off. */
enum ColourMode {
  AUTO,
  ALWAYS,
  NEVER;

  static ColourMode parse(String id) {
    for (ColourMode mode : values()) {
      if (mode.name().toLowerCase(Locale.ROOT).equals(id)) {
        return mode;
      }
    }
    throw new IllegalArgumentException("--color must be auto, always or never, not '" + id + "'");
  }
}
