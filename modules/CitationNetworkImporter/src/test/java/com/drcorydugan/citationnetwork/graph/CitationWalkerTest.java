/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.drcorydugan.citationnetwork.source.SourceException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/**
 * The walk is checked against a network whose shape is known exactly.
 *
 * <pre>
 *   HUB is cited by A, B and C
 *   A is cited by D
 *   B is cited by D as well, which is what makes D reachable twice
 *   C cites HUB and A, so there is a citation INSIDE the neighbourhood
 * </pre>
 */
public class CitationWalkerTest {

    private FakeSource source;

    @Before
    public void buildNetwork() {
        source = new FakeSource();
        source.addWork("HUB", List.of());
        source.addWork("A", List.of("HUB"));
        source.addWork("B", List.of("HUB"));
        source.addWork("C", List.of("HUB", "A"));
        source.addWork("D", List.of("A", "B"));
    }

    private WalkSettings settings(int depth, WalkSettings.Direction direction) {
        WalkSettings walk = new WalkSettings();
        walk.setSeedIdentifier("HUB");
        walk.setDepth(depth);
        walk.setDirection(direction);
        walk.setNeighboursPerWork(50);
        walk.setMaximumWorks(100);
        return walk;
    }

    @Test
    public void depthZeroReturnsTheSeedAlone() throws Exception {
        CitationGraph graph = new CitationWalker(source,
                settings(0, WalkSettings.Direction.CITING), null).walk();

        assertEquals(1, graph.nodeCount());
        assertEquals(0, graph.edgeCount());
        assertEquals(0, graph.getGeneration("HUB"));
    }

    @Test
    public void oneGenerationCollectsTheWorksThatCiteTheSeed() throws Exception {
        CitationGraph graph = new CitationWalker(source,
                settings(1, WalkSettings.Direction.CITING), null).walk();

        assertEquals("the hub and its three citers", 4, graph.nodeCount());
        assertTrue(graph.hasWork("A"));
        assertTrue(graph.hasWork("C"));
        assertFalse("D is two generations away", graph.hasWork("D"));
        assertEquals(1, graph.getGeneration("A"));
    }

    @Test
    public void aWorkReachableTwiceIsKeptOnceAtItsFirstGeneration() throws Exception {
        CitationGraph graph = new CitationWalker(source,
                settings(2, WalkSettings.Direction.CITING), null).walk();

        assertEquals("hub, A, B, C, D and nothing repeated", 5, graph.nodeCount());
        assertEquals("D is reached from both A and B, and stays at generation two",
                2, graph.getGeneration("D"));
        long distinct = graph.getWorks().stream().map(w -> w.getId()).distinct().count();
        assertEquals(distinct, graph.nodeCount());
    }

    @Test
    public void citationsInsideTheNeighbourhoodAreClosedWithoutAnotherRequest() throws Exception {
        CitationGraph graph = new CitationWalker(source,
                settings(1, WalkSettings.Direction.CITING), null).walk();

        assertTrue("C cites HUB", graph.getCitations().stream()
                .anyMatch(c -> c.getCitedId().equals("HUB") && c.getCitingId().equals("C")));
        assertTrue("C also cites A, and both are in the graph, so the edge is kept",
                graph.getCitations().stream()
                        .anyMatch(c -> c.getCitedId().equals("A") && c.getCitingId().equals("C")));
    }

    @Test
    public void everyEdgeRunsFromTheCitedWorkToTheCitingOne() throws Exception {
        CitationGraph graph = new CitationWalker(source,
                settings(1, WalkSettings.Direction.CITING), null).walk();

        assertTrue(graph.getCitations().stream()
                .allMatch(c -> graph.hasWork(c.getCitedId()) && graph.hasWork(c.getCitingId())));
        assertFalse("no citation points at the hub from outside the graph",
                graph.getCitations().stream().anyMatch(c -> c.getCitingId().equals("HUB")));
    }

    @Test
    public void theMaximumIsHonoured() throws Exception {
        WalkSettings walk = settings(2, WalkSettings.Direction.CITING);
        walk.setMaximumWorks(3);

        CitationGraph graph = new CitationWalker(source, walk, null).walk();

        assertTrue("the cap holds", graph.nodeCount() <= 3);
    }

    @Test
    public void cancellationStopsTheWalkWhereItStands() throws Exception {
        CitationWalker.Observer cancelling = new CitationWalker.Observer() {
            @Override
            public void progress(String message, int nodes, int edges) {
                // nothing to do
            }

            @Override
            public boolean isCancelled() {
                return true;
            }
        };

        CitationGraph graph = new CitationWalker(source,
                settings(2, WalkSettings.Direction.CITING), cancelling).walk();

        assertEquals("the seed is resolved and then the walk stops", 1, graph.nodeCount());
    }

    @Test
    public void aRefusedNeighbourhoodIsRecordedAndTheWalkCarriesOn() throws Exception {
        source.failOn("HUB");
        CitationWalker walker = new CitationWalker(source,
                settings(1, WalkSettings.Direction.CITING), null);

        CitationGraph graph = walker.walk();

        assertEquals("the seed is still there", 1, graph.nodeCount());
        assertEquals(1, walker.getProblems().size());
        assertTrue(walker.getProblems().get(0).contains("the source refused HUB"));
    }

    @Test
    public void followingReferencesRunsBackwardsInsteadOfForwards() throws Exception {
        WalkSettings walk = settings(1, WalkSettings.Direction.REFERENCES);
        walk.setSeedIdentifier("C");

        CitationGraph graph = new CitationWalker(source, walk, null).walk();

        assertTrue(graph.hasWork("HUB"));
        assertTrue(graph.hasWork("A"));
        assertFalse("nothing that cites C is fetched in this direction", graph.hasWork("D"));
    }

    @Test
    public void aWalkWithNeitherQueryNorIdentifierSaysSo() {
        WalkSettings walk = new WalkSettings();
        try {
            new CitationWalker(source, walk, null).walk();
            org.junit.Assert.fail("a walk with no starting point must raise");
        } catch (SourceException e) {
            assertTrue(e.getMessage().contains("work identifier or a search query"));
        }
    }

    @Test
    public void aQueryStartsTheWalkFromTheSearchResults() throws Exception {
        WalkSettings walk = settings(0, WalkSettings.Direction.CITING);
        walk.setSeedIdentifier("");
        walk.setQuery("iron");
        walk.setSeedCount(2);

        CitationGraph graph = new CitationWalker(source, walk, null).walk();

        assertEquals(2, graph.nodeCount());
    }
}
