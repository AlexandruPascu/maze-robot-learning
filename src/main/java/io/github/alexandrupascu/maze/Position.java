package io.github.alexandrupascu.maze;

/** A tile coordinate: {@code x} is the column and {@code y} the row. */
public record Position(int x, int y) {

  public Position step(Heading heading) {
    return new Position(x + heading.dx(), y + heading.dy());
  }

  public int manhattanDistance(Position other) {
    return Math.abs(x - other.x) + Math.abs(y - other.y);
  }

  @Override
  public String toString() {
    return "(" + x + ", " + y + ")";
  }
}
