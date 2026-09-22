/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * The clock and the wait are both supplied, so the spacing is asserted
 * without the test spending it.
 */
public class ThrottlingFetcherTest {

    private static final class Fake {
        long now = 1_000L;
        final List<Long> slept = new ArrayList<>();
    }

    @Test
    public void theFirstRequestIsNotDelayed() throws Exception {
        Fake fake = new Fake();
        HttpFetcher fetcher = new ThrottlingFetcher((url, headers) -> "{}",
                Duration.ofMillis(1000), () -> fake.now, millis -> fake.slept.add(millis));

        fetcher.get("https://example.org/a");

        assertTrue("nothing to wait for on the first request", fake.slept.isEmpty());
    }

    @Test
    public void asecondRequestWaitsOutTheRemainderOfTheInterval() throws Exception {
        Fake fake = new Fake();
        HttpFetcher fetcher = new ThrottlingFetcher((url, headers) -> "{}",
                Duration.ofMillis(1000), () -> fake.now, millis -> fake.slept.add(millis));

        fetcher.get("https://example.org/a");
        fake.now += 300;
        fetcher.get("https://example.org/b");

        assertEquals(1, fake.slept.size());
        assertEquals("three hundred of the thousand had passed",
                Long.valueOf(700), fake.slept.get(0));
    }

    @Test
    public void aRequestAfterTheIntervalHasPassedWaitsForNothing() throws Exception {
        Fake fake = new Fake();
        HttpFetcher fetcher = new ThrottlingFetcher((url, headers) -> "{}",
                Duration.ofMillis(1000), () -> fake.now, millis -> fake.slept.add(millis));

        fetcher.get("https://example.org/a");
        fake.now += 5000;
        fetcher.get("https://example.org/b");

        assertTrue(fake.slept.isEmpty());
    }

    @Test
    public void everyRequestStillReachesTheDelegate() throws Exception {
        Fake fake = new Fake();
        List<String> seen = new ArrayList<>();
        HttpFetcher fetcher = new ThrottlingFetcher((url, headers) -> {
            seen.add(url);
            return "{}";
        }, Duration.ofMillis(10), () -> fake.now, millis -> fake.slept.add(millis));

        fetcher.get("https://example.org/a", Map.of());
        fake.now += 10;
        fetcher.get("https://example.org/b", Map.of());

        assertEquals(List.of("https://example.org/a", "https://example.org/b"), seen);
    }
}
