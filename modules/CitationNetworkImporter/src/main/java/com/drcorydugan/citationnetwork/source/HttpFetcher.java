/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source;

import java.io.IOException;
import java.util.Map;

/**
 * Fetches the body of a uniform resource locator as text.
 *
 * <p>This exists so the source layer can be tested against recorded responses
 * with no network, and so caching can be added as a decorator rather than
 * written into every source.</p>
 */
public interface HttpFetcher {

    /**
     * @param url     an absolute uniform resource locator
     * @param headers request headers, which some sources use to carry a key
     * @return the response body as text
     * @throws IOException if the request fails after any retries the
     *                     implementation makes
     */
    String get(String url, Map<String, String> headers) throws IOException;

    /**
     * The same request with no headers of its own.
     */
    default String get(String url) throws IOException {
        return get(url, Map.of());
    }
}
