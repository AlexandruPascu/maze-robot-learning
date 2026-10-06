package io.github.alexandrupascu.maze.generate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.TestMazes;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MazeSpecTest {

  @ParameterizedTest
  @EnumSource(Layout.class)
  void perfectMazesConnectEveryCellWithoutLoops(Layout layout) {
    for (int cells : new int[] {2, 5, 12}) {
      for (long seed = 0; seed < 25; seed++) {
        Maze maze = new MazeSpec(layout, cells, 0).generate(seed);
        int cellCount = cells * cells;
        // A spanning tree over the cells: every cell plus exactly one passage per tree edge.
        assertEquals(2 * cellCount - 1, maze.openTiles());
        assertEquals(maze.openTiles(), TestMazes.reachable(maze));
      }
    }
  }

  @ParameterizedTest
  @EnumSource(Layout.class)
  void loopsKnockThroughTheRequestedShareOfWalls(Layout layout) {
    int cells = 10;
    int closedWalls = 2 * cells * (cells - 1) - (cells * cells - 1);
    for (long seed = 0; seed < 10; seed++) {
      Maze perfect = new MazeSpec(layout, cells, 0).generate(seed);
      Maze loopy = new MazeSpec(layout, cells, 0.25).generate(seed);
      assertEquals(perfect.openTiles() + Math.round(0.25 * closedWalls), loopy.openTiles());
      for (int y = 0; y < perfect.height(); y++) {
        for (int x = 0; x < perfect.width(); x++) {
          assertTrue(!perfect.isOpen(x, y) || loopy.isOpen(x, y), "loops only remove walls");
          if (x % 2 == 0 && y % 2 == 0) {
            assertFalse(loopy.isOpen(x, y), "pillars between cells stay wall");
          }
        }
      }
    }
  }

  @Test
  void equalSeedsGiveEqualMazes() {
    MazeSpec spec = new MazeSpec(Layout.PRIM, 12, 0.1, TargetPlacement.RANDOM);
    assertEquals(spec.generate(99), spec.generate(99));
    assertNotEquals(spec.generate(99), spec.generate(100));
  }

  // java.util.Random is fully specified, so these drawings must match on every JVM and platform.
  @Test
  void generatorsReproduceRecordedMazes() {
    assertEquals("""
        #########
        #S#     #
        # # # # #
        #   # # #
        # #######
        #       #
        # #######
        #      T#
        #########
        """, TestMazes.draw(new MazeSpec(Layout.PRIM, 4, 0).generate(42)));
    assertEquals("""
        #########
        #S#     #
        # # ### #
        #   # # #
        ##### # #
        #   #   #
        # # # ###
        # #    T#
        #########
        """, TestMazes.draw(new MazeSpec(Layout.BACKTRACKER, 4, 0).generate(42)));
    assertEquals("""
        #########
        #S      #
        # # # # #
        #   #   #
        # # ### #
        #       #
        # ### ###
        #    T  #
        #########
        """, TestMazes.draw(new MazeSpec(Layout.PRIM, 4, 0.5, TargetPlacement.RANDOM).generate(42)));
  }

  @Test
  void randomTargetsAreOtherCells() {
    MazeSpec spec = new MazeSpec(Layout.BACKTRACKER, 6, 0, TargetPlacement.RANDOM);
    Set<Position> targets = new HashSet<>();
    for (long seed = 0; seed < 200; seed++) {
      Maze maze = spec.generate(seed);
      Position target = maze.target();
      assertTrue(target.x() % 2 == 1 && target.y() % 2 == 1, "targets sit on cells");
      assertNotEquals(maze.start(), target);
      targets.add(target);
    }
    assertEquals(35, targets.size(), "every cell except the start is eventually chosen");
  }

  @Test
  void rejectsInvalidSpecs() {
    assertThrows(IllegalArgumentException.class, () -> new MazeSpec(Layout.PRIM, 1, 0));
    assertThrows(IllegalArgumentException.class, () -> new MazeSpec(Layout.PRIM, 5, -0.1));
    assertThrows(IllegalArgumentException.class, () -> new MazeSpec(Layout.PRIM, 5, 1.5));
    assertThrows(IllegalArgumentException.class, () -> new MazeSpec(Layout.PRIM, 5, Double.NaN));
    assertThrows(IllegalArgumentException.class, () -> Layout.parse("spiral"));
    assertThrows(IllegalArgumentException.class, () -> TargetPlacement.parse("centre"));
  }
}
