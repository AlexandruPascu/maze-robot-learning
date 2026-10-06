/*
 * For the Grand finale I used the implementation from exercise 2, the one with the Arraylist
 * capable of solving loopy mazes as well. Because the robot records only the route it is sure it
 * leads to the target, I only compare the current heading of the robot with the saved one from
 * the first run (when it learnt the route) and make it face that heading.
 *
 * The only downsize of my implementation is that I need for the last step to see where is the
 * target in order make the robot make the final step. However with this way, I don't initialize any
 * other variable or data structure, thus making sure I save space and don't have any extra data
 * that is not used or it is redundant. I tried to think of a method to add and adapt as little as
 * I could the already existing solution I had from previous exercises, thus recycling as much code
 * as possible and not wasting time implementing many new features. I always consider that thinking
 * how to adapt and reuse is way better than always making new things (upcycle).
 *
 * The biggest issues with this implementation is that using a version of Tremaux algorithm and
 * graph search, it doesn't give me the shortest path in a loopy maze, but nonetheless a shorter one
 * than it firstly took to reach the goal. This is because there are too many solutions to the loopy
 * maze and it cannot figure it out every step that is not necessary to take to reach the target.
 * However, for prime mazes, those without multiple loops the robot will always take the shortest
 * path in the second run using the learnt route from the first try.
 *
 * For making sure that everything is running all right I used different prints throughout the code
 * to see how the methods are called and if the variables update how they were supposed to. I will
 * add the printJunction method here because I liked how I made it and to have a proof that I
 * really used it. All the testing was done with all 4 maze generators and various maze dimensions.
 *
 * // Print the number of the junction, its coordinates and the heading the robot was facing
 * private void printJunction() {
 *    // Array of strings with the possible headings
 *    String[] headings = {"NORTH", "EAST", "SOUTH", "WEST"};
 *    // Last index of the arraylist
 *    int lastIndex = junctionRecorder.juncX.size() - 1;
 *    System.out.print("Junction " + lastIndex + " (x=" + junctionRecorder.juncX.get(lastIndex) +
 *                     ", y=" + junctionRecorder.juncY.get(lastIndex) + ") heading ");
 *    // Iterate through the array of possible headings to determine the robot's heading
 *    for (int i = 0; i < headings.length; i++) {
 *      // Being 4 possible headings use modulo 4 to decide which of the heading to print
 *      if (junctionRecorder.arrived.get(size) % 4 == i) {
 *        System.out.println(headings[i]);
 *      }
 *    }
 *  }
 *
 */

import uk.ac.warwick.dcs.maze.logic.IRobot;
import java.util.ArrayList;

public class Explorer {

  private int pollRun = 0; // Incremented after each pass
  private RobotData robotData; // Data store for junctions
  private int explorerMode = 1; // 1 = explore, 0 = backtrack,

  public void controlRobot(IRobot robot) {
    // On the first move of the runs before the learnt route of a new maze
    if ((robot.getRuns() % 2 == 0) && (pollRun == 0)) {
      // Reset the robot data stored by initialising the robotdata class
      robotData = new RobotData();
      // Reset the junction recorder stored by initialising the junctionrecorder object
      robotData.resetJunctionRecorder();
      // Make sure the correct default behaviour is chosen in the beginning
      explorerMode = 1;
    }
    // If the robot had a run through this maze, use the learnt route to target
    if (robot.getRuns() % 2 == 1) {
      learntCrontrol(robot);
      //If it's the first run through a maze, build the route to the target
    } else {
      // Choose the right behaviour based on the exploreMode value: 1 = explore, 0 = backtrack
      if (explorerMode == 1) {
        exploreControl(robot);
      } else {
        backtrackControl(robot);
      }
      // Increment pollRun so that the data is not reset each time the robot moves
      pollRun++;
    }
  }

  // Use the route that the robot learnt is reaching the target without exploring again
  private void learntCrontrol(IRobot robot) {
    // If the robot is at a deadend in the beginning, make it advance one step without collision
    if (nonwallExits(robot) == 1) {
      robot.face(chooseSituation(nonwallExits(robot), robot));
    } else {
      /*
       * If the robot is not at a deadend and it's not the last step, compare it's heading to the
       * recorded heading from this step it has, and translate it to a facing
       */
      if (robotData.junctionRecorder.arrived.size() > 1) {
        robot.face(translateHeadingToTurn(robot, robotData.junctionRecorder.arrived.get(1)));
        // Remove the first element of junction recorder
        robotData.removeFirstElementJunction();
        // If the robot is at the last step, see where the target is and make the robot face it
      } else {
        robot.face(translateHeadingToTurn(robot, findTarget(robot)));
        // Remove the first element of junction recorder
        robotData.removeFirstElementJunction();
      }
    }
  }

  // Implement the explore behaviour
  private void exploreControl(IRobot robot) {
    // If the route has not been recorded yet, save it in the robotData with the robot heading
    if (robotData.recordJunctionChecker(nonwallExits(robot), robot) == true) {
      robotData.recordJunction(robot.getLocation().x, robot.getLocation().y,
                               robot.getHeading());
    }
    // Choose the right situation the robot is in and make it face the correct direction
    robot.face(chooseSituation(nonwallExits(robot), robot));
    // If the robot is at a deadend and it's not the starting point, switch to backtracking
    if ((nonwallExits(robot) < 2) && (robotData.junctionRecorder.arrived.size() != 0)) {
      explorerMode = 0;
      backtrackControl(robot);
    }
  }

