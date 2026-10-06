package io.github.alexandrupascu.maze.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexandrupascu.maze.bench.LearningCurves;
import io.github.alexandrupascu.maze.generate.Layout;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class LearningChartTest {
  private static final LearningCurves.Family PRIM = new LearningCurves.Family(Layout.PRIM, 0.1);

  @Test
  void drawsOneLinePerRobotInWellFormedSvgForBothThemes() throws Exception {
    LearningCurves.Result result = LearningCurves.run(new LearningCurves.Settings(3, 5, 6, 4));
    String svg = LearningChart.render(result, PRIM);
    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
    assertEquals("svg", document.getDocumentElement().getTagName());
    assertEquals(LearningCurves.Learner.values().length, document.getElementsByTagName("polyline").getLength());
    assertTrue(svg.contains("@media (prefers-color-scheme:dark)"));
    assertTrue(svg.contains("Learning curves: 6×6 Prim mazes with 10% loops"));
    assertEquals(svg, LearningChart.render(result, PRIM), "the drawing is deterministic");
  }

  @Test
  void logTicksAreCleanValuesCoveringTheData() {
    assertEquals(List.of(50.0, 100.0, 200.0, 500.0, 1000.0, 2000.0, 5000.0), LearningChart.ticks(57.3, 2828.4));
    assertEquals(List.of(1.0, 2.0, 5.0), LearningChart.ticks(1, 3));
    assertEquals(List.of(20.0, 50.0, 100.0), LearningChart.ticks(24.5, 100));
  }

  @Test
  void rejectsAMazeTypeThatWasNotMeasured() {
    LearningCurves.Result result = LearningCurves.run(new LearningCurves.Settings(1, 2, 4, 1));
    assertThrows(IllegalArgumentException.class,
        () -> LearningChart.render(result, new LearningCurves.Family(Layout.PRIM, 0.5)));
  }
}
