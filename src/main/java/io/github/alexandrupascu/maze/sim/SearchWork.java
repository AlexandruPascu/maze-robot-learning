package io.github.alexandrupascu.maze.sim;

/** A robot that plans, reporting how much searching it has done since its maze began. */
public interface SearchWork {

  /** Nodes taken off a search frontier: tiles or cells expanded, summed over every search. */
  long expansions();
}
