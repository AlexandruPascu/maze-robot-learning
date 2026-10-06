package io.github.alexandrupascu.maze.search;

import io.github.alexandrupascu.maze.Heading;
import io.github.alexandrupascu.maze.Position;
import java.util.Arrays;

/**
 * D* Lite (Koenig and Likhachev, 2002): shortest routes to a fixed goal on a four-way grid whose
 * tiles become blocked as a moving robot discovers walls. It searches backwards from the goal, and
 * after each discovery it repairs only the distances the new walls affect instead of searching
 * again. Every tile starts passable, which is planning under the freespace assumption.
 */
public final class DStarLite {
  private static final int INFINITE = Integer.MAX_VALUE / 4;
  private static final Heading[] HEADINGS = Heading.values();

  private final int width;
  private final int height;
  private final int goal;
  private final boolean[] blocked;
  private final int[] g;
  private final int[] rhs;
  private final Queue queue;
  private int start;
  private int keyModifier;
  private boolean changed;
  private long expansions;

  public DStarLite(int width, int height, Position start, Position goal) {
    this.width = width;
    this.height = height;
    int tiles = width * height;
    this.goal = index(goal);
    this.start = index(start);
    blocked = new boolean[tiles];
    g = new int[tiles];
    rhs = new int[tiles];
    Arrays.fill(g, INFINITE);
    Arrays.fill(rhs, INFINITE);
    queue = new Queue(tiles);
    rhs[this.goal] = 0;
    queue.put(this.goal, key(this.goal));
    changed = true;
  }

  /**
   * The robot now stands on {@code tile}, which may be anywhere. Queued priorities were computed from
   * the old position, so the key modifier grows by the distance moved to keep them lower bounds.
   * Moving along the route returned by {@link #next()} triggers no further search.
   */
  public void moveTo(Position tile) {
    int moved = index(tile);
    if (moved != start) {
      keyModifier += heuristic(start, moved);
      start = moved;
      changed = true;
    }
  }

  /** A wall discovered on {@code tile}; the affected routes are repaired at the next {@link #next()}. */
  public void block(Position tile) {
    int wall = index(tile);
    if (wall == goal) {
      throw new IllegalArgumentException("the goal cannot be a wall");
    }
    if (blocked[wall]) {
      return;
    }
    changed = true;
    blocked[wall] = true;
    rhs[wall] = INFINITE;
    update(wall);
    for (Heading heading : HEADINGS) {
      int next = neighbour(wall, heading);
      if (next >= 0 && next != goal) {
        rhs[next] = bestSuccessor(next);
        update(next);
      }
    }
  }

  /** The neighbouring tile that starts a shortest route to the goal, preferring N, E, S, W on ties. */
  public Position next() {
    if (distance() >= INFINITE) {
      throw new IllegalStateException("the goal cannot be reached from " + position(start));
    }
    int best = -1;
    for (Heading heading : HEADINGS) {
      int next = neighbour(start, heading);
      if (next >= 0 && !blocked[next] && (best < 0 || g[next] < g[best])) {
        best = next;
      }
    }
    return position(best);
  }

  /**
   * Length of a shortest route from the robot's tile to the goal, assuming unknown tiles are open.
   * After a repair the robot's own one-step look-ahead value is exact, even when its g-value is not.
   */
  public int distance() {
    repair();
    return start == goal ? 0 : rhs[start];
  }

  /** Tiles taken off the priority queue so far: the planner's search work. */
  public long expansions() {
    return expansions;
  }

  private void repair() {
    if (!changed) {
      return;
    }
    changed = false;
    while (!queue.isEmpty() && (queue.topKey() < key(start) || rhs[start] > g[start])) {
      int tile = queue.top();
      long oldKey = queue.topKey();
      long newKey = key(tile);
      expansions++;
      if (oldKey < newKey) {
        queue.put(tile, newKey);
      } else if (g[tile] > rhs[tile]) {
        g[tile] = rhs[tile];
        queue.remove(tile);
        for (Heading heading : HEADINGS) {
          int previous = neighbour(tile, heading);
          if (previous >= 0) {
            if (previous != goal) {
              rhs[previous] = Math.min(rhs[previous], add(cost(previous, tile), g[tile]));
            }
            update(previous);
          }
        }
      } else {
        int oldG = g[tile];
        g[tile] = INFINITE;
        update(tile);
        for (Heading heading : HEADINGS) {
          int previous = neighbour(tile, heading);
          if (previous >= 0) {
            if (previous != goal && rhs[previous] == add(cost(previous, tile), oldG)) {
              rhs[previous] = bestSuccessor(previous);
            }
            update(previous);
          }
        }
      }
    }
  }

