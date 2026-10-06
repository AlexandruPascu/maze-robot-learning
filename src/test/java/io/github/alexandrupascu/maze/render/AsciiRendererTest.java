package io.github.alexandrupascu.maze.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.Position;
import io.github.alexandrupascu.maze.TestMazes;
import io.github.alexandrupascu.maze.agents.Agent;
import io.github.alexandrupascu.maze.generate.Layout;
import io.github.alexandrupascu.maze.generate.MazeSpec;
import io.github.alexandrupascu.maze.search.ShortestPath;
import io.github.alexandrupascu.maze.sim.RunResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class AsciiRendererTest {
  // Run 2 takes the long way round below the wall; the shortest route runs straight along the top.
  private final Maze maze = TestMazes.parse(
      "#########",
      "#S     T#",
      "# ##### #",
      "#       #",
      "#########");

  @Test
  void marksDetoursAndTheMissedShortestRoute() {
    List<Position> longWay = new ArrayList<>(List.of(at(1, 1), at(1, 2)));
    for (int x = 1; x <= 7; x++) {
      longWay.add(at(x, 3));
    }
    longWay.addAll(List.of(at(7, 2), at(7, 1)));
    List<Position> explored = new ArrayList<>(List.of(at(1, 1), at(2, 1), at(3, 1), at(2, 1), at(1, 1)));
    explored.addAll(longWay.subList(1, longWay.size()));
    Trial trial = trial(explored, longWay);
    assertEquals("""
        ##################
        ##S ++++++++++T ##
        ##~~##########~~##
        ##~~~~~~~~~~~~~~##
        ##################
        """, AsciiRenderer.render(trial, false), "the whole long way is a detour; the top row was missed");
  }

  @Test
  void showsVisitedTilesAndNoDetoursWhenRunTwoIsShortest() {
    List<Position> straight = List.of(at(1, 1), at(2, 1), at(3, 1), at(4, 1), at(5, 1), at(6, 1), at(7, 1));
    List<Position> explored = new ArrayList<>(List.of(at(1, 1), at(1, 2), at(1, 3), at(2, 3), at(1, 3), at(1, 2)));
    explored.addAll(straight);
    String drawing = AsciiRenderer.render(trial(explored, straight), false);
    assertEquals("""
        ##################
        ##S **********T ##
        ##..##########  ##
        ##....          ##
        ##################
        """, drawing);
    assertFalse(drawing.contains("+") || drawing.contains("~"));
  }

  @Test
  void colourRestylesTheSameCharacters() {
    Maze generated = new MazeSpec(Layout.PRIM, 9, 0.3).generate(3);
    for (Agent agent : Agent.values()) {
      Trial trial = Trial.run(generated, agent, 3);
      String coloured = AsciiRenderer.render(trial, true);
      assertEquals(AsciiRenderer.render(trial, false), strip(coloured));
      for (String line : coloured.split("\n")) {
        assertTrue(line.endsWith("\u001b[0m"), "every line resets its colour: " + line);
      }
    }
    assertEquals(AsciiRenderer.legend(false), strip(AsciiRenderer.legend(true)));
    Maze perfect = new MazeSpec(Layout.PRIM, 9, 0).generate(3);
    String route = AsciiRenderer.render(Trial.run(perfect, Agent.COURSEWORK, 3), true);
    assertTrue(route.contains("\u001b[38;5;244;48;5;244m##"), "walls are solid grey blocks");
    assertTrue(route.contains("\u001b[1;38;5;33m**"), "run 2 on the shortest route is bold blue");
    assertFalse(strip(route).contains("~") || strip(route).contains("+"), "perfect mazes have no detours");
  }

  @Test
  void legendExplainsEveryMark() {
    assertEquals("""
        ## wall   .. visited on run 1   ** run 2 on a shortest route
        ~~ run 2 detour   ++ shortest route run 2 missed   S  start   T  target
        """, AsciiRenderer.legend(false));
  }

  private Trial trial(List<Position> first, List<Position> second) {
    return new Trial(Agent.COURSEWORK, maze,
        new RunResult(0, first.size() - 1, 0, true, first),
        new RunResult(1, second.size() - 1, 0, true, second),
        ShortestPath.closestShortestRoute(maze, maze.start(), maze.target(), new HashSet<>(second)));
  }

  private static Position at(int x, int y) {
    return new Position(x, y);
  }

  private static String strip(String text) {
    return text.replaceAll("\u001b\\[[0-9;]*m", "");
  }
}
