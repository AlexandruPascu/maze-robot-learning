package io.github.alexandrupascu.maze.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {
  private final ByteArrayOutputStream out = new ByteArrayOutputStream();
  private final ByteArrayOutputStream err = new ByteArrayOutputStream();

  @Test
  void helpPrintsUsage() {
    assertEquals(0, run("help"));
    assertTrue(output().startsWith("Usage: maze-robot"));
  }

  @Test
  void mistakesExitWithStatusTwoAndAMessage() {
    assertEquals(2, run("explore"));
    assertTrue(errors().contains("unknown command 'explore'"));
    assertEquals(2, run("show", "--colour", "red"));
    assertEquals(2, run("show", "--cells"));
    assertEquals(2, run("show", "--cells", "ten"));
    assertEquals(2, run("show", "--agent", "dijkstra"));
    assertEquals(2, run("show", "--loops", "2"));
  }

  @Test
  void numbersTooLargeForTheirOptionAreRejected() {
    assertEquals(2, run("show", "--cells", "4294967302"));
    assertTrue(errors().contains("--cells is too large"));
  }

  @Test
  void showPrintsBothRunsWithALegend() {
    assertEquals(0, run("show", "--agent", "coursework", "--cells", "6", "--seed", "4"));
    String text = output();
    assertTrue(text.startsWith("Coursework explorer (2022) on a prim 6x6 maze, 0% loops, seed 4"));
    assertTrue(text.contains("run 1: ") && text.contains("shortest: "));
    assertTrue(text.contains("## wall   .. visited on run 1   ** run 2 on a shortest route\n"));
    assertTrue(text.contains("\n\n" + "#".repeat(26) + "\n##S "), "13 tiles, two characters each, start top-left");
    assertTrue(text.contains("T ##\n" + "#".repeat(26) + "\n"), "target in the bottom-right cell");
    assertFalse(text.contains("\u001b"), "no colour when output is not a terminal");
  }

  // Only bites where the separator is not a bare line feed, as on Windows.
  @Test
  void everyLineEndsWithThePlatformSeparator() {
    assertEquals(0, run("show", "--cells", "4"));
    String raw = out.toString(StandardCharsets.UTF_8);
    assertFalse(raw.replace(System.lineSeparator(), "").contains("\n"), "no bare line feeds mixed into the output");
  }

  @Test
  void colourFollowsTheFlagAndTheEnvironment() {
    assertEquals(0, run(new Terminal(Map.of(), true, false), "show", "--cells", "5"));
    assertTrue(output().contains("\u001b[38;5;244;48;5;244m##"), "auto colours a terminal");
    assertEquals(0, run(new Terminal(Map.of("NO_COLOR", "1"), true, false), "show", "--cells", "5"));
    assertFalse(output().contains("\u001b"));
    assertEquals(0, run(new Terminal(Map.of("NO_COLOR", "1"), true, false), "show", "--cells", "5", "--color", "always"));
    assertTrue(output().contains("\u001b["), "an explicit flag beats NO_COLOR");
    assertEquals(0, run(new Terminal(Map.of(), true, false), "show", "--cells", "5", "--colour", "never"));
    assertFalse(output().contains("\u001b"));
    assertEquals(2, run("show", "--color", "sometimes"));
    assertTrue(errors().contains("--color must be auto, always or never"));
  }

  @Test
  void renderWritesAnSvgImage(@TempDir Path directory) throws IOException {
    Path file = directory.resolve("maze.svg");
    assertEquals(0, run("render", "--cells", "6", "--out", file.toString()));
    String svg = Files.readString(file);
    assertTrue(svg.startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\""));
    assertTrue(svg.contains("<title>") && svg.contains("<polyline") && svg.trim().endsWith("</svg>"));
  }

  @Test
  void benchmarkWritesItsReports(@TempDir Path directory) {
    assertEquals(0, run("benchmark", "--mazes", "2", "--sizes", "4", "--out", directory.toString()));
    for (String name : new String[] {"summary.csv", "search.csv", "summary.md"}) {
      assertTrue(Files.isRegularFile(directory.resolve(name)), name);
    }
    assertTrue(output().contains("--mazes 2 --sizes 4"));
  }

  // Tests never depend on the developer's terminal or environment variables.
  private int run(String... args) {
    return run(new Terminal(Map.of(), false, false), args);
  }

  private int run(Terminal terminal, String... args) {
    out.reset();
    err.reset();
    return Main.run(args, new PrintStream(out, true, StandardCharsets.UTF_8),
        new PrintStream(err, true, StandardCharsets.UTF_8), terminal);
  }

  // Output uses the platform's line endings; the assertions compare content.
  private String output() {
    return out.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
  }

  private String errors() {
    return err.toString(StandardCharsets.UTF_8);
  }
}
