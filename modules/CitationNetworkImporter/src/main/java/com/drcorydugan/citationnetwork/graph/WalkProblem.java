/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.graph;

/**
 * Something that went wrong without stopping the walk.
 *
 * <p>It carries the parts and not a sentence, so whoever shows it to a user
 * writes that sentence in their own language.</p>
 */
public final class WalkProblem {

    /** Which side of a work could not be fetched. */
    public enum Kind {
        CITING,
        REFERENCES
    }

    private final Kind kind;
    private final String workId;
    private final String detail;

    public WalkProblem(Kind kind, String workId, String detail) {
        this.kind = kind;
        this.workId = workId;
        this.detail = detail;
    }

    public Kind getKind() {
        return kind;
    }

    public String getWorkId() {
        return workId;
    }

    /** The source's own message, for a log rather than for a user. */
    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        return kind + " " + workId + ": " + detail;
    }
}
