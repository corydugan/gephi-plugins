/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.importer;

import static org.junit.Assert.assertEquals;

import com.drcorydugan.citationnetwork.graph.WalkSettings;
import org.junit.Test;

/**
 * The panel's own mapping, which is the part that can go wrong silently: a
 * dropdown index turning into the wrong direction or the wrong source.
 */
public class CitationNetworkPanelTest {

    @Test
    public void theWizardCannotFinishWithNeitherAQueryNorAnIdentifier() {
        assertEquals(false, CitationNetworkPanel.isComplete("", ""));
        assertEquals("whitespace is not an answer", false, CitationNetworkPanel.isComplete("  ", " "));
        assertEquals(false, CitationNetworkPanel.isComplete(null, null));
        assertEquals(true, CitationNetworkPanel.isComplete("iron deficiency", ""));
        assertEquals(true, CitationNetworkPanel.isComplete("", "W4300771061"));
        assertEquals(true, CitationNetworkPanel.isComplete("iron", "W4300771061"));
    }

    @Test
    public void everyDropdownIndexMapsToTheDirectionBesideIt() {
        assertEquals(WalkSettings.Direction.CITING, CitationNetworkPanel.directionOf(0));
        assertEquals(WalkSettings.Direction.REFERENCES, CitationNetworkPanel.directionOf(1));
        assertEquals(WalkSettings.Direction.BOTH, CitationNetworkPanel.directionOf(2));
    }

    @Test
    public void anUnsetDropdownFallsBackToCitingRatherThanFailing() {
        assertEquals(WalkSettings.Direction.CITING, CitationNetworkPanel.directionOf(-1));
        assertEquals(WalkSettings.Direction.CITING, CitationNetworkPanel.directionOf(99));
    }

    @Test
    public void theSourcePickerMapsToTheSourceIdentifiers() {
        assertEquals(ImportSettings.SOURCE_OPENALEX, CitationNetworkPanel.sourceIdOf(0));
        assertEquals(ImportSettings.SOURCE_SEMANTIC_SCHOLAR, CitationNetworkPanel.sourceIdOf(1));
        assertEquals("an unset picker means the first source",
                ImportSettings.SOURCE_OPENALEX, CitationNetworkPanel.sourceIdOf(-1));
    }

    @Test
    public void settingsRefuseAnEmptySourceIdentifier() {
        ImportSettings settings = new ImportSettings();
        settings.setSourceId("  ");
        assertEquals(ImportSettings.SOURCE_OPENALEX, settings.getSourceId());
        settings.setSourceId(ImportSettings.SOURCE_SEMANTIC_SCHOLAR);
        assertEquals(ImportSettings.SOURCE_SEMANTIC_SCHOLAR, settings.getSourceId());
    }

    @Test
    public void walkSettingsClampTheNumbersTheSpinnersFeedThem() {
        WalkSettings walk = new WalkSettings();
        walk.setDepth(-3);
        walk.setMaximumWorks(0);
        walk.setNeighboursPerWork(0);
        walk.setSeedCount(0);

        assertEquals(0, walk.getDepth());
        assertEquals(1, walk.getMaximumWorks());
        assertEquals(1, walk.getNeighboursPerWork());
        assertEquals(1, walk.getSeedCount());
    }
}
