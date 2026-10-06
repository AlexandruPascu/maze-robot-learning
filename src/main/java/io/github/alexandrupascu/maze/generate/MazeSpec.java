package io.github.alexandrupascu.maze.generate;

import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * A recipe for square mazes of {@code cells} by {@code cells} cells. Cell {@code (cx, cy)} sits on
 * tile {@code (2cx + 1, 2cy + 1)}, so a maze is {@code 2 * cells + 1} tiles wide. A perfect maze is
 * carved first; {@code loops} is then the fraction of the remaining walls between two cells that
 * are knocked through, which creates alternative routes.
 */
public record MazeSpec(Layout layout, int cells, double loops, TargetPlacement target) {
  private static final Heading[] HEADINGS = Heading.values();

  public MazeSpec {
    Objects.requireNonNull(layout);
    Objects.requireNonNull(target);
    if (cells < 2 || cells > 700) {
      throw new IllegalArgumentException("cells must be between 2 and 700");
    }
    if (!(loops >= 0 && loops <= 1)) {
      throw new IllegalArgumentException("loops must be a fraction between 0 and 1");
    }
  }

  public MazeSpec(Layout layout, int cells, double loops) {
    this(layout, cells, loops, TargetPlacement.CORNER);
  }

  /** Builds the maze for a seed. java.util.Random is specified exactly, so seeds port across JVMs. */
  public Maze generate(long seed) {
    Random random = new Random(seed);
    int size = 2 * cells + 1;
    boolean[] open = new boolean[size * size];
    switch (layout) {
      case PRIM -> carvePrim(open, size, random);
      case BACKTRACKER -> carveBacktracker(open, size, random);
    }
    if (loops > 0) {
      addLoops(open, size, random);
    }
    Position start = new Position(1, 1);
    return new Maze(size, size, open, start, placeTarget(size, random));
  }

  private void carvePrim(boolean[] open, int size, Random random) {
    boolean[] carved = new boolean[cells * cells];
    // Frontier edges encoded as cell * 4 + heading; each cell adds its four edges once.
    int[] frontier = new int[cells * cells * 4];
    int count = addCell(open, size, carved, 0, frontier, 0);
    while (count > 0) {
      int pick = random.nextInt(count);
      int edge = frontier[pick];
      frontier[pick] = frontier[--count];
      int cell = edge >> 2;
      Heading heading = HEADINGS[edge & 3];
      int next = neighbour(cell, heading);
      if (next >= 0 && !carved[next]) {
        openWall(open, size, cell, heading);
        count = addCell(open, size, carved, next, frontier, count);
      }
    }
  }

  private int addCell(boolean[] open, int size, boolean[] carved, int cell, int[] frontier, int count) {
    carved[cell] = true;
    open[tileOf(cell, size)] = true;
    for (Heading heading : HEADINGS) {
      frontier[count++] = cell * 4 + heading.ordinal();
    }
    return count;
  }

  private void carveBacktracker(boolean[] open, int size, Random random) {
    boolean[] carved = new boolean[cells * cells];
    int[] stack = new int[cells * cells];
    Heading[] options = new Heading[4];
    int top = 0;
    stack[top++] = 0;
    carved[0] = true;
    open[tileOf(0, size)] = true;
    while (top > 0) {
      int cell = stack[top - 1];
      int choices = 0;
      for (Heading heading : HEADINGS) {
        int next = neighbour(cell, heading);
        if (next >= 0 && !carved[next]) {
          options[choices++] = heading;
        }
      }
      if (choices == 0) {
        top--;
        continue;
      }
      Heading heading = options[random.nextInt(choices)];
      int next = neighbour(cell, heading);
      openWall(open, size, cell, heading);
      carved[next] = true;
      open[tileOf(next, size)] = true;
      stack[top++] = next;
    }
  }

  // Walls between two cells have exactly one odd coordinate; the border is never touched.
  private void addLoops(boolean[] open, int size, Random random) {
    List<Integer> walls = new ArrayList<>();
    for (int y = 1; y < size - 1; y++) {
      for (int x = 1; x < size - 1; x++) {
        if (x % 2 != y % 2 && !open[y * size + x]) {
          walls.add(y * size + x);
        }
      }
    }
    Collections.shuffle(walls, random);
    long remove = Math.round(loops * walls.size());
    for (int i = 0; i < remove; i++) {
      open[walls.get(i)] = true;
    }
  }

  private Position placeTarget(int size, Random random) {
    if (target == TargetPlacement.CORNER) {
      return new Position(size - 2, size - 2);
    }
    int cell = 1 + random.nextInt(cells * cells - 1);
    return new Position(2 * (cell % cells) + 1, 2 * (cell / cells) + 1);
  }

  private int neighbour(int cell, Heading heading) {
    int x = cell % cells + heading.dx();
    int y = cell / cells + heading.dy();
    return x < 0 || y < 0 || x >= cells || y >= cells ? -1 : y * cells + x;
  }

  private int tileOf(int cell, int size) {
    return (2 * (cell / cells) + 1) * size + 2 * (cell % cells) + 1;
  }

  private void openWall(boolean[] open, int size, int cell, Heading heading) {
    open[tileOf(cell, size) + heading.dy() * size + heading.dx()] = true;
  }
}
