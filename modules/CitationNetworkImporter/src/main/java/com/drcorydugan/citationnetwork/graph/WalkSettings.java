/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.graph;

/**
 * What to walk, how far, and how much of it to keep.
 */
public final class WalkSettings {

    /** Which way to follow citations out of a work. */
    public enum Direction {
        /** The works this one cites, so the graph runs backwards in time. */
        REFERENCES,
        /** The works that cite this one, so the graph runs forwards in time. */
        CITING,
        /** Both of the above. */
        BOTH
    }

    private String query = "";
    private String seedIdentifier = "";
    private int seedCount = 10;
    private int depth = 1;
    private int maximumWorks = 500;
    private int neighboursPerWork = 25;
    private Direction direction = Direction.CITING;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query.trim();
    }

    public String getSeedIdentifier() {
        return seedIdentifier;
    }

    public void setSeedIdentifier(String seedIdentifier) {
        this.seedIdentifier = seedIdentifier == null ? "" : seedIdentifier.trim();
    }

    public int getSeedCount() {
        return seedCount;
    }

    public void setSeedCount(int seedCount) {
        this.seedCount = Math.max(1, seedCount);
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = Math.max(0, depth);
    }

    public int getMaximumWorks() {
        return maximumWorks;
    }

    public void setMaximumWorks(int maximumWorks) {
        this.maximumWorks = Math.max(1, maximumWorks);
    }

    public int getNeighboursPerWork() {
        return neighboursPerWork;
    }

    public void setNeighboursPerWork(int neighboursPerWork) {
        this.neighboursPerWork = Math.max(1, neighboursPerWork);
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction == null ? Direction.CITING : direction;
    }

    /**
     * Whether the walk starts from one named work rather than from a search.
     */
    public boolean hasSeedIdentifier() {
        return !seedIdentifier.isEmpty();
    }
}
