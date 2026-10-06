package io.github.alexandrupascu.maze.coursework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexandrupascu.maze.Maze;
import io.github.alexandrupascu.maze.TestMazes;
import io.github.alexandrupascu.maze.generate.Layout;
import io.github.alexandrupascu.maze.generate.MazeSpec;
import io.github.alexandrupascu.maze.generate.TargetPlacement;
import io.github.alexandrupascu.maze.sim.MazeEnvironment;
import io.github.alexandrupascu.maze.sim.RunResult;
import io.github.alexandrupascu.maze.sim.Runner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CourseworkPortTest {
  private static final Path ORIGINAL = Path.of("coursework", "GrandFinale.java");
  private static final Path PORT =
      Path.of("src", "main", "java", "io", "github", "alexandrupascu", "maze", "coursework", "GrandFinale.java");
  private static final String ADDED_MEMBERS =
      "private final Random random; public GrandFinale(Random random) { this.random = random; } ";

  @Test
  void portDiffersFromTheOriginalOnlyByTheDocumentedChanges() throws IOException {
    String original = Files.readString(ORIGINAL);
    String port = Files.readString(PORT);
    assertEquals(List.of("uk.ac.warwick.dcs.maze.logic.IRobot", "java.util.ArrayList"), imports(original));
    assertEquals(List.of("java.util.ArrayList", "java.util.Random"), imports(port));
    assertTrue(code(port).contains(ADDED_MEMBERS), "the injected Random is declared as documented");
    String expected = code(original).replace("Math.random()", "random.nextDouble()");
    String actual = code(port).replace(ADDED_MEMBERS, "");
    int at = 0;
    while (at < Math.min(expected.length(), actual.length()) && expected.charAt(at) == actual.charAt(at)) {
      at++;
    }
    int from = Math.max(0, at - 60);
    assertEquals(expected.substring(from, Math.min(expected.length(), at + 60)),
        actual.substring(from, Math.min(actual.length(), at + 60)),
        "the port's code differs from the original near character " + at);
    assertEquals(expected.length(), actual.length());
  }

  @ParameterizedTest
  @EnumSource(Layout.class)
  void secondRunFollowsTheShortestRouteOnPerfectMazes(Layout layout) {
    for (int cells : new int[] {4, 9, 16}) {
      for (long seed = 0; seed < 30; seed++) {
        Maze maze = new MazeSpec(layout, cells, 0, TargetPlacement.RANDOM).generate(seed);
        List<RunResult> runs = twoRuns(maze, seed);
        assertTrue(runs.get(0).reachedTarget());
        assertEquals(TestMazes.distance(maze, maze.start(), maze.target()), runs.get(1).steps());
        assertEquals(0, runs.get(0).collisions() + runs.get(1).collisions());
      }
    }
  }

  @Test
  void finishesLoopyMazesWithoutCollisions() {
    for (double loops : new double[] {0.1, 0.25, 0.5}) {
      for (long seed = 0; seed < 30; seed++) {
        Maze maze = new MazeSpec(Layout.PRIM, 12, loops).generate(seed);
        List<RunResult> runs = twoRuns(maze, seed);
        assertTrue(runs.get(0).reachedTarget() && runs.get(1).reachedTarget());
        assertEquals(0, runs.get(0).collisions() + runs.get(1).collisions());
      }
    }
  }

  @Test
  void equalSeedsReplayTheSameRunsAndOtherSeedsExploreDifferently() {
    Maze maze = new MazeSpec(Layout.PRIM, 15, 0).generate(5);
    assertEquals(twoRuns(maze, 1), twoRuns(maze, 1));
    Set<Integer> firstRunLengths = new HashSet<>();
    for (long seed = 0; seed < 10; seed++) {
      firstRunLengths.add(twoRuns(maze, seed).get(0).steps());
    }
    assertTrue(firstRunLengths.size() > 1, "junction choices are random");
  }

  private static List<RunResult> twoRuns(Maze maze, long seed) {
    MazeEnvironment environment = new MazeEnvironment(maze);
    CourseworkRobot robot = new CourseworkRobot();
    robot.beginMaze(environment.info(seed));
    int limit = Runner.defaultStepLimit(environment);
    return List.of(Runner.run(environment, robot, limit, true), Runner.run(environment, robot, limit, true));
  }

  private static List<String> imports(String source) {
    Matcher matcher = Pattern.compile("(?m)^import\\s+([\\w.]+);").matcher(source);
    return matcher.results().map(result -> result.group(1)).toList();
  }

  // Source text without comments, package and import lines, with whitespace collapsed.
  private static String code(String source) {
    return source.replace("\r\n", "\n")
        .replaceAll("(?s)/\\*.*?\\*/", " ")
        .replaceAll("//[^\n]*", " ")
        .replaceAll("(?m)^\\s*(package|import)\\s[^;]*;", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }
}
