/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source.openalex;

import com.drcorydugan.citationnetwork.source.CitationSource;
import com.drcorydugan.citationnetwork.source.HttpFetcher;
import com.drcorydugan.citationnetwork.source.SourceException;
import com.drcorydugan.citationnetwork.source.Work;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The OpenAlex implementation of {@link CitationSource}.
 *
 * <p>Free text search is rate limited for anonymous callers, so the optional
 * key is passed through when the user supplies one. The email address goes in
 * the polite pool parameter, which is OpenAlex's own convention for faster
 * service; both values come from the user and neither is held in this code.</p>
 */
public final class OpenAlexSource implements CitationSource {

    /** OpenAlex refuses a page larger than this. */
    public static final int MAX_PER_PAGE = 200;

    /** Identifiers per batched lookup, kept well inside the filter length limit. */
    static final int ID_BATCH = 50;

    private final HttpFetcher fetcher;
    private final String baseUrl;
    private final String mailto;
    private final String apiKey;

    public OpenAlexSource(HttpFetcher fetcher, String mailto, String apiKey) {
        this(fetcher, "https://api.openalex.org", mailto, apiKey);
    }

    OpenAlexSource(HttpFetcher fetcher, String baseUrl, String mailto, String apiKey) {
        this.fetcher = fetcher;
        this.baseUrl = baseUrl;
        this.mailto = emptyToNull(mailto);
        this.apiKey = emptyToNull(apiKey);
    }

    @Override
    public String getId() {
        return "openalex";
    }

    @Override
    public String getDisplayName() {
        return "OpenAlex";
    }

    @Override
    public Work fetchWork(String identifier) throws SourceException {
        String path = "/works/" + normaliseIdentifier(identifier);
        JsonObject json = OpenAlexMapper.parse(get(url(path, new LinkedHashMap<>())));
        return OpenAlexMapper.toWork(json);
    }

    @Override
    public List<Work> search(String query, int limit) throws SourceException {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("search", query);
        return page("/works", parameters, limit);
    }

    @Override
    public List<Work> fetchCiting(String workId, int limit) throws SourceException {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("filter", "cites:" + OpenAlexMapper.shortId(workId));
        return page("/works", parameters, limit);
    }

    @Override
    public List<Work> fetchReferences(String workId, int limit) throws SourceException {
        Work work = fetchWork(workId);
        List<String> ids = work.getReferencedWorks();
        if (ids.size() > limit) {
            ids = ids.subList(0, limit);
        }
        return fetchWorks(ids);
    }

    /**
     * Resolve a list of identifiers in batches, which is far cheaper than one
     * request per work.
     */
    public List<Work> fetchWorks(List<String> ids) throws SourceException {
        List<Work> works = new ArrayList<>();
        for (int start = 0; start < ids.size(); start += ID_BATCH) {
            List<String> batch = ids.subList(start, Math.min(ids.size(), start + ID_BATCH));
            Map<String, String> parameters = new LinkedHashMap<>();
            parameters.put("filter", "openalex_id:" + String.join("|", batch));
            works.addAll(page("/works", parameters, batch.size()));
        }
        return works;
    }

    /**
     * Walk pages with a cursor until {@code limit} works are collected or the
     * source runs out.
     */
    private List<Work> page(String path, Map<String, String> parameters, int limit)
            throws SourceException {
        List<Work> collected = new ArrayList<>();
        String cursor = "*";
        while (collected.size() < limit && cursor != null) {
            Map<String, String> query = new LinkedHashMap<>(parameters);
            query.put("per-page", String.valueOf(Math.min(MAX_PER_PAGE, limit - collected.size())));
            query.put("cursor", cursor);
            JsonObject json = OpenAlexMapper.parse(get(url(path, query)));
            List<Work> works = OpenAlexMapper.toWorks(json);
            if (works.isEmpty()) {
                break;
            }
            collected.addAll(works);
            cursor = OpenAlexMapper.nextCursor(json);
        }
        if (collected.size() > limit) {
            return new ArrayList<>(collected.subList(0, limit));
        }
        return collected;
    }

    private String get(String url) throws SourceException {
        try {
            return fetcher.get(url);
        } catch (IOException e) {
            throw new SourceException("OpenAlex could not be reached: " + e.getMessage(), e);
        }
    }

    String url(String path, Map<String, String> parameters) {
        Map<String, String> all = new LinkedHashMap<>(parameters);
        if (mailto != null) {
            all.put("mailto", mailto);
        }
        if (apiKey != null) {
            all.put("api_key", apiKey);
        }
        StringBuilder url = new StringBuilder(baseUrl).append(path);
        boolean first = true;
        for (Map.Entry<String, String> entry : all.entrySet()) {
            url.append(first ? '?' : '&');
            first = false;
            url.append(entry.getKey()).append('=').append(encode(entry.getValue()));
        }
        return url.toString();
    }

    /**
     * Accepts a short identifier, a full OpenAlex uniform resource locator, a
     * bare digital object identifier, or a doi.org link, and returns the path
     * segment OpenAlex expects.
     */
    static String normaliseIdentifier(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http") && lower.contains("openalex.org/")) {
            return OpenAlexMapper.shortId(value);
        }
        if (lower.startsWith("doi:")) {
            return "doi:" + value.substring(4);
        }
        if (lower.contains("doi.org/")) {
            return "doi:" + OpenAlexMapper.bareDoi(value);
        }
        if (value.startsWith("10.")) {
            return "doi:" + value;
        }
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
