/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class CachingFetcherTest {

    @Test
    public void secondCallIsServedFromDiskAndNeverReachesTheDelegate() throws IOException {
        Path directory = Files.createTempDirectory("citation-network-cache");
        AtomicInteger calls = new AtomicInteger();
        HttpFetcher delegate = url -> {
            calls.incrementAndGet();
            return "{\"ok\":true}";
        };
        CachingFetcher fetcher = new CachingFetcher(delegate, directory);

        assertFalse(fetcher.isCached("https://example.org/a"));
        assertEquals("{\"ok\":true}", fetcher.get("https://example.org/a"));
        assertTrue(fetcher.isCached("https://example.org/a"));
        assertEquals("{\"ok\":true}", fetcher.get("https://example.org/a"));

        assertEquals("the delegate is called once and the cache answers after that",
                1, calls.get());
    }

    @Test
    public void differentUniformResourceLocatorsGetDifferentEntries() throws IOException {
        Path directory = Files.createTempDirectory("citation-network-cache");
        CachingFetcher fetcher = new CachingFetcher(url -> "{\"url\":\"" + url + "\"}", directory);

        fetcher.get("https://example.org/a");
        fetcher.get("https://example.org/b");

        assertEquals(2, Files.list(directory).count());
    }
}
