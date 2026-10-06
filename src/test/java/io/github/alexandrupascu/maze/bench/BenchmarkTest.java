package io.github.alexandrupascu.maze.bench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexandrupascu.maze.agents.Agent;
import io.github.alexandrupascu.maze.bench.Benchmark.AgentSummary;
import io.github.alexandrupascu.maze.bench.Benchmark.Report;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class BenchmarkTest {
  private static final Benchmark.Settings SMALL = new Benchmark.Settings(6, 7, List.of(5, 8));

  @Test
  void identicalSettingsGiveIdenticalReports() {
    Report first = Benchmark.run(SMALL);
    Report second = Benchmark.run(SMALL);
    assertEquals(ReportWriter.agentsCsv(first), ReportWriter.agentsCsv(second));
    assertEquals(ReportWriter.searchCsv(first), ReportWriter.searchCsv(second));
    assertEquals(ReportWriter.markdown(first), ReportWriter.markdown(second));
  }

  @Test
  void coversEveryConfigurationAndRobotWithoutFailures() {
    Report report = Benchmark.run(SMALL);
    int configs = Benchmark.LOOPS.size() * Benchmark.LAYOUTS.size() * SMALL.sizes().size();
    assertEquals(configs * Agent.values().length, report.agents().size());
    assertEquals(configs, report.searches().size());
    for (AgentSummary row : report.agents()) {
      assertEquals(0, row.failures(), row.agent().id() + " on " + row.config().label());
      assertEquals(0, row.collisions(), row.agent().id() + " on " + row.config().label());
      assertEquals(SMALL.mazes(), row.mazes());
    }
  }

  @Test
  void knownGuaranteesHoldInTheSummaries() {
    for (AgentSummary row : Benchmark.run(SMALL).agents()) {
      if (row.agent() == Agent.A_STAR) {
        assertEquals(row.shortestSteps(), row.run1Steps());
        assertEquals(row.shortestSteps(), row.run2Steps());
      }
      if (row.config().loops() == 0 && row.agent() != Agent.A_STAR) {
        assertEquals(row.mazes(), row.run2Shortest(), row.agent().id() + " replays a shortest route on perfect mazes");
      }
    }
  }

  @Test
  void numbersIgnoreTheDefaultLocale() {
    Locale saved = Locale.getDefault();
    try {
      Locale.setDefault(Locale.GERMANY);
      String csv = ReportWriter.agentsCsv(Benchmark.run(new Benchmark.Settings(2, 1, List.of(4))));
      for (String line : csv.split("\n")) {
        assertEquals(13, line.split(",", -1).length, line);
      }
      assertTrue(csv.contains("."), "decimals use a dot");
    } finally {
      Locale.setDefault(saved);
    }
  }
}
