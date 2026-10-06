/*
 * For this exercise I chose to implement more methods than asked in the guide to make sure that
 * I have the least amount of repeated code as I could. Also with this approach the methods aren't
 * too complex, thus easy to understand, readable and reusable.
 *
 * passageExits is just iterating through all the relative directions of the robot and counting
 * using the robot.look() the number of squares that are passages. For the nonwallExits a similar
 * approach is taken but the method is counting all the squares that are not wall around the robot
 * (passage or bennbefore).
 *
 * For the four controller I implemented deadEnd and corridor as in the guide, but for the junction
 * I decided to add the chooseRandomExit. It is used both for choosing a random beenbefore or
 * passage depending on the case. Also I took the liberty to just call the junction in the
 * crossroad method understanding that it has the exact same implementation. Other methods I added
 * are chooseSituation that calls according to the number of walls, the right controller. Also,
 * reserveHeading (self explanatory) and translateHeadingToTurn (same) that are used for backtrack.
 *
 * The control method chooses accordingly between explore and backtrack. The explore method is
 * randomly exploring, saving new junctions and the heading and also calling the backtrack when
 * it is at a deadend. The RobotData class is implemented with variable initialization,
 * RecordJunction object and constructors for both classes. A recordJunction method and additional
 * recordJunctionChecker that checks if a junction was recorded before. printJunction and
 * searchJunction to return the initial heading a robot had when it first passed a junction.
 * The backtracker extends the explore method when there is a junction with unexplored paths and
 * also goes back at junctions with paths that cannot be explored anymore the opposite way it first
 * entered that particular junction.
 *
 * For making sure that everything is running all right I used different prints throughout the code
 * to see how the methods are called and if the variables update how they were supposed to. I will
 * add the printJunction method here because I liked how I made it and to have a proof that I
 * really used it. All the testing was done with all 4 maze generators and various maze dimensions.
 *
 * // Print the number of the junction, its coordinates and the heading the robot was facing
 * private void printJunction() {
 *   // Array of strings with the possible headings
 *   String[] headings = {"NORTH", "EAST", "SOUTH", "WEST"};
 *
 *   System.out.print("Junction " + junctionCounter + " (x=" +
 *                    junctionRecorder[junctionCounter].juncX +
 *                    ", y=" + junctionRecorder[junctionCounter].juncY + ") heading ");
 *
 *   // Iterate through the array of possible headings to determine the robot's heading
 *   for (int i = 0; i < headings.length; i++) {
 *     // Being 4 possible headings use modulo 4 to decide which of the heading to print
 *     if (junctionRecorder[junctionCounter].arrived % 4 == i) {
 *       System.out.println(headings[i]);
 *     }
 *   }
 * }
 *
 *
 * The Explorer will not always find the target using this strategy if the maze is for instance
 * a loopy one. We can also call it a complex maze, where not the walls are linked together, so
 * in some situations the robot will end up in an infinite loop using this strategy.
 *
 * For the prime maze (or simple mazes - all walls linked) it should always find the target. The
 * strategy itself is like a Depth-first search for a graph, therefore the maximum steps it will
 * take in the worst case scenario is the number of junctions (vertex) + the number of squares
 * between neighbour junctions (edges).
 */

import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.ArrayList;

public class Ex1 {

  private int pollRun = 0; // Incremented after each pass
  private RobotData robotData; // Data store for junctions
  private int explorerMode = 1; // 1 = explore, 0 = backtrack

  public void controlRobot(IRobot robot) {
    // On the first move of the first run of a new maze
    if ((robot.getRuns() == 0) && (pollRun == 0)) {
      // Reset the robot data stored by initialising the robotdata class
      robotData = new RobotData();
      // Make sure the correct default behaviour is chosen in the beginning
      explorerMode = 1;
    }
    // Choose the right behaviour based on the exploreMode value: 1 = explore, 0 = backtrack
    if (explorerMode == 1) {
      exploreControl(robot);
    } else {
      backtrackControl(robot);
    }
    // Increment pollRun so that the data is not reset each time the robot moves
    pollRun++;
  }

