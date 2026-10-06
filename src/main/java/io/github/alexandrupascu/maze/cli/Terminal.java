package io.github.alexandrupascu.maze.cli;

import java.io.Console;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Where the program's output is going, as far as colour is concerned. {@code ./gradlew run} pipes
 * output through Gradle, which hides the terminal, so the build sets {@value #GRADLE_HINT} for that
 * task and colour is decided from the remaining environment.
 */
record Terminal(Map<String, String> environment, boolean interactive, boolean windows) {
  static final String GRADLE_HINT = "MAZE_ROBOT_VIA_GRADLE";

  Terminal {
    environment = Map.copyOf(environment);
  }

  static Terminal detect() {
    return new Terminal(System.getenv(), stdoutIsTerminal(), System.getProperty("os.name", "").startsWith("Windows"));
  }

  boolean colour(ColourMode mode) {
    return switch (mode) {
      case ALWAYS -> true;
      case NEVER -> false;
      case AUTO -> automaticColour();
    };
  }

  // NO_COLOR (no-color.org) and FORCE_COLOR are common conventions; an explicit --color beats both.
  private boolean automaticColour() {
    if (!environment.getOrDefault("NO_COLOR", "").isEmpty()) {
      return false;
    }
    String force = environment.getOrDefault("FORCE_COLOR", "");
    if (!force.isEmpty() && !force.equals("0") && !force.equalsIgnoreCase("false")) {
      return true;
    }
    String term = environment.getOrDefault("TERM", "");
    if (term.equals("dumb") || !(interactive || "true".equals(environment.get(GRADLE_HINT)))) {
      return false;
    }
    if (windows) {
      // The classic Windows console shows escape codes literally; these terminals interpret them.
      return !term.isEmpty()
          || environment.containsKey("WT_SESSION")
          || environment.containsKey("TERM_PROGRAM")
          || environment.containsKey("ANSICON")
          || "ON".equals(environment.get("ConEmuANSI"));
    }
    return interactive || !term.isEmpty();
  }

  private static boolean stdoutIsTerminal() {
    Console console = System.console();
    if (console == null) {
      return false;
    }
    try {
      // From Java 22, System.console() also exists when output is redirected; isTerminal() tells.
      Method isTerminal = Console.class.getMethod("isTerminal");
      return (Boolean) isTerminal.invoke(console);
    } catch (NoSuchMethodException javaTwentyOne) {
      return true;
    } catch (ReflectiveOperationException error) {
      return false;
    }
  }
}
