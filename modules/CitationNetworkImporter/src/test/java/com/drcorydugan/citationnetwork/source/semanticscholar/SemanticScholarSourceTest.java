/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source.semanticscholar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.drcorydugan.citationnetwork.source.RecordingFetcher;
import com.drcorydugan.citationnetwork.source.Work;
import java.util.List;
import org.junit.Test;

/**
 * The second source, against payloads recorded from Semantic Scholar on
 * 2026-09-21.
 */
public class SemanticScholarSourceTest {

    private static RecordingFetcher fetcher(String referencesFixture) {
        return new RecordingFetcher(url -> {
            if (url.contains("/citations")) {
                return "s2_citations.json";
            }
            if (url.contains("/references")) {
                return referencesFixture;
            }
            if (url.contains("/paper/")) {
                return "s2_seed.json";
            }
            return null;
        });
    }

    private static SemanticScholarSource source(String referencesFixture) {
        return source(fetcher(referencesFixture), null);
    }

    private static SemanticScholarSource source(RecordingFetcher fetcher, String apiKey) {
        return new SemanticScholarSource(fetcher,
                "https://api.semanticscholar.org/graph/v1", apiKey);
    }

    @Test
    public void aPaperIsMappedFromItsOwnFieldNames() throws Exception {
        Work work = source("s2_references_available.json").fetchWork("10.1182/blood-2005-07-3046");

        assertEquals("306d216d64e132b27439e225505ff2dc17644723", work.getId());
        assertEquals("10.1182/blood-2005-07-3046", work.getDoi());
        assertEquals("Blood", work.getVenue());
        assertEquals(2, work.getAuthors().size());
        assertTrue(work.getCitedByCount() > 900);
    }

    @Test
    public void aCitationsPageReadsTheCitingPaperFromEachEntry() throws Exception {
        List<Work> citing = source("s2_references_available.json")
                .fetchCiting("306d216d64e132b27439e225505ff2dc17644723", 5);

        assertEquals(5, citing.size());
        assertTrue(citing.stream().allMatch(w -> w.getId() != null && !w.getId().isEmpty()));
    }

    @Test
    public void aReferencesPageReadsTheCitedPaperFromEachEntry() throws Exception {
        SemanticScholarSource source = source("s2_references_available.json");

        List<Work> cited = source.fetchReferences("eb55a813d810bb2d4a34d37a4dcd9e293f769f81", 5);

        assertEquals(5, cited.size());
        assertFalse("this paper's references are not withheld", source.getLastRequestWasElided());
    }

    @Test
    public void aWithheldReferenceListIsReportedRatherThanLookingEmpty() throws Exception {
        SemanticScholarSource source = source("s2_references_elided.json");

        List<Work> cited = source.fetchReferences("306d216d64e132b27439e225505ff2dc17644723", 5);

        assertTrue("the publisher withheld them, so nothing comes back", cited.isEmpty());
        assertTrue("and the source says that is why", source.getLastRequestWasElided());
    }

    @Test
    public void aKeyTravelsInTheHeaderWhereTheSourceReadsIt() throws Exception {
        RecordingFetcher fetcher = fetcher("s2_references_available.json");

        source(fetcher, "a-key").fetchWork("10.1182/blood-2005-07-3046");

        assertEquals("a-key", fetcher.getLastHeaders().get("x-api-key"));
        assertTrue("and never as a query parameter, where it is ignored",
                fetcher.getRequested().stream().noneMatch(url -> url.contains("x-api-key=")));
    }

    @Test
    public void noKeyMeansNoHeader() throws Exception {
        RecordingFetcher fetcher = fetcher("s2_references_available.json");

        source(fetcher, null).fetchWork("10.1182/blood-2005-07-3046");

        assertTrue(fetcher.getLastHeaders().isEmpty());
    }

    @Test
    public void identifiersAreNormalisedToTheFormTheSourceExpects() {
        assertEquals("DOI:10.1/abc", SemanticScholarSource.normaliseIdentifier("10.1/abc"));
        assertEquals("DOI:10.1/abc",
                SemanticScholarSource.normaliseIdentifier("https://doi.org/10.1/abc"));
        assertEquals("306d216d", SemanticScholarSource.normaliseIdentifier(" 306d216d "));
        assertEquals("PMID:16189263", SemanticScholarSource.normaliseIdentifier("PMID:16189263"));
    }
}