  // Implement the explore behaviour
  private void exploreControl(IRobot robot) {
    // If the junction has not been recorded yet, save it in the robotData with the robot heading
    if (robotData.recordJunctionChecker(nonwallExits(robot), robot) == true) {
      robotData.recordJunction(robot.getLocation().x, robot.getLocation().y,
                               robot.getHeading());
      // Increase the junctionCounter
      robotData.junctionCounter++;
    }
    // Choose the right situation the robot is in and make it face the correct direction
    robot.face(chooseSituation(nonwallExits(robot), robot));
    // If the robot is at a deadend and it's not the starting point, switch to backtracking
    if ((nonwallExits(robot) < 2) && (robotData.junctionCounter != 0)) {
      explorerMode = 0;
      backtrackControl(robot);
    }
  }

  // Implement the backtrack behaviour
  private void backtrackControl(IRobot robot) {
    // If the robot is in a junction, decide whether it needs to backtrack or explore
    if (nonwallExits(robot) > 2) {
      // If there are any unvisited exits at the junction switch back to exploring
      if (passageExits(robot) > 0) {
        exploreControl(robot);
        explorerMode = 1;
        // If there aren't any unvisited exits backtrack the robot the way it firstly came through
      } else {
        robot.face(translateHeadingToTurn(robot, reverseHeading(robotData.searchJunction(
                                            robot.getLocation().x, robot.getLocation().y))));
      }
      // If the robot is not in a junction, backtrack straight through the corridor or choose a turn
    } else {
      robot.face(chooseSituation(nonwallExits(robot), robot));
    }
  }

  /*
   * To turn the robot to a desired heading get the relative direction it needs to face.
   * Relative directions (ahead, right, behind, left) are implemented as ahead, ahead + 1 and so on
   */
  private int translateHeadingToTurn(IRobot robot, int heading) {
    // Firstly calculate the difference between the desired heading and the current one
    int difference = heading - robot.getHeading();
    // Based on the difference being negative or positive there are 2 possibilities
    if (difference >= 0) {
      // When positive just add to the difference ahead and will get the right direction
      return IRobot.AHEAD + difference;
    } else {
      // When negative you need to add 4 as well
      return IRobot.AHEAD + 4 + difference;
    }
  }

  // Reverse the heading it first had when passing a junction
  private int reverseHeading(int heading) {
    /*
     * The headings north, east, south, west are implemented as north, north + 1 and so on.
     * To calculate the opposite heading, add 2 to the current one, apply modulo 4 (there are 4
     * headings, thus use modulo 4) and then add north to get the right number
     */
    return (heading + 2) % 4 + IRobot.NORTH;
  }

  // Reset the explore mode variable to the default value (1 = explore)
  private void resetExplorerMode() {
    explorerMode = 1;
  }

  /*
   * Based on the number of exits available to the robot determine the correct type of situation
   * it is in and extend the right method. This will ensure that the correct behaviour is present
   */
  private int chooseSituation(int exits, IRobot robot) {
    if (exits < 2) {
      return deadEnd(robot);
    } else if (exits == 2) {
      return corridor(robot);
    } else if (exits == 3) {
      return junction(robot);
    } else {
      return crossroads(robot);
    }
  }

  // Search for the only way a robot can go when it is in a deadend
  private int deadEnd(IRobot robot) {
    /*
     * Iterate through all relative directions of the robot ahead, right, behind, left
     * which we know that are encoded as ahead, ahead + 1 and so on
     */
    for (int i = IRobot.AHEAD; i < IRobot.LEFT + 1; i++) {
      /*
       * Using the method robot.look() implemented in the maze interface look for the relative
       * direction where there is not a wall and return it
       */
      if (robot.look(i) != IRobot.WALL) {
        return i;
      }
    }

    return 0;
  }

