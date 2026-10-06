package io.github.alexandrupascu.maze.sim;

import io.github.alexandrupascu.maze.Position;

/**
 * What a robot is told about a new maze: the arena size and the start and target tiles, never the
 * walls. {@code seed} drives the robot's own random choices so that every run can be replayed.
 */
public record MazeInfo(int width, int height, Position start, Position target, long seed) {}
