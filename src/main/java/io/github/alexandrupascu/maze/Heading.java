package io.github.alexandrupascu.maze;

/** A compass heading on the tile grid. Columns grow eastwards and rows grow southwards. */
public enum Heading {
  NORTH(0, -1),
  EAST(1, 0),
  SOUTH(0, 1),
  WEST(-1, 0);

  private static final Heading[] ALL = values();

  private final int dx;
  private final int dy;

  Heading(int dx, int dy) {
    this.dx = dx;
    this.dy = dy;
  }

  public int dx() {
    return dx;
  }

  public int dy() {
    return dy;
  }

  /** The heading after turning in a relative direction. */
  public Heading turn(Direction direction) {
    return ALL[(ordinal() + direction.ordinal()) % 4];
  }

  /** The relative direction that turns this heading into {@code target}. */
  public Direction directionTo(Heading target) {
    return Direction.ofQuarterTurns(target.ordinal() - ordinal());
  }

  /** The heading of a single step between two adjacent tiles. */
  public static Heading between(Position from, Position to) {
    for (Heading heading : ALL) {
      if (from.x() + heading.dx == to.x() && from.y() + heading.dy == to.y()) {
        return heading;
      }
    }
    throw new IllegalArgumentException(from + " and " + to + " are not adjacent");
  }
}
