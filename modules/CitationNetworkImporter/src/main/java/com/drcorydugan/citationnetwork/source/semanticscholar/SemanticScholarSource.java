/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source.semanticscholar;

import com.drcorydugan.citationnetwork.source.CitationSource;
import com.drcorydugan.citationnetwork.source.HttpFetcher;
import com.drcorydugan.citationnetwork.source.SourceException;
import com.drcorydugan.citationnetwork.source.Work;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Semantic Scholar implementation of {@link CitationSource}.
 *
 * <p>The key travels in an x-api-key header. As a query parameter it is
 * ignored, so a user's key would silently do nothing.</p>
 *
 * <p>Paging is by offset rather than by cursor. A publisher can restrict a
 * paper's reference list, in which case the source answers with a null data
 * field; that is reported as no neighbours rather than as a failure, and the
 * {@link #getLastRequestWasElided()} flag lets the caller say so.</p>
 */
public final class SemanticScholarSource implements CitationSource {

    static final int MAX_PAGE = 100;

    private static final String FIELDS =
            "paperId,externalIds,title,year,venue,citationCount,isOpenAccess,authors,publicationTypes";

    private final HttpFetcher fetcher;
    private final String baseUrl;
    private final String apiKey;
    private int elidedCount;

    public SemanticScholarSource(HttpFetcher fetcher, String apiKey) {
        this(fetcher, "https://api.semanticscholar.org/graph/v1", apiKey);
    }

    SemanticScholarSource(HttpFetcher fetcher, String baseUrl, String apiKey) {
        this.fetcher = fetcher;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey == null || apiKey.isBlank() ? null : apiKey.trim();
    }

    @Override
    public String getId() {
        return "semanticscholar";
    }

    @Override
    public String getDisplayName() {
        return "Semantic Scholar";
    }

    /**
     * How many neighbourhood requests came back with the field removed by a
     * publisher rather than empty, counted over the life of this source.
     */
    public int getElidedCount() {
        return elidedCount;
    }

    @Override
    public Work fetchWork(String identifier) throws SourceException {
        String url = baseUrl + "/paper/" + normaliseIdentifier(identifier) + "?fields=" + FIELDS;
        Work work = SemanticScholarMapper.toWork(SemanticScholarMapper.parse(get(url)));
        if (work == null) {
            throw new SourceException("the source returned a paper with no identifier");
        }
        return work;
    }

    @Override
    public List<Work> search(String query, int limit) throws SourceException {
        String url = baseUrl + "/paper/search?query=" + encode(query)
                + "&fields=" + FIELDS + "&limit=" + Math.min(MAX_PAGE, limit);
        JsonObject page = SemanticScholarMapper.parse(get(url));
        List<Work> works = new ArrayList<>();
        if (page.has("data") && !page.get("data").isJsonNull()) {
            for (var element : page.getAsJsonArray("data")) {
                Work work = SemanticScholarMapper.toWork(element.getAsJsonObject());
                if (work != null) {
                    works.add(work);
                }
            }
        }
        return works.size() > limit ? works.subList(0, limit) : works;
    }

    @Override
    public List<Work> fetchCiting(String workId, int limit) throws SourceException {
        return neighbours(workId, "citations", "citingPaper", limit);
    }

    @Override
    public List<Work> fetchReferences(String workId, int limit) throws SourceException {
        return neighbours(workId, "references", "citedPaper", limit);
    }

    private List<Work> neighbours(String workId, String path, String key, int limit)
            throws SourceException {
        List<Work> collected = new ArrayList<>();
        Integer offset = 0;
        while (collected.size() < limit && offset != null) {
            String url = baseUrl + "/paper/" + normaliseIdentifier(workId) + "/" + path
                    + "?fields=" + FIELDS
                    + "&limit=" + Math.min(MAX_PAGE, limit - collected.size())
                    + "&offset=" + offset;
            JsonObject page = SemanticScholarMapper.parse(get(url));
            if (SemanticScholarMapper.isElided(page)) {
                elidedCount++;
                break;
            }
            List<Work> works = SemanticScholarMapper.toWorks(page, key);
            if (works.isEmpty()) {
                break;
            }
            collected.addAll(works);
            offset = SemanticScholarMapper.nextOffset(page);
        }
        return collected.size() > limit ? new ArrayList<>(collected.subList(0, limit)) : collected;
    }

    private String get(String url) throws SourceException {
        try {
            return fetcher.get(url, apiKey == null ? Map.of() : Map.of("x-api-key", apiKey));
        } catch (IOException e) {
            throw new SourceException("Semantic Scholar could not be reached: " + e.getMessage(), e);
        }
    }

    /**
     * Accepts a paper identifier, a bare digital object identifier, a doi.org
     * link, or an already prefixed form such as DOI: or PMID:.
     */
    static String normaliseIdentifier(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("doi.org/")) {
            int index = lower.indexOf("doi.org/");
            return "DOI:" + value.substring(index + "doi.org/".length());
        }
        if (value.startsWith("10.")) {
            return "DOI:" + value;
        }
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
