package io.github.alexandrupascu.maze.sim;

import io.github.alexandrupascu.maze.Position;
import java.util.List;

/** One run's outcome. {@code trail} lists the tiles visited in order, when recording was requested. */
public record RunResult(int run, int steps, int collisions, boolean reachedTarget, List<Position> trail) {}
