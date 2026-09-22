/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.drcorydugan.citationnetwork.graph.CitationGraph;
import com.drcorydugan.citationnetwork.source.Work;
import java.util.List;
import org.junit.Test;

/**
 * The translation from the walk's graph into Gephi's container, which is the
 * one place a wrong column or a reversed edge would only show up at runtime.
 */
public class ContainerWriteTest {

    private static Work full(String id) {
        return new Work(id, "10.1/" + id, "Title of " + id, 2005, "Blood", 42,
                true, false, List.of("An author"), List.of("An institution"),
                List.of("Anaemia"), "article", List.of());
    }

    /** Shaped like the 1968 report: a hub with almost nothing on it. */
    private static Work sparse(String id) {
        return new Work(id, null, null, null, null, 949,
                false, false, List.of(), List.of(), List.of(), null, List.of());
    }

    private static CitationGraph graph() {
        CitationGraph graph = new CitationGraph();
        graph.addWork(sparse("HUB"), 0);
        graph.addWork(full("A"), 1);
        graph.addCitation("HUB", "A");
        return graph;
    }

    @Test
    public void everyColumnTheWriteUsesIsDeclaredFirst() {
        FakeContainer container = new FakeContainer();
        CitationNetworkImporter.addColumns(container.loader());
        new CitationNetworkImporter().write(graph(), container.loader(), "openalex");

        for (String column : container.nodeValues.get("A").keySet()) {
            assertTrue("the column " + column + " is declared before it is written",
                    container.nodeColumns.contains(column));
        }
        assertEquals(12, container.nodeColumns.size());
    }

    @Test
    public void theFullWorkCarriesEveryValue() {
        FakeContainer container = new FakeContainer();
        new CitationNetworkImporter().write(graph(), container.loader(), "openalex");

        var values = container.nodeValues.get("A");
        assertEquals("10.1/A", values.get(CitationNetworkImporter.COLUMN_DOI));
        assertEquals(2005, values.get(CitationNetworkImporter.COLUMN_YEAR));
        assertEquals("Blood", values.get(CitationNetworkImporter.COLUMN_VENUE));
        assertEquals(42, values.get(CitationNetworkImporter.COLUMN_CITED_BY));
        assertEquals(true, values.get(CitationNetworkImporter.COLUMN_OPEN_ACCESS));
        assertEquals("An author", values.get(CitationNetworkImporter.COLUMN_AUTHORS));
        assertEquals("An institution", values.get(CitationNetworkImporter.COLUMN_INSTITUTIONS));
        assertEquals("Anaemia", values.get(CitationNetworkImporter.COLUMN_CONCEPTS));
        assertEquals("article", values.get(CitationNetworkImporter.COLUMN_TYPE));
        assertEquals("openalex", values.get(CitationNetworkImporter.COLUMN_SOURCE));
        assertEquals(1, values.get(CitationNetworkImporter.COLUMN_GENERATION));
    }

    @Test
    public void aWorkWithNothingOnItLeavesThoseColumnsUnsetRatherThanNull() {
        FakeContainer container = new FakeContainer();
        new CitationNetworkImporter().write(graph(), container.loader(), "openalex");

        var values = container.nodeValues.get("HUB");
        assertFalse("no digital object identifier means no value written",
                values.containsKey(CitationNetworkImporter.COLUMN_DOI));
        assertFalse(values.containsKey(CitationNetworkImporter.COLUMN_VENUE));
        assertFalse(values.containsKey(CitationNetworkImporter.COLUMN_YEAR));
        assertFalse(values.containsKey(CitationNetworkImporter.COLUMN_AUTHORS));
        assertFalse(values.containsKey(CitationNetworkImporter.COLUMN_TYPE));
        assertEquals("what it does hold is still written",
                949, values.get(CitationNetworkImporter.COLUMN_CITED_BY));
        assertEquals("and no value is ever null",
                0, values.values().stream().filter(java.util.Objects::isNull).count());
    }

    @Test
    public void aWorkWithNoTitleIsLabelledWithItsIdentifier() {
        FakeContainer container = new FakeContainer();
        new CitationNetworkImporter().write(graph(), container.loader(), "openalex");

        assertEquals("HUB", container.nodeLabels.get("HUB"));
        assertEquals("Title of A", container.nodeLabels.get("A"));
    }

    @Test
    public void theEdgeRunsFromTheCitedWorkToTheCitingOne() {
        FakeContainer container = new FakeContainer();
        new CitationNetworkImporter().write(graph(), container.loader(), "openalex");

        assertEquals(1, container.edges.size());
        assertEquals("HUB", container.edges.get(0)[0]);
        assertEquals("A", container.edges.get(0)[1]);
    }
}
