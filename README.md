# Maze Robot Learning

[![Build and test](https://github.com/AlexandruPascu/maze-robot-learning/actions/workflows/build.yml/badge.svg)](https://github.com/AlexandruPascu/maze-robot-learning/actions/workflows/build.yml)

A robot explores a maze it has never seen, then runs it again using what it learned. This
repository runs my 2022 university coursework robot alongside a map-building planner and an A\*
oracle on thousands of seeded mazes and measures what each one gains on its second run. The
simulator is self-contained Java 21, deterministic on every platform, and ready for learned agents.

![Two copies of the same 15x15-cell maze. The coursework robot's second-run route loops back on itself; the map planner's is nearly direct.](docs/maze.svg)

*One Prim maze, 15×15 cells with 10% of internal walls removed, seed 1. Regenerate it with
`./gradlew run --args="render"`.*

## Results

Mean steps over 300 seeded mazes of 15×15 cells for each layout. Run 1 explores the unseen maze; run
2 starts again from the same place with whatever the robot remembered. "Shortest" is the A\* route.

| Maze, 15×15 cells | Shortest | Coursework run 1 | Coursework run 2 | Map planner run 1 | Map planner run 2 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Prim, no loops | 59.4 | 437.9 | 59.4 | 303.1 | 59.4 |
| Backtracker, no loops | 183.7 | 411.6 | 183.7 | 183.7 | 183.7 |
| Prim, 10% loops | 57.2 | 640.8 | 130.4 | 187.5 | 74.0 |
| Backtracker, 10% loops | 80.7 | 667.0 | 265.0 | 127.1 | 101.9 |
| Prim, 25% loops | 56.4 | 830.0 | 261.8 | 118.5 | 72.9 |
| Backtracker, 25% loops | 61.2 | 886.6 | 395.0 | 86.3 | 75.5 |

The [full results](reports/summary.md) also cover 7×7 and 30×30 mazes. They show:

- **Perfect mazes have one route, and both robots replay it.** Run 2 was a shortest route in every
  maze, for both robots.
- **The 2022 claim of "4x fewer steps on the second run" depends on the maze.** The median gain is
  3.7× on 7×7 Prim mazes and 14.7× at 30×30. It is only 1.4–2.4× on backtracker mazes, whose single
  route already threads through much of the maze.
- **Loops break replay.** The coursework robot repeats its exploration trail, detours included. Its
  run 2 averages 1.7–13.1× the shortest route on mazes with loops. The map planner plans a fresh
  route on its map and stays within 1.1–1.5×.
- **On backtracker mazes the map planner's first run is already a shortest route.** This held in
  all 900 benchmark mazes, and a test checks it with random targets. It follows from how
  depth-first carving works: every wall separates a cell from one of its ancestors in the carving
  tree. Each side branch is therefore sealed by walls the robot saw on its way in, and an
  optimistic planner never enters one. Prim mazes have no such guarantee.
- **A\* finds the same routes as Dijkstra's algorithm while expanding 8–77% fewer tiles.** The
  heuristic helps least on backtracker mazes without loops, whose winding corridors point away from
  the target.

## Quick start

You need JDK 21 or newer. The Gradle wrapper downloads Gradle itself. On Windows, use `gradlew.bat`.

```sh
./gradlew build                                                    # compile and run the tests
./gradlew run --args="show --agent coursework --loops 0.25 --seed 4" # one maze in the terminal
./gradlew run --args="benchmark"                                   # about 10 s; rewrites reports/
./gradlew run --args="render"                                      # rewrites docs/maze.svg
./gradlew run --args="help"                                        # every option
```

`show` draws the maze two characters per tile, so it looks square, under a legend. It compares the
run-2 route with the shortest route that overlaps it most:

| Mark | Meaning |
| --- | --- |
| `##` | Wall |
| `..` | Visited on run 1 only |
| `**` | Run 2 on a shortest route |
| `~~` | Run 2 detour |
| `++` | Shortest route that run 2 missed |
| `S`, `T` | Start and target |

Options select:

- the robot: `coursework`, `map-planner` or `a-star`;
- the layout: `prim` or `backtracker`;
- the size in cells, the share of loops, a corner or random target, and the seed.

The drawing is in colour when the output reaches a terminal, including through `./gradlew run`.
Colour only restyles the same characters. `--color always` or `--color never` overrides the
detection, and so does `NO_COLOR=1`; an explicit flag wins.

## The robots

**Coursework explorer (2022).** My original CS118 solution, run unchanged apart from an injectable
random source. On run 1 it explores at random, depth first. It prefers exits it has not visited,
backtracks from dead ends, and keeps a stack of the cells on its current route with the heading it
arrived in. On run 2 it replays that stack.

**Map planner.** Records every wall and passage it senses in a map that it keeps between runs.

- **Run 1:** it plans a shortest route to the target as if every unknown tile were open, and replans
  whenever it finds a wall on that route. Robot navigation calls this planning under the freespace
  assumption.
- **Run 2:** it plans only through tiles it has seen to be open, so it never gambles on an unexplored
  shortcut. That is also why it sometimes misses one, the exploration-versus-exploitation trade-off
  that the learning agents will tackle.

**A\* oracle.** Is handed the full map and follows an A\* route using the Manhattan distance, which
never overestimates on this grid. It sets the lower bound for the other robots. The benchmark also
runs Dijkstra's algorithm, which is A\* without the heuristic, to measure how much work the
heuristic saves.

## The simulator

- **Mazes:** a maze is a grid of wall and floor tiles with cells on odd coordinates. A perfect maze
  is carved by randomized Prim's algorithm or a recursive backtracker, starting from the top-left
  cell. Loops come from knocking through a share of the remaining walls between cells. The target is
  the bottom-right cell or a random one.
- **Sensing and moving:** sensing matches the coursework framework. Before each step a robot sees its
  four neighbouring tiles as wall, passage or been-before, plus its position, heading, the target's
  position and the run number. It then turns and moves one tile; walking into a wall still costs a
  step.
- **The `Robot` interface:** robots implement `beginMaze`, `beginRun`, `act` and `afterStep`.
  `afterStep` reports every move's outcome, ready for agents that learn from feedback.
- **Determinism:** all randomness is seeded through `java.util.Random`, whose algorithm is specified
  exactly. The same seed therefore reproduces a maze and a robot's choices on any JVM and operating
  system. CI regenerates the reports and the image on Linux, macOS and Windows and fails if a single
  byte changes.

## Code and tests

| Path | Contents |
| --- | --- |
| `src/main/java/io/github/alexandrupascu/maze/` | Grid model: `Maze`, `Position`, `Heading`, `Direction`, `Sight` |
| `…/generate/` | `MazeSpec`: Prim and backtracker carving, loops, target placement |
| `…/sim/` | `MazeEnvironment`, `Robot`, `Runner` and the observation types |
| `…/coursework/` | The ported 2022 controller, its adapter and the `IRobot` re-declaration |
| `…/agents/` | `MapPlanner`, `AStarOracle` and the agent registry |
| `…/search/` | A\* and Dijkstra's algorithm on a known maze |
| `…/bench/`, `…/render/`, `…/cli/` | Benchmark and reports, SVG and text drawings, command line |
| `coursework/` | The 2022 files, byte for byte ([notes](coursework/README.md)) |
| `reports/` | Benchmark results as CSV and Markdown |

`./gradlew build` compiles with all warnings as errors and runs 49 JUnit tests. They cover:

- **Mazes:** generator properties (spanning trees, loop counts, recorded seeds).
- **Simulation:** the simulator's movement and sensing rules.
- **Robots:** that the port matches the original code, every robot's guarantees on perfect and
  looped mazes, and the depth-first property above.
- **Search:** that A\* and Dijkstra agree with breadth-first search, and that the route used for
  drawings is a shortest route overlapping run 2 as much as possible.
- **Drawings:** the terminal marks, that colour changes nothing but styling, and when colour is used
  (terminals, `./gradlew run`, `NO_COLOR`, `FORCE_COLOR`, Windows consoles).
- **Reports and CLI:** report determinism, including under a non-English locale, and the command
  line.

## Background and credit

This project began as robot-maze coursework for CS118 at the University of Warwick, uploaded in
2022. The originals are in [coursework/](coursework/), unchanged, with notes on what each file is.
They were written against the department's maze framework, which belongs to the university and is
not included. The simulator here is an independent implementation of what the robot needs: four-way
sensing, been-before marks and repeated runs.

The 2022 description presented the robot as a mix of Dijkstra's, Trémaux's and A\* algorithms
forming an "iterative machine learning" solver. The code is a randomized depth-first explorer with
route replay, and the measurements above replace that description.

## Next

The next step is learned agents on this simulator and benchmark:

- tabular Q-learning and Dyna-Q, with learning curves compared against the two-run robots;
- a cross-entropy-trained junction policy, evaluated on maze seeds it never saw in training.

## License

[MIT](LICENSE)