  /*
   * Being 2 possibilities for a corridor, check if the robot can move forward or it is at a turn
   * and needs to find if it's a left or a right
   */
  private int corridor(IRobot robot) {
    // If in the front of the robot there is not a wall, it needs to go straight forward
    if (robot.look(IRobot.AHEAD) != IRobot.WALL) {
      return IRobot.AHEAD;
    } else {
      /*
       * If in the front of the robot there is a wall, it means it is at a turn and needs to find
       * it. Iterate through relative directions of the robot right and left which we know that
       * are encoded as right and right + 2
       */
      for (int i = IRobot.RIGHT; i < IRobot.LEFT + 1; i += 2) {
        /*
         * Use the method robot.look() implemented in the maze interface to look for the relative
         * direction where there is not a wall and return it
         */
        if (robot.look(i) != IRobot.WALL) {
          return i;
        }
      }
    }

    return 0;
  }

  /*
   * When the robot arrives at a junction, it needs to check if there are any exits not visited
   * and then choose one randomly if there are more. If there are none, it will choose randomly
   * between the exits that it has visited already
   */
  private int junction(IRobot robot) {
    // check if there are unvisited exits with passageExits, then choose randomly between them
    if (passageExits(robot) != 0) {
      return chooseRandomExit(robot, IRobot.PASSAGE);
    } else {
      // if there aren't any unvisited exits, choose randomly between visited exits
      return chooseRandomExit(robot, IRobot.BEENBEFORE);
    }
  }

  /*
   * The same behaviour from junction method is needed for the crossroads method because they are
   * just junctions with one more exit, therefore when a crossroad is met, extend junction method
   */
  private int crossroads(IRobot robot) {
    return junction(robot);
  }

  /*
   * Taking as a parameter exitType, meaning beenbefore or passage, choose randomly the available
   * exit from the junction, visited or unvisited
   */
  private int chooseRandomExit(IRobot robot, int exitType) {
    int randomNumber;
    // array list of integers that will contain possible exits
    ArrayList<Integer> exits = new ArrayList<Integer>();
    /*
     * Iterate through all relative directions of the robot ahead, right, behind, left
     * which are encoded as ahead, ahead + 1 and so on
     */
    for (int i = IRobot.AHEAD; i < IRobot.LEFT + 1; i++) {
      /*
       * Use the method robot.look(), implemented in the maze interface, to check if the type of
       * the square is of exitType (beenbefore or passage) and add to the list when there's a match
       */
      if (robot.look(i) == exitType) {
        exits.add(i);
      }
    }
    /*
     * There are a number of 1 to 4 possible exits for a junction, thus get a random number between
     * 0 and 3 to choose a random exit from the list. Use the size of the list (0 to 3), multiply
     * by a random generated real number from 0 to 1 and then floor. This will ensure a natural
     * number in the (0,3) interval.
     *
     */
    randomNumber = (int) Math.floor(Math.random() * exits.size());
    // Return from the array list the random selected exit of exitType for the junction
    return exits.get(randomNumber);

  }

  /*
   * When the robot is at a junction, count how many of the exits haven't been searched already
   * The type of square to look for is encoded as passage in the maze interface.
   */
  private int passageExits(IRobot robot) {

    int numberOfPassage = 0;
    /*
     * Iterate through all relative directions of the robot ahead, right, behind, left
     * which we know that are encoded as ahead, ahead + 1 and so on
     */
    for (int i = IRobot.AHEAD; i < IRobot.LEFT + 1; i++) {
      /*
       * Use the method robot.look(), implemented in the maze interface, to check if the type of
       * the square is passage (unvisited) and increase the counter when there's a match
       */
      if (robot.look(i) == IRobot.PASSAGE) {
        numberOfPassage++;
      }
    }

    return numberOfPassage;
  }

  /*
   * Count how many nonwallExist there are at a junction (square needs to be
   * visited or never visited in every direction with an exit)
   */
  private int nonwallExits(IRobot robot) {

    int numberOfWalls = 0;
    /*
     * Iterate through all relative directions of the robot ahead, right, behind, left
     * which are encoded as ahead, ahead + 1 and so on
     */
    for (int i = IRobot.AHEAD; i < IRobot.LEFT + 1; i++) {
      /*
       * There are 2 types of nonwallExists (visited and not visited), use the method
       * robot.look(), implemented in the maze interface to check if the type of the square
       * is wall and increase the counter when there's a match
       */
      if (robot.look(i) == IRobot.WALL) {
        numberOfWalls ++;
      }
    }
    /*
     * Because the wall exits were counted, subtract, from the total possible number of exits,
     * which is 4, the number of them that are wall. Therefore find the number of nonWallExits
     */
    return 4 - numberOfWalls;
  }

