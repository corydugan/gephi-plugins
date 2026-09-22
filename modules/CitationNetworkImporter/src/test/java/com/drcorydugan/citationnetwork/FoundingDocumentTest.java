/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.drcorydugan.citationnetwork.graph.CitationGraph;
import com.drcorydugan.citationnetwork.graph.CitationWalker;
import com.drcorydugan.citationnetwork.graph.WalkSettings;
import com.drcorydugan.citationnetwork.source.RecordingFetcher;
import com.drcorydugan.citationnetwork.source.Work;
import com.drcorydugan.citationnetwork.source.openalex.OpenAlexSource;
import org.junit.Test;

/**
 * A hub with almost no metadata of its own.
 *
 * <p>The 1968 World Health Organization scientific group report on nutritional
 * anaemias is cited by hundreds of works and carries no digital object
 * identifier, no authorships and no references in the source. A citation graph
 * shows it as a node with high in degree and no out degree, which is what a
 * definitional source looks like from inside the literature that inherited
 * it.</p>
 */
public class FoundingDocumentTest {

    private static final String SEED = "W4300771061";

    private OpenAlexSource source() {
        return new OpenAlexSource(new RecordingFetcher(url -> {
            if (url.contains("/works/" + SEED)) {
                return "who1968_seed.json";
            }
            if (url.contains("filter=cites")) {
                return "who1968_citing.json";
            }
            return null;
        }), "someone@example.org", null);
    }

    @Test
    public void theFoundingDocumentCarriesAlmostNoMetadata() throws Exception {
        Work seed = source().fetchWork(SEED);

        assertEquals(Integer.valueOf(1968), seed.getYear());
        assertEquals("article", seed.getType());
        assertNull("the source holds no digital object identifier for it", seed.getDoi());
        assertTrue("and no authorships", seed.getAuthors().isEmpty());
        assertTrue("and no references, so it cites nothing in the graph",
                seed.getReferencedWorks().isEmpty());
        assertTrue("while being cited hundreds of times", seed.getCitedByCount() > 900);
    }

    @Test
    public void theWalkShowsAPureSinkWithEveryEdgePointingIn() throws Exception {
        WalkSettings settings = new WalkSettings();
        settings.setSeedIdentifier(SEED);
        settings.setDepth(1);
        settings.setDirection(WalkSettings.Direction.CITING);
        settings.setNeighboursPerWork(10);
        settings.setMaximumWorks(100);

        CitationGraph graph = new CitationWalker(source(), settings, null).walk();

        assertEquals("the report and the ten recorded citers", 11, graph.nodeCount());
        assertTrue("nothing in the graph is cited BY the report",
                graph.getCitations().stream().noneMatch(c -> c.getCitingId().equals(SEED)));
        long intoTheSeed = graph.getCitations().stream()
                .filter(c -> c.getCitedId().equals(SEED)).count();
        assertEquals("every recorded citer points at it", 10, intoTheSeed);
    }
}
