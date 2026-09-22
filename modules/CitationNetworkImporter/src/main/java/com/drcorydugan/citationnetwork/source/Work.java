/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One scholarly work, reduced to the fields a citation graph needs.
 *
 * <p>Every field is taken straight from the source and nothing is derived or
 * inferred. A field the source does not carry is null for a single value or an
 * empty list for a collection, never a substituted default.</p>
 */
public final class Work {

    private final String id;
    private final String doi;
    private final String title;
    private final Integer year;
    private final String venue;
    private final int citedByCount;
    private final boolean openAccess;
    private final boolean retracted;
    private final List<String> authors;
    private final List<String> concepts;
    private final List<String> referencedWorks;

    public Work(String id,
                String doi,
                String title,
                Integer year,
                String venue,
                int citedByCount,
                boolean openAccess,
                boolean retracted,
                List<String> authors,
                List<String> concepts,
                List<String> referencedWorks) {
        this.id = Objects.requireNonNull(id, "a work needs an identifier");
        this.doi = doi;
        this.title = title;
        this.year = year;
        this.venue = venue;
        this.citedByCount = citedByCount;
        this.openAccess = openAccess;
        this.retracted = retracted;
        this.authors = copy(authors);
        this.concepts = copy(concepts);
        this.referencedWorks = copy(referencedWorks);
    }

    private static List<String> copy(List<String> in) {
        return in == null ? Collections.emptyList() : List.copyOf(in);
    }

    /**
     * The source's own identifier, normalised to its short form, for example
     * W3198910543 rather than the full uniform resource locator.
     */
    public String getId() {
        return id;
    }

    public String getDoi() {
        return doi;
    }

    public String getTitle() {
        return title;
    }

    public Integer getYear() {
        return year;
    }

    public String getVenue() {
        return venue;
    }

    public int getCitedByCount() {
        return citedByCount;
    }

    public boolean isOpenAccess() {
        return openAccess;
    }

    public boolean isRetracted() {
        return retracted;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public List<String> getConcepts() {
        return concepts;
    }

    /**
     * Identifiers of the works this one cites, in the source's own order.
     */
    public List<String> getReferencedWorks() {
        return referencedWorks;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Work)) {
            return false;
        }
        return id.equals(((Work) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Work[" + id + ", " + year + ", " + title + "]";
    }
}
