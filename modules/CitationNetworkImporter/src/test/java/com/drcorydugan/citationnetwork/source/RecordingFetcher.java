/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A fetcher that answers from recorded fixtures and records every uniform
 * resource locator it was asked for. No test in this module touches the
 * network.
 */
public final class RecordingFetcher implements HttpFetcher {

    private final Function<String, String> router;
    private final List<String> requested = new ArrayList<>();
    private Map<String, String> lastHeaders = Map.of();

    public RecordingFetcher(Function<String, String> router) {
        this.router = router;
    }

    @Override
    public String get(String url, Map<String, String> headers) throws IOException {
        requested.add(url);
        this.lastHeaders = headers;
        String fixture = router.apply(url);
        if (fixture == null) {
            throw new IOException("no fixture is routed for " + url);
        }
        return read(fixture);
    }

    public List<String> getRequested() {
        return requested;
    }

    public Map<String, String> getLastHeaders() {
        return lastHeaders;
    }

    public static String read(String fixture) throws IOException {
        try (InputStream in = RecordingFetcher.class.getResourceAsStream("/fixtures/" + fixture)) {
            if (in == null) {
                throw new IOException("fixture not found on the test classpath: " + fixture);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