  private int bestSuccessor(int tile) {
    int best = INFINITE;
    for (Heading heading : HEADINGS) {
      int next = neighbour(tile, heading);
      if (next >= 0) {
        best = Math.min(best, add(cost(tile, next), g[next]));
      }
    }
    return best;
  }

  // Moving between two adjacent tiles costs one step unless either is a known wall.
  private int cost(int from, int to) {
    return blocked[from] || blocked[to] ? INFINITE : 1;
  }

  private void update(int tile) {
    if (g[tile] != rhs[tile]) {
      queue.put(tile, key(tile));
    } else {
      queue.remove(tile);
    }
  }

  // Priority [min(g, rhs) + h + km; min(g, rhs)] packed into one long that compares the same way.
  private long key(int tile) {
    int cost = Math.min(g[tile], rhs[tile]);
    int estimate = cost >= INFINITE ? INFINITE : cost + heuristic(start, tile) + keyModifier;
    return (long) estimate << 32 | cost;
  }

  private int heuristic(int a, int b) {
    return Math.abs(a % width - b % width) + Math.abs(a / width - b / width);
  }

  private static int add(int a, int b) {
    return a >= INFINITE || b >= INFINITE ? INFINITE : a + b;
  }

  private int neighbour(int tile, Heading heading) {
    int x = tile % width + heading.dx();
    int y = tile / width + heading.dy();
    return x < 0 || y < 0 || x >= width || y >= height ? -1 : y * width + x;
  }

  private int index(Position tile) {
    return tile.y() * width + tile.x();
  }

  private Position position(int tile) {
    return new Position(tile % width, tile / width);
  }

  /** A binary min-heap of tiles with decrease-key and removal; ties go to the lower tile index. */
  private static final class Queue {
    private final int[] tiles;
    private final long[] keys;
    private final int[] slot;
    private int size;

    Queue(int capacity) {
      tiles = new int[capacity];
      keys = new long[capacity];
      slot = new int[capacity];
      Arrays.fill(slot, -1);
    }

    boolean isEmpty() {
      return size == 0;
    }

    int top() {
      return tiles[0];
    }

    long topKey() {
      return keys[0];
    }

    void put(int tile, long key) {
      int i = slot[tile];
      if (i < 0) {
        i = size++;
        tiles[i] = tile;
        slot[tile] = i;
      }
      keys[i] = key;
      siftDown(siftUp(i));
    }

    void remove(int tile) {
      int i = slot[tile];
      if (i < 0) {
        return;
      }
      slot[tile] = -1;
      size--;
      if (i < size) {
        tiles[i] = tiles[size];
        keys[i] = keys[size];
        slot[tiles[i]] = i;
        siftDown(siftUp(i));
      }
    }

    private int siftUp(int i) {
      while (i > 0 && less(i, (i - 1) / 2)) {
        swap(i, (i - 1) / 2);
        i = (i - 1) / 2;
      }
      return i;
    }

    private void siftDown(int i) {
      while (true) {
        int smallest = i;
        for (int child = 2 * i + 1; child <= 2 * i + 2 && child < size; child++) {
          if (less(child, smallest)) {
            smallest = child;
          }
        }
        if (smallest == i) {
          return;
        }
        swap(i, smallest);
        i = smallest;
      }
    }

    private boolean less(int a, int b) {
      return keys[a] < keys[b] || keys[a] == keys[b] && tiles[a] < tiles[b];
    }

    private void swap(int a, int b) {
      int tile = tiles[a];
      long key = keys[a];
      tiles[a] = tiles[b];
      keys[a] = keys[b];
      tiles[b] = tile;
      keys[b] = key;
      slot[tiles[a]] = a;
      slot[tiles[b]] = b;
    }
  }
}
