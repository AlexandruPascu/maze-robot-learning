package io.github.alexandrupascu.maze.sim;

/** The outcome of one step: the next observation, whether a wall stopped the robot, and whether it arrived. */
public record StepResult(Observation observation, boolean collided, boolean reachedTarget) {}
