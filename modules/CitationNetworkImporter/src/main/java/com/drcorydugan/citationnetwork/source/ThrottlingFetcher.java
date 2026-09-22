/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Keeps a minimum interval between requests that actually leave the machine.
 *
 * <p>An unauthenticated caller shares a pool with everyone else, so a walk
 * that issues requests as fast as it can is the behaviour that gets a source
 * to shut the door. A cached response costs nothing and is not delayed.</p>
 */
public final class ThrottlingFetcher implements HttpFetcher {

    private final HttpFetcher delegate;
    private final long intervalMillis;
    private final LongSupplier clock;
    private final Sleeper sleeper;
    private long lastRequestAt = Long.MIN_VALUE;

    /** Waits, so a test can assert the wait without spending it. */
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    public ThrottlingFetcher(HttpFetcher delegate, Duration interval) {
        this(delegate, interval, System::currentTimeMillis, Thread::sleep);
    }

    ThrottlingFetcher(HttpFetcher delegate, Duration interval, LongSupplier clock, Sleeper sleeper) {
        this.delegate = delegate;
        this.intervalMillis = Math.max(0, interval.toMillis());
        this.clock = clock;
        this.sleeper = sleeper;
    }

    @Override
    public String get(String url, Map<String, String> headers) throws IOException {
        long now = clock.getAsLong();
        long earliest = lastRequestAt == Long.MIN_VALUE ? now : lastRequestAt + intervalMillis;
        if (now < earliest) {
            try {
                sleeper.sleep(earliest - now);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("the wait between requests was interrupted", e);
            }
        }
        lastRequestAt = Math.max(clock.getAsLong(), earliest);
        return delegate.get(url, headers);
    }
}