  // Implement the backtrack behaviour
  private void backtrackControl(IRobot robot) {
    // If the robot is not a corridor, decide whether it needs to backtrack or explore
    if (nonwallExits(robot) > 1) {
      // If there are any unvisited exits at the moment near the robot switch back to exploring
      if (passageExits(robot) > 0) {
        exploreControl(robot);
        explorerMode = 1;
        // If there aren't any unvisited exits backtrack the robot the way it firstly came through
      } else {
        robot.face(translateHeadingToTurn(robot, reverseHeading(robotData.searchJunction(
                                            robot.getLocation().x, robot.getLocation().y))));
        /*
         * After the robot reversed through a route, it will never come back therefore remove
         * the last element from the arraylist
         */
        robotData.removeJunction();
      }
    }
  }

  // Return the heading in which the target is relative to the robot position
  private int findTarget(IRobot robot) {
    // Robot is below the target
    if (robot.getLocation().y > robot.getTargetLocation().y) {
      return IRobot.NORTH;
      // Robot is above the target
    } else if (robot.getLocation().y < robot.getTargetLocation().y) {
      return IRobot.SOUTH;
    }
    // Robot is left to the target
    if (robot.getLocation().x < robot.getTargetLocation().x) {
      return IRobot.EAST;
      // Robot is right to the target
    } else if (robot.getLocation().x > robot.getTargetLocation().x) {
      return IRobot.WEST;
    }
    return -1;
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

  // Reset the pollrun variable to default 0
  private void resetPollRun() {
    pollRun = 0;
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

    private JunctionRecorder junctionRecorder; // JunctionRecorder object

    // RobotData constructor, initializes the JuncitonRecorder object
    private RobotData() {
      this.junctionRecorder = new JunctionRecorder();
    }

    private class JunctionRecorder {
      ArrayList<Integer> juncX; // X-coordinate of the junctions
      ArrayList<Integer> juncY; // Y-coordinate of the junctions
      ArrayList<Integer> arrived; // Heading the robot first arrived from

      // Constructor for JunctionRecorder, initialize all the arraylists
      private JunctionRecorder() {
        this.juncX = new ArrayList<Integer>();
        this.juncY = new ArrayList<Integer>();
        this.arrived = new ArrayList<Integer>();
      }
    }

    /*
     * Record the heading the robot was facing when first encountering the junction and the
     * X and Y coordinates of it using the arraylists
     */
    private void recordJunction(int junctionCoordinateX, int junctionCoordinateY, int heading) {
      junctionRecorder.juncX.add(junctionCoordinateX);
      junctionRecorder.juncY.add(junctionCoordinateY);
      junctionRecorder.arrived.add(heading);
    }

    /*
     * Check when a robot is passing a route if it was previously recorded and return true
     * if it's a new route, otherwise false
     */
    private boolean recordJunctionChecker(int exits, IRobot robot) {
      // Make sure it is not a deadend
      if (exits > 1) {
        // if there are no recorded heading save it
        if (junctionRecorder.juncX.size() - 1 == - 1) {
          return true;
          // Compare if X and Y coordinates are the same with the last heading already recorded
        } else if (junctionRecorder.juncX.get(junctionRecorder.juncX.size() - 1) ==
                   robot.getLocation().x &&
                   junctionRecorder.juncY.get(junctionRecorder.juncY.size() - 1) ==
                   robot.getLocation().y) {
          return false;
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
      // Find the correct junction by comparing the current X and Y coordinates to the others'
      if (junctionRecorder.juncX.get(junctionRecorder.juncX.size() - 1) == junctionCoordinateX &&
          junctionRecorder.juncY.get(junctionRecorder.juncY.size() - 1) == junctionCoordinateY) {
        return junctionRecorder.arrived.get(junctionRecorder.juncX.size() - 1);
      }
      return -1;
    }

    // Remove the last element of every arraylist from junctionRecorder
    private void removeJunction() {
      junctionRecorder.juncX.remove(junctionRecorder.juncX.size() - 1);
      junctionRecorder.juncY.remove(junctionRecorder.juncY.size() - 1);
      junctionRecorder.arrived.remove(junctionRecorder.arrived.size() - 1);
    }

    // Remove the first element of every arraylist from junctionRecorder
    private void removeFirstElementJunction() {
      junctionRecorder.juncX.remove(0);
      junctionRecorder.juncY.remove(0);
      junctionRecorder.arrived.remove(0);

    }

    // After each run, reset the JunctionRecorder data
    private void resetJunctionRecorder() {
      junctionRecorder = new JunctionRecorder();
    }
  }

  /*
   * When pressing the Reset button in the maze interface call this method and use it to make
   * sure it resets the JunctionRecorder object and explore mode variable to default (explore)
   */
  public void reset() {
    resetExplorerMode();
    resetPollRun();
  }

}

//merge