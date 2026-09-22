/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.drcorydugan.citationnetwork.graph.CitationGraph;
import com.drcorydugan.citationnetwork.graph.CitationWalker;
import com.drcorydugan.citationnetwork.graph.WalkSettings;
import com.drcorydugan.citationnetwork.source.RecordingFetcher;
import com.drcorydugan.citationnetwork.source.Work;
import com.drcorydugan.citationnetwork.source.openalex.OpenAlexSource;
import org.junit.Test;

/**
 * The whole path, from recorded payloads through the source to a graph.
 *
 * <p>The neighbourhood is a real one: Beutler and Waalen's paper on the lower
 * limit of normal for blood haemoglobin, and ten of the works citing it, both
 * recorded from OpenAlex on 2026-09-21.</p>
 */
public class ThresholdNeighbourhoodTest {

    private static final String SEED = "W2072443528";

    private OpenAlexSource source() {
        return new OpenAlexSource(new RecordingFetcher(url -> {
            if (url.contains("/works/" + SEED)) {
                return "threshold_seed.json";
            }
            if (url.contains("filter=cites")) {
                return "threshold_citing.json";
            }
            return null;
        }), "someone@example.org", null);
    }

    @Test
    public void theSeedIsReadWithItsRealFields() throws Exception {
        Work seed = source().fetchWork(SEED);

        assertEquals("10.1182/blood-2005-07-3046", seed.getDoi());
        assertEquals(Integer.valueOf(2005), seed.getYear());
        assertEquals("Blood", seed.getVenue());
        assertEquals(20, seed.getReferencedWorks().size());
        assertTrue(seed.getTitle().startsWith("The definition of anemia"));
    }

    @Test
    public void oneGenerationBuildsTheSeedAndItsCiters() throws Exception {
        WalkSettings settings = new WalkSettings();
        settings.setSeedIdentifier(SEED);
        settings.setDepth(1);
        settings.setDirection(WalkSettings.Direction.CITING);
        settings.setNeighboursPerWork(10);
        settings.setMaximumWorks(100);

        CitationGraph graph = new CitationWalker(source(), settings, null).walk();

        assertEquals("the seed plus the ten recorded citers", 11, graph.nodeCount());
        assertEquals(0, graph.getGeneration(SEED));

        long fromSeed = graph.getCitations().stream()
                .filter(c -> c.getCitedId().equals(SEED)).count();
        assertEquals("one edge from the seed to each citer", 10, fromSeed);

        long betweenCiters = graph.getCitations().size() - fromSeed;
        assertEquals("the citers also cite each other, so the neighbourhood is not a star",
                7, betweenCiters);
        assertTrue("every edge joins two works that are in the graph",
                graph.getCitations().stream().allMatch(c ->
                        graph.hasWork(c.getCitedId()) && graph.hasWork(c.getCitingId())));
    }
}
