/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.graph;

import com.drcorydugan.citationnetwork.source.Work;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The result of a citation walk: the works found, and the directed citations
 * between them.
 *
 * <p>An edge runs from the CITED work to the CITING work, which is the
 * direction a claim travels. A citation whose other end was never fetched is
 * not kept, so every edge joins two nodes that are both in the graph.</p>
 */
public final class CitationGraph {

    private final Map<String, Work> nodes = new LinkedHashMap<>();
    private final Set<Citation> edges = new LinkedHashSet<>();
    private final Map<String, Integer> generation = new LinkedHashMap<>();

    /**
     * Add a work, or keep the one already held. The first one wins, so a seed
     * keeps its generation of zero.
     *
     * @return true when the work was new
     */
    public boolean addWork(Work work, int atGeneration) {
        if (nodes.containsKey(work.getId())) {
            return false;
        }
        nodes.put(work.getId(), work);
        generation.put(work.getId(), atGeneration);
        return true;
    }

    /**
     * Add a citation. It is ignored unless both ends are already nodes, and a
     * repeat of the same pair is ignored.
     *
     * @return true when the edge was new and both ends were present
     */
    public boolean addCitation(String citedId, String citingId) {
        if (citedId == null || citingId == null || citedId.equals(citingId)) {
            return false;
        }
        if (!nodes.containsKey(citedId) || !nodes.containsKey(citingId)) {
            return false;
        }
        return edges.add(new Citation(citedId, citingId));
    }

    public boolean hasWork(String id) {
        return nodes.containsKey(id);
    }

    public Collection<Work> getWorks() {
        return nodes.values();
    }

    public Set<Citation> getCitations() {
        return edges;
    }

    public int getGeneration(String id) {
        return generation.getOrDefault(id, -1);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    /**
     * One directed citation, from the cited work to the citing work.
     */
    public static final class Citation {

        private final String citedId;
        private final String citingId;

        Citation(String citedId, String citingId) {
            this.citedId = citedId;
            this.citingId = citingId;
        }

        public String getCitedId() {
            return citedId;
        }

        public String getCitingId() {
            return citingId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Citation)) {
                return false;
            }
            Citation that = (Citation) other;
            return citedId.equals(that.citedId) && citingId.equals(that.citingId);
        }

        @Override
        public int hashCode() {
            return 31 * citedId.hashCode() + citingId.hashCode();
        }

        @Override
        public String toString() {
            return citedId + " -> " + citingId;
        }
    }
}
