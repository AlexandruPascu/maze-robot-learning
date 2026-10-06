package io.github.alexandrupascu.maze;

/** A turn relative to the robot's heading, ordered by clockwise quarter turns. */
public enum Direction {
  AHEAD,
  RIGHT,
  BEHIND,
  LEFT;

  private static final Direction[] ALL = values();

  /** The direction reached by a number of clockwise quarter turns, modulo four. */
  public static Direction ofQuarterTurns(int turns) {
    return ALL[Math.floorMod(turns, 4)];
  }
}
