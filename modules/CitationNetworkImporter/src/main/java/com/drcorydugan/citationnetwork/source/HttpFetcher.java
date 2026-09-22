/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source;

import java.io.IOException;

/**
 * Fetches the body of a uniform resource locator as text.
 *
 * <p>This exists so the source layer can be tested against recorded responses
 * with no network, and so caching can be added as a decorator rather than
 * written into every source.</p>
 */
public interface HttpFetcher {

    /**
     * @param url an absolute uniform resource locator
     * @return the response body as text
     * @throws IOException if the request fails after any retries the
     *                     implementation makes
     */
    String get(String url) throws IOException;
}
