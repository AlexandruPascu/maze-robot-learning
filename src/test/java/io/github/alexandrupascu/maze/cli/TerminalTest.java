package io.github.alexandrupascu.maze.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TerminalTest {

  @Test
  void autoColoursInteractiveTerminalsOnly() {
    assertTrue(unix(Map.of("TERM", "xterm-256color"), true).colour(ColourMode.AUTO));
    assertTrue(unix(Map.of(), true).colour(ColourMode.AUTO));
    assertFalse(unix(Map.of("TERM", "xterm-256color"), false).colour(ColourMode.AUTO), "redirected output");
    assertFalse(unix(Map.of("TERM", "dumb"), true).colour(ColourMode.AUTO));
  }

  @Test
  void environmentConventionsAreRespected() {
    assertFalse(unix(Map.of("NO_COLOR", "1"), true).colour(ColourMode.AUTO));
    assertTrue(unix(Map.of("NO_COLOR", ""), true).colour(ColourMode.AUTO), "an empty NO_COLOR is ignored");
    assertTrue(unix(Map.of("FORCE_COLOR", "1"), false).colour(ColourMode.AUTO));
    assertFalse(unix(Map.of("FORCE_COLOR", "0"), false).colour(ColourMode.AUTO));
    assertFalse(unix(Map.of("NO_COLOR", "1", "FORCE_COLOR", "1"), true).colour(ColourMode.AUTO), "NO_COLOR wins");
  }

  @Test
  void gradleRunColoursWhenATerminalTypeIsKnown() {
    assertTrue(unix(Map.of(Terminal.GRADLE_HINT, "true", "TERM", "xterm-256color"), false).colour(ColourMode.AUTO));
    assertFalse(unix(Map.of(Terminal.GRADLE_HINT, "true"), false).colour(ColourMode.AUTO), "no TERM, e.g. CI");
    assertFalse(unix(Map.of(Terminal.GRADLE_HINT, "true", "TERM", "xterm", "NO_COLOR", "1"), false)
        .colour(ColourMode.AUTO));
  }

  @Test
  void windowsNeedsATerminalThatUnderstandsEscapeCodes() {
    assertFalse(new Terminal(Map.of(), true, true).colour(ColourMode.AUTO), "classic console");
    assertTrue(new Terminal(Map.of("WT_SESSION", "1"), true, true).colour(ColourMode.AUTO), "Windows Terminal");
    assertTrue(new Terminal(Map.of("TERM_PROGRAM", "vscode", Terminal.GRADLE_HINT, "true"), false, true)
        .colour(ColourMode.AUTO));
  }

  @Test
  void explicitModesOverrideEverything() {
    assertTrue(unix(Map.of("NO_COLOR", "1", "TERM", "dumb"), false).colour(ColourMode.ALWAYS));
    assertFalse(unix(Map.of("FORCE_COLOR", "1"), true).colour(ColourMode.NEVER));
  }

  private static Terminal unix(Map<String, String> environment, boolean interactive) {
    return new Terminal(environment, interactive, false);
  }
}
