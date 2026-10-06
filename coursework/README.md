# Coursework originals (2022)

These five files are my robot-maze coursework for CS118 at the University of Warwick, exactly as
uploaded in 2022, CRLF line endings included. They compile only against the department's maze
framework, which belongs to the university and is not part of this repository.

| File | What it is |
| --- | --- |
| `Ex1.java` | Random depth-first explorer that records junctions in a fixed array of 10,000 entries |
| `Ex2.java` | The same explorer with an `ArrayList` route stack, which also copes with loops |
| `Ex3.java` | The same code as `Ex2.java` under another class name and header comment |
| `Explorer.java` | Route learning: records the route on run 1 and replays it on run 2 |
| `GrandFinale.java` | The same code as `Explorer.java` under another class name |

`GrandFinale` runs in the simulator as the "Coursework explorer (2022)" through a mechanical port,
[`src/main/java/io/github/alexandrupascu/maze/coursework/GrandFinale.java`](../src/main/java/io/github/alexandrupascu/maze/coursework/GrandFinale.java).
The port adds a package line, takes `IRobot` from a re-declaration of the interface instead of the
university's framework, and replaces `Math.random()` with an injected `java.util.Random` so that
runs can be replayed. `CourseworkPortTest` removes comments and whitespace from both files and fails
if the code differs in any other way.

## The 2022 description, corrected

The original README said:

> Redefined a mix between Dijkstra's, Trémaux's and A\* algorithms using Java in order to build an
> efficient Iterative Machine Learning maze solver robot, accomplishing an average of 4x steps
> decrease on the 2nd run

The code does not use Dijkstra's algorithm, A\* or machine learning. It explores at random,
depth first, using the framework's been-before marks in the spirit of Trémaux's algorithm, keeps a
stack of the cells on its current route, and replays that stack on the second run. The "4x" holds
for small perfect Prim mazes (a median of 3.7× fewer steps at 7×7 cells) but depends heavily on the maze;
see the [benchmark results](../reports/summary.md).