  /*
   * When the robot is at a junction, count how many of the exits have been searched already.
   * The type of square that the method looks for is encoded as beenbefore in the maze interface.
   */
  private int beenbeforeExits(IRobot robot) {

    int numberOfBeenbefore = 0;
    /*
     * Iterate through all relative directions of the robot ahead, right, behind, left
     * which are encoded as ahead, ahead + 1 and so on
     */
    for (int i = IRobot.AHEAD; i < IRobot.LEFT + 1; i++) {
      /*
       * Use the method robot.look(), implemented in the maze interface, to check if the type of
       * the square is beenbefore (visited) and increase the counter when there's a match
       */
      if (robot.look(i) == IRobot.BEENBEFORE) {
        numberOfBeenbefore ++;
      }
    }

    return numberOfBeenbefore;
  }

  private class RobotData {

    private int maxJunctions = 10000; // Max number likely to occur
    private int junctionCounter; // No. of junctions stored
    private JunctionRecorder[] junctionRecorder; // Array of JunctionRecorder objects

    // RobotData constructor, initializes an array of JuncitonRecorder objects and variables to 0
    private RobotData() {
      this.junctionCounter = 0;
      this.junctionRecorder = new JunctionRecorder[maxJunctions];
    }

    private class JunctionRecorder {
      private int juncX; // X-coordinate of the junctions
      private int juncY; // Y-coordinate of the junctions
      private int arrived; // Heading the robot first arrived from

      // Constructor for JunctionRecorder taking as parameters the X and Y coordinates and a heading
      private JunctionRecorder(int junctionCoordinateX, int unctionCoordinateY, int heading) {
        this.juncX = junctionCoordinateX;
        this.juncY = unctionCoordinateY;
        this.arrived = heading;
      }
    }

    /*
     * Record the heading the robot was facing when first encountering the junction and the
     * X and Y coordinates of it
     */
    private void recordJunction(int junctionCoordinateX, int junctionCoordinateY, int heading) {
      junctionRecorder[junctionCounter] = new JunctionRecorder(junctionCoordinateX,
          junctionCoordinateY, heading);
    }

    /*
     * Check when a robot is passing a junction if it was previously recorded and return true
     * if it's a new junctions, otherwise false
     */
    private boolean recordJunctionChecker(int exits, IRobot robot) {
      // Make sure it is a junction by having 3 or 4 exits
      if (exits == 3 || exits == 4) {
        // Iterate through all the recorded junctions
        for (int i = 0; i < junctionCounter; i++) {
          // Compare if X and Y coordinates are the same with junctions already recorded
          if (robot.getLocation().x == junctionRecorder[i].juncX &&
              robot.getLocation().y == junctionRecorder[i].juncY) {
            return false;
          }
        }
        return true;
      }
      return false;
    }

    /*
     * Search through the recorded junctions which of them the robot came back to return the
     * initial heading it had
     */
    private int searchJunction(int junctionCoordinateX, int junctionCoordinateY) {
      // Iterate through all the recorded junction array
      for (int i = 0; i < junctionCounter; i++) {
        // Find the correct junction by comparing the current X and Y coordinates to the others'
        if (junctionCoordinateX == junctionRecorder[i].juncX &&
            junctionCoordinateY == junctionRecorder[i].juncY) {
          return junctionRecorder[i].arrived;
        }
      }
      return -1;
    }

    // reset the junction counter to 0
    private void resetJunctionCounter() {
      junctionCounter = 0;
    }
  }

  /*
   * When pressing the Reset button in the maze interface call this method and use it to make
   * sure it resets the junction counter to 0 and explore mode variable to default (explore)
   */
  public void reset() {
    robotData.resetJunctionCounter();
    resetExplorerMode();
  }

}