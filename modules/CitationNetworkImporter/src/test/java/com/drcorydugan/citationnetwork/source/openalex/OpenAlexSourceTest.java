/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source.openalex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.drcorydugan.citationnetwork.source.RecordingFetcher;
import com.drcorydugan.citationnetwork.source.SourceException;
import com.drcorydugan.citationnetwork.source.Work;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class OpenAlexSourceTest {

    private static OpenAlexSource source(RecordingFetcher fetcher) {
        return new OpenAlexSource(fetcher, "https://api.openalex.org", "someone@example.org", null);
    }

    private static RecordingFetcher routed() {
        return new RecordingFetcher(url -> {
            if (url.contains("/works/W3198910543")) {
                return "work_single.json";
            }
            if (url.contains("filter=cites")) {
                return "cited_by_page1.json";
            }
            if (url.contains("openalex_id")) {
                // A batched identifier lookup returns everything asked for on one
                // page, so this fixture carries a null cursor.
                return "batch_page.json";
            }
            return null;
        });
    }

    @Test
    public void oneWorkIsFetchedAndMapped() throws Exception {
        Work work = source(routed()).fetchWork("W3198910543");
        assertEquals("W3198910543", work.getId());
        assertEquals(Integer.valueOf(2021), work.getYear());
    }

    @Test
    public void aLimitIsHonouredEvenWhenThePageIsLarger() throws Exception {
        List<Work> citing = source(routed()).fetchCiting("W3198910543", 3);
        assertEquals("the recorded page holds five works and the limit is three",
                3, citing.size());
    }

    @Test
    public void identifiersAreBatchedRatherThanFetchedOneAtATime() throws Exception {
        RecordingFetcher fetcher = routed();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < OpenAlexSource.ID_BATCH + 10; i++) {
            ids.add("W" + (1000 + i));
        }

        source(fetcher).fetchWorks(ids);

        long batched = fetcher.getRequested().stream()
                .filter(url -> url.contains("openalex_id")).count();
        assertEquals("sixty identifiers become two requests, not sixty", 2, batched);
    }

    @Test
    public void theRequestCarriesThePolitePoolAddressAndEncodesTheQuery() {
        RecordingFetcher fetcher = routed();
        OpenAlexSource openAlex = new OpenAlexSource(fetcher, "https://api.openalex.org",
                "someone@example.org", "a-key");
        String url = openAlex.url("/works", new java.util.LinkedHashMap<>(
                java.util.Map.of("search", "iron deficiency")));

        assertTrue(url.contains("mailto=someone%40example.org"));
        assertTrue(url.contains("api_key=a-key"));
        assertTrue("a space is encoded rather than left raw", url.contains("iron%20deficiency"));
    }

    @Test
    public void everyIdentifierFormTheUserMightPasteIsAccepted() {
        assertEquals("W123", OpenAlexSource.normaliseIdentifier("https://openalex.org/W123"));
        assertEquals("W123", OpenAlexSource.normaliseIdentifier(" W123 "));
        assertEquals("doi:10.1/abc", OpenAlexSource.normaliseIdentifier("10.1/abc"));
        assertEquals("doi:10.1/abc", OpenAlexSource.normaliseIdentifier("https://doi.org/10.1/abc"));
        assertEquals("doi:10.1/abc", OpenAlexSource.normaliseIdentifier("doi:10.1/abc"));
    }

    @Test
    public void anEmptyResultStopsRatherThanLooping() throws Exception {
        RecordingFetcher empty = new RecordingFetcher(url -> "empty_page.json");
        List<Work> works = source(empty).fetchCiting("W1", 50);
        assertTrue(works.isEmpty());
        assertEquals("one request is enough to learn there is nothing",
                1, empty.getRequested().size());
    }

    @Test
    public void aSourceErrorIsReportedRatherThanSwallowed() {
        RecordingFetcher broken = new RecordingFetcher(url -> null);
        try {
            source(broken).fetchWork("W1");
            org.junit.Assert.fail("a failed fetch must raise");
        } catch (SourceException e) {
            assertTrue(e.getMessage().contains("OpenAlex could not be reached"));
        }
    }

    @Test
    public void worksAreDeduplicatedByIdentifier() throws Exception {
        List<Work> citing = source(routed()).fetchCiting("W3198910543", 5);
        List<String> ids = citing.stream().map(Work::getId).collect(Collectors.toList());
        assertEquals("the recorded page carries no repeats",
                ids.size(), ids.stream().distinct().count());
    }
}
