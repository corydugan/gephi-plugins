/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source;

import java.util.List;

/**
 * A bibliographic source that can be walked as a citation graph.
 *
 * <p>OpenAlex is the first implementation. The interface exists so that
 * Crossref, PubMed or an institutional source can be added behind the same
 * import wizard rather than as a second plugin.</p>
 *
 * <p>Implementations do no caching and no threading of their own. Caching is a
 * decorator on the {@link HttpFetcher} they are built with, and progress and
 * cancellation belong to the importer that drives them.</p>
 */
public interface CitationSource {

    /**
     * A stable identifier for this source, for example "openalex". It is used
     * in settings and in the node attribute recording where a work came from.
     */
    String getId();

    /**
     * The name a user reads in the wizard, for example "OpenAlex".
     */
    String getDisplayName();

    /**
     * Fetch one work by whatever identifier the user pasted: the source's own
     * identifier, a digital object identifier, or a uniform resource locator
     * carrying either.
     *
     * @throws SourceException if the work cannot be found or cannot be read
     */
    Work fetchWork(String identifier) throws SourceException;

    /**
     * Search the source's free text index and return at most {@code limit}
     * works, most relevant first.
     *
     * @throws SourceException if the source refuses or the payload fails to parse
     */
    List<Work> search(String query, int limit) throws SourceException;

    /**
     * The works that cite {@code workId}, at most {@code limit} of them.
     *
     * @throws SourceException if the source refuses or the payload fails to parse
     */
    List<Work> fetchCiting(String workId, int limit) throws SourceException;

    /**
     * The works cited BY {@code workId}, at most {@code limit} of them. The
     * identifiers are already on {@link Work#getReferencedWorks()}; this
     * resolves them into full works.
     *
     * @throws SourceException if the source refuses or the payload fails to parse
     */
    List<Work> fetchReferences(String workId, int limit) throws SourceException;
}
