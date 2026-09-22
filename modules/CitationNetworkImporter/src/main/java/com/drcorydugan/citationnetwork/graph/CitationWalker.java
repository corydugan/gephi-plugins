/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.graph;

import com.drcorydugan.citationnetwork.source.CitationSource;
import com.drcorydugan.citationnetwork.source.SourceException;
import com.drcorydugan.citationnetwork.source.Work;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Walks a {@link CitationSource} outwards from a seed and builds a
 * {@link CitationGraph}.
 *
 * <p>The walk is breadth first by generation, so a maximum that is reached
 * part way through leaves a graph whose nearest neighbourhood is complete
 * rather than a random slice of it. Nothing here touches Gephi, which is what
 * lets the walk be tested against recorded responses.</p>
 */
public final class CitationWalker {

    /**
     * Receives progress and can stop the walk. The importer implements this
     * over Gephi's own progress ticket and cancel button.
     */
    public interface Observer {

        /** Called after each step, with the running totals. */
        void progress(String message, int nodes, int edges);

        /** Checked before each request; true stops the walk where it stands. */
        boolean isCancelled();
    }

    /** An observer that reports nothing and never cancels. */
    public static final Observer SILENT = new Observer() {
        @Override
        public void progress(String message, int nodes, int edges) {
            // nothing to do
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    };

    private final CitationSource source;
    private final WalkSettings settings;
    private final Observer observer;
    private final List<String> problems = new ArrayList<>();

    public CitationWalker(CitationSource source, WalkSettings settings, Observer observer) {
        this.source = source;
        this.settings = settings;
        this.observer = observer == null ? SILENT : observer;
    }

    /**
     * Anything that went wrong without stopping the walk, for the import
     * report. A neighbourhood that could not be fetched is recorded here
     * rather than silently dropped.
     */
    public List<String> getProblems() {
        return problems;
    }

    public CitationGraph walk() throws SourceException {
        CitationGraph graph = new CitationGraph();
        List<Work> seeds = fetchSeeds();
        for (Work seed : seeds) {
            graph.addWork(seed, 0);
        }
        observer.progress("Seeds resolved", graph.nodeCount(), graph.edgeCount());

        Set<String> frontier = new LinkedHashSet<>();
        for (Work seed : seeds) {
            frontier.add(seed.getId());
        }

        for (int generation = 1; generation <= settings.getDepth(); generation++) {
            if (observer.isCancelled() || graph.nodeCount() >= settings.getMaximumWorks()) {
                break;
            }
            Set<String> next = new LinkedHashSet<>();
            for (String id : frontier) {
                if (observer.isCancelled() || graph.nodeCount() >= settings.getMaximumWorks()) {
                    break;
                }
                expand(graph, id, generation, next);
                observer.progress("Generation " + generation, graph.nodeCount(), graph.edgeCount());
            }
            frontier = next;
        }

        // A citation between two works already in the graph is worth keeping
        // even when neither was fetched as the other's neighbour.
        closeInternalCitations(graph);
        return graph;
    }

    private List<Work> fetchSeeds() throws SourceException {
        if (settings.hasSeedIdentifier()) {
            List<Work> seeds = new ArrayList<>();
            seeds.add(source.fetchWork(settings.getSeedIdentifier()));
            return seeds;
        }
        if (settings.getQuery().isEmpty()) {
            throw new SourceException("the walk needs either a work identifier or a search query");
        }
        return source.search(settings.getQuery(), settings.getSeedCount());
    }

    private void expand(CitationGraph graph, String id, int generation, Set<String> next) {
        WalkSettings.Direction direction = settings.getDirection();
        if (direction == WalkSettings.Direction.CITING || direction == WalkSettings.Direction.BOTH) {
            for (Work citing : fetchSafely(id, true)) {
                if (graph.nodeCount() >= settings.getMaximumWorks() && !graph.hasWork(citing.getId())) {
                    break;
                }
                if (graph.addWork(citing, generation)) {
                    next.add(citing.getId());
                }
                graph.addCitation(id, citing.getId());
            }
        }
        if (direction == WalkSettings.Direction.REFERENCES || direction == WalkSettings.Direction.BOTH) {
            for (Work cited : fetchSafely(id, false)) {
                if (graph.nodeCount() >= settings.getMaximumWorks() && !graph.hasWork(cited.getId())) {
                    break;
                }
                if (graph.addWork(cited, generation)) {
                    next.add(cited.getId());
                }
                graph.addCitation(cited.getId(), id);
            }
        }
    }

    private List<Work> fetchSafely(String id, boolean citing) {
        try {
            return citing
                    ? source.fetchCiting(id, settings.getNeighboursPerWork())
                    : source.fetchReferences(id, settings.getNeighboursPerWork());
        } catch (SourceException e) {
            problems.add((citing ? "citing works" : "references") + " for " + id
                    + " could not be fetched: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Every work already carries the identifiers it cites, so any citation
     * between two works in the graph can be added without another request.
     */
    private void closeInternalCitations(CitationGraph graph) {
        for (Work work : new ArrayList<>(graph.getWorks())) {
            for (String referenced : work.getReferencedWorks()) {
                graph.addCitation(referenced, work.getId());
            }
        }
    }
}
