/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source;

/**
 * Thrown when a citation source cannot answer, whether because the network
 * failed, the source refused, or the payload did not parse.
 */
public class SourceException extends Exception {

    private static final long serialVersionUID = 1L;

    public SourceException(String message) {
        super(message);
    }

    public SourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
