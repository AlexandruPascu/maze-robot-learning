package io.github.alexandrupascu.maze.cli;

import java.io.Console;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the program's output is going, as far as colour is concerned. {@code ./gradlew run} pipes
 * output through Gradle, which hides the terminal, so the build sets {@value #GRADLE_HINT} for that
 * task and colour is decided from the remaining environment.
 */
record Terminal(Map<String, String> environment, boolean interactive, boolean windows) {
  static final String GRADLE_HINT = "MAZE_ROBOT_VIA_GRADLE";
  /** The only environment variables colour detection reads. */
  static final List<String> VARIABLES =
      List.of("NO_COLOR", "FORCE_COLOR", "TERM", GRADLE_HINT, "WT_SESSION", "TERM_PROGRAM", "ANSICON", "ConEmuANSI");

  Terminal {
    environment = Map.copyOf(environment);
  }

  // Copies just those variables, so nothing else from the environment can leak into output.
  static Terminal detect() {
    Map<String, String> environment = new HashMap<>();
    for (String name : VARIABLES) {
      String value = System.getenv(name);
      if (value != null) {
        environment.put(name, value);
      }
    }
    return new Terminal(environment, stdoutIsTerminal(), System.getProperty("os.name", "").startsWith("Windows"));
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
