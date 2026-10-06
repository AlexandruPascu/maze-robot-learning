package io.github.alexandrupascu.maze.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
  void showPrintsBothRunsOnTheMaze() {
    assertEquals(0, run("show", "--agent", "coursework", "--cells", "6", "--seed", "4"));
    String text = output();
    assertTrue(text.startsWith("Coursework explorer (2022) on a prim 6x6 maze, 0% loops, seed 4"));
    assertTrue(text.contains("run 1: ") && text.contains("shortest: "));
    assertTrue(text.contains("S") && text.contains("T") && text.contains("*"));
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

  private int run(String... args) {
    out.reset();
    err.reset();
    return Main.run(args, new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
  }

  private String output() {
    return out.toString(StandardCharsets.UTF_8);
  }

  private String errors() {
    return err.toString(StandardCharsets.UTF_8);
  }
}
