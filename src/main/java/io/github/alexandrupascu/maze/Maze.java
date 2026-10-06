package io.github.alexandrupascu.maze;

import java.util.Arrays;
import java.util.Objects;

/**
 * An immutable maze of square tiles, each either wall or open floor, with a closed outer border.
 * Generated mazes put cells on odd coordinates and wall or passage tiles between them.
 */
public final class Maze {
  private final int width;
  private final int height;
  private final boolean[] open;
  private final Position start;
  private final Position target;

  public Maze(int width, int height, boolean[] open, Position start, Position target) {
    if (width < 3 || height < 3 || open.length != width * height) {
      throw new IllegalArgumentException("a maze needs at least 3x3 tiles and one entry per tile");
    }
    this.width = width;
    this.height = height;
    this.open = open.clone();
    this.start = Objects.requireNonNull(start);
    this.target = Objects.requireNonNull(target);
    for (int x = 0; x < width; x++) {
      if (this.open[x] || this.open[(height - 1) * width + x]) {
        throw new IllegalArgumentException("the top and bottom border must be wall");
      }
    }
    for (int y = 0; y < height; y++) {
      if (this.open[y * width] || this.open[y * width + width - 1]) {
        throw new IllegalArgumentException("the left and right border must be wall");
      }
    }
    if (!isOpen(start) || !isOpen(target) || start.equals(target)) {
      throw new IllegalArgumentException("start and target must be different open tiles");
    }
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public Position start() {
    return start;
  }

  public Position target() {
    return target;
  }

  public boolean isOpen(int x, int y) {
    return x >= 0 && y >= 0 && x < width && y < height && open[y * width + x];
  }

  public boolean isOpen(Position position) {
    return isOpen(position.x(), position.y());
  }

  public int openTiles() {
    int count = 0;
    for (boolean tile : open) {
      if (tile) {
        count++;
      }
    }
    return count;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Maze maze
        && width == maze.width
        && height == maze.height
        && start.equals(maze.start)
        && target.equals(maze.target)
        && Arrays.equals(open, maze.open);
  }

  @Override
  public int hashCode() {
    return Objects.hash(width, height, start, target, Arrays.hashCode(open));
  }
}
