/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source.openalex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.drcorydugan.citationnetwork.source.RecordingFetcher;
import com.drcorydugan.citationnetwork.source.SourceException;
import com.drcorydugan.citationnetwork.source.Work;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import org.junit.Test;

/**
 * Reads the recorded OpenAlex payloads and checks the mapping against what is
 * actually in them. The fixtures were recorded from the live interface on
 * 2026-09-21 and trimmed of fields this plugin does not read.
 */
public class OpenAlexMapperTest {

    private JsonObject fixture(String name) throws IOException, SourceException {
        return OpenAlexMapper.parse(RecordingFetcher.read(name));
    }

    @Test
    public void singleWorkMapsEveryFieldTheGraphNeeds() throws Exception {
        Work work = OpenAlexMapper.toWork(fixture("work_single.json"));

        assertEquals("W3198910543", work.getId());
        assertEquals("10.1016/s2352-3026(21)00193-9", work.getDoi());
        assertEquals(Integer.valueOf(2021), work.getYear());
        assertEquals("The Lancet Haematology", work.getVenue());
        assertEquals(4, work.getAuthors().size());
        assertEquals(84, work.getReferencedWorks().size());
        assertFalse("this work is not retracted in the recorded payload", work.isRetracted());
        assertEquals("article", work.getType());
        assertTrue("the recorded authorships carry institutions",
                work.getInstitutions().contains("University of Malawi"));
        assertEquals("institutions are distinct", work.getInstitutions().size(),
                work.getInstitutions().stream().distinct().count());
        assertNotNull(work.getTitle());
        assertTrue("every reference is a short identifier",
                work.getReferencedWorks().stream().allMatch(id -> id.startsWith("W")));
    }

    @Test
    public void resultsPageMapsAndCarriesACursor() throws Exception {
        JsonObject page = fixture("cited_by_page1.json");
        List<Work> works = OpenAlexMapper.toWorks(page);

        assertEquals(5, works.size());
        assertNotNull("the recorded page is not the last one", OpenAlexMapper.nextCursor(page));
        assertTrue(works.stream().allMatch(w -> w.getId().startsWith("W")));
    }

    @Test
    public void anErrorPayloadBecomesAReadableFailure() {
        String body = "{\"error\":\"Rate limit exceeded\",\"message\":\"retry later\",\"retryAfter\":39}";
        try {
            OpenAlexMapper.parse(body);
            org.junit.Assert.fail("an error payload must not parse as a work");
        } catch (SourceException e) {
            assertTrue(e.getMessage().contains("Rate limit exceeded"));
        }
    }

    @Test
    public void identifiersAndDigitalObjectIdentifiersAreNormalised() {
        assertEquals("W123", OpenAlexMapper.shortId("https://openalex.org/W123"));
        assertEquals("W123", OpenAlexMapper.shortId("W123"));
        assertEquals("10.1/abc", OpenAlexMapper.bareDoi("https://doi.org/10.1/abc"));
        assertEquals("10.1/abc", OpenAlexMapper.bareDoi("10.1/abc"));
    }
}
