package io.github.alexandrupascu.maze.coursework;

import java.awt.Point;

/**
 * The robot interface the 2022 coursework controller calls, re-declared here so it runs without
 * the university's framework, which is not included. Only the members the controller uses exist.
 * The constant values satisfy the arithmetic the controller relies on: four consecutive headings
 * starting at a multiple of four, four consecutive clockwise directions, and three tile states.
 */
public interface IRobot {
  int NORTH = 1000;
  int EAST = 1001;
  int SOUTH = 1002;
  int WEST = 1003;

  int AHEAD = 2000;
  int RIGHT = 2001;
  int BEHIND = 2002;
  int LEFT = 2003;

  int WALL = 3000;
  int PASSAGE = 3001;
  int BEENBEFORE = 3002;

  int look(int direction);

  void face(int direction);

  int getHeading();

  Point getLocation();

  Point getTargetLocation();

  int getRuns();
}
