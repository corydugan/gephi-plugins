/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source.semanticscholar;

import com.drcorydugan.citationnetwork.source.SourceException;
import com.drcorydugan.citationnetwork.source.Work;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a Semantic Scholar paper payload into a {@link Work}.
 *
 * <p>A reference or citation page carries the neighbour under its own key,
 * {@code citedPaper} or {@code citingPaper}, and either may be null when the
 * source holds no record for it.</p>
 */
public final class SemanticScholarMapper {

    private SemanticScholarMapper() {
    }

    public static Work toWork(JsonObject json) {
        String id = text(json, "paperId");
        if (id == null) {
            return null;
        }
        String doi = null;
        if (json.has("externalIds") && json.get("externalIds").isJsonObject()) {
            doi = text(json.getAsJsonObject("externalIds"), "DOI");
        }
        List<String> authors = new ArrayList<>();
        if (json.has("authors") && json.get("authors").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("authors")) {
                String name = text(element.getAsJsonObject(), "name");
                if (name != null) {
                    authors.add(name);
                }
            }
        }
        String type = null;
        if (json.has("publicationTypes") && json.get("publicationTypes").isJsonArray()
                && !json.getAsJsonArray("publicationTypes").isEmpty()) {
            type = json.getAsJsonArray("publicationTypes").get(0).getAsString();
        }
        return new Work(id,
                doi == null ? null : doi.toLowerCase(java.util.Locale.ROOT),
                text(json, "title"),
                integer(json, "year"),
                text(json, "venue"),
                integer(json, "citationCount") == null ? 0 : integer(json, "citationCount"),
                json.has("isOpenAccess") && !json.get("isOpenAccess").isJsonNull()
                        && json.get("isOpenAccess").getAsBoolean(),
                false,
                authors,
                List.of(),
                List.of(),
                type,
                List.of());
    }

    /**
     * Read a citations or references page. {@code key} is the field holding the
     * neighbour, and a null entry under it is skipped.
     */
    public static List<Work> toWorks(JsonObject page, String key) {
        List<Work> works = new ArrayList<>();
        if (!page.has("data") || page.get("data").isJsonNull()) {
            return works;
        }
        for (JsonElement element : page.getAsJsonArray("data")) {
            JsonObject entry = element.getAsJsonObject();
            if (!entry.has(key) || entry.get(key).isJsonNull()) {
                continue;
            }
            Work work = toWork(entry.getAsJsonObject(key));
            if (work != null) {
                works.add(work);
            }
        }
        return works;
    }

    /**
     * Whether the source answered with no data at all, which happens when a
     * publisher restricts the field rather than when the paper has no
     * neighbours.
     */
    public static boolean isElided(JsonObject page) {
        return page.has("data") && page.get("data").isJsonNull();
    }

    /**
     * The offset of the next page, or null when this is the last one.
     */
    public static Integer nextOffset(JsonObject page) {
        if (!page.has("next") || page.get("next").isJsonNull()) {
            return null;
        }
        return page.get("next").getAsInt();
    }

    public static JsonObject parse(String body) throws SourceException {
        try {
            JsonElement element = JsonParser.parseString(body);
            if (!element.isJsonObject()) {
                throw new SourceException("the source returned something that is not an object");
            }
            JsonObject object = element.getAsJsonObject();
            if (object.has("error")) {
                throw new SourceException("the source returned an error: " + text(object, "error"));
            }
            if (object.has("message") && !object.has("paperId") && !object.has("data")) {
                throw new SourceException("the source returned: " + text(object, "message"));
            }
            return object;
        } catch (JsonParseException e) {
            throw new SourceException("the source returned a payload that did not parse", e);
        }
    }

    private static String text(JsonObject json, String field) {
        if (json == null || !json.has(field) || json.get(field).isJsonNull()) {
            return null;
        }
        return json.get(field).getAsString();
    }

    private static Integer integer(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) {
            return null;
        }
        return json.get(field).getAsInt();
    }
}
