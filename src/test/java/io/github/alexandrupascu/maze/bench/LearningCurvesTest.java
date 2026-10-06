package io.github.alexandrupascu.maze.bench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexandrupascu.maze.bench.LearningCurves.Curve;
import io.github.alexandrupascu.maze.bench.LearningCurves.FamilyResult;
import io.github.alexandrupascu.maze.bench.LearningCurves.Learner;
import io.github.alexandrupascu.maze.bench.LearningCurves.Result;
import java.util.List;
import org.junit.jupiter.api.Test;

class LearningCurvesTest {
  private static final LearningCurves.Settings SMALL = new LearningCurves.Settings(4, 6, 6, 11);

  @Test
  void identicalSettingsGiveIdenticalReports() {
    Result first = LearningCurves.run(SMALL);
    Result second = LearningCurves.run(SMALL);
    assertEquals(LearningReport.curvesCsv(first), LearningReport.curvesCsv(second));
    assertEquals(LearningReport.summaryCsv(first), LearningReport.summaryCsv(second));
    assertEquals(LearningReport.markdown(first), LearningReport.markdown(second));
  }

  @Test
  void everyMazeTypeHasACurvePerRobotWithOneMeanPerRun() {
    Result result = LearningCurves.run(SMALL);
    assertEquals(LearningCurves.families().size(), result.families().size());
    for (FamilyResult family : result.families()) {
      assertEquals(Learner.values().length, family.curves().size());
      for (Curve curve : family.curves()) {
        assertEquals(SMALL.runs(), curve.meanSteps().size());
        for (double steps : curve.meanSteps()) {
          assertTrue(steps >= family.shortest() - 1e-9, curve.learner().id() + " beat the shortest route");
        }
      }
    }
    String csv = LearningReport.curvesCsv(result);
    assertEquals(1 + result.families().size() * Learner.values().length * SMALL.runs(), csv.split("\n").length);
  }

  @Test
  void theRouteProverConvergesByItsSecondRunEverywhere() {
    for (FamilyResult family : LearningCurves.run(SMALL).families()) {
      Curve prover = family.curves().get(Learner.ROUTE_PROVER.ordinal());
      assertEquals(SMALL.mazes(), prover.converged());
      assertTrue(prover.shortestFromRun() >= 1 && prover.shortestFromRun() <= 2);
      assertEquals(family.shortest(), prover.meanSteps().get(1), 1e-9);
    }
  }

  @Test
  void tablesShowTheFirstRunsThenEveryTenthAndTheLast() {
    assertEquals(List.of(1, 2, 3, 5, 10, 20, 30), LearningReport.shownRuns(30));
    assertEquals(List.of(1, 2, 3, 5, 10, 12), LearningReport.shownRuns(12));
    assertEquals(List.of(1, 2, 3), LearningReport.shownRuns(3));
  }
}
