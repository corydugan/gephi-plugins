/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source.openalex;

import com.drcorydugan.citationnetwork.source.SourceException;
import com.drcorydugan.citationnetwork.source.Work;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns an OpenAlex work payload into a {@link Work}.
 *
 * <p>Every field is read from the payload or left absent. Nothing is inferred,
 * and a missing field never becomes a substituted default, so a graph built
 * from these works shows what the source actually holds.</p>
 */
public final class OpenAlexMapper {

    private OpenAlexMapper() {
    }

    /**
     * Parse a single work document.
     */
    public static Work toWork(JsonObject json) {
        String id = shortId(text(json, "id"));
        String doi = bareDoi(text(json, "doi"));
        String title = text(json, "title");
        if (title == null) {
            title = text(json, "display_name");
        }
        Integer year = integer(json, "publication_year");
        String venue = venueOf(json);
        int citedBy = json.has("cited_by_count") && !json.get("cited_by_count").isJsonNull()
                ? json.get("cited_by_count").getAsInt() : 0;
        boolean openAccess = json.has("open_access")
                && json.getAsJsonObject("open_access").has("is_oa")
                && json.getAsJsonObject("open_access").get("is_oa").getAsBoolean();
        boolean retracted = json.has("is_retracted")
                && !json.get("is_retracted").isJsonNull()
                && json.get("is_retracted").getAsBoolean();

        List<String> authors = new ArrayList<>();
        List<String> institutions = new ArrayList<>();
        if (json.has("authorships") && json.get("authorships").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("authorships")) {
                JsonObject authorship = element.getAsJsonObject();
                if (authorship.has("author") && authorship.get("author").isJsonObject()) {
                    String name = text(authorship.getAsJsonObject("author"), "display_name");
                    if (name != null) {
                        authors.add(name);
                    }
                }
                if (authorship.has("institutions") && authorship.get("institutions").isJsonArray()) {
                    for (JsonElement each : authorship.getAsJsonArray("institutions")) {
                        String name = text(each.getAsJsonObject(), "display_name");
                        if (name != null && !institutions.contains(name)) {
                            institutions.add(name);
                        }
                    }
                }
            }
        }

        List<String> concepts = new ArrayList<>();
        if (json.has("concepts") && json.get("concepts").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("concepts")) {
                String name = text(element.getAsJsonObject(), "display_name");
                if (name != null) {
                    concepts.add(name);
                }
            }
        }

        List<String> references = new ArrayList<>();
        if (json.has("referenced_works") && json.get("referenced_works").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("referenced_works")) {
                String reference = shortId(element.getAsString());
                if (reference != null) {
                    references.add(reference);
                }
            }
        }

        return new Work(id, doi, title, year, venue, citedBy, openAccess, retracted,
                authors, institutions, concepts, text(json, "type"), references);
    }

    /**
     * Parse a results page and return the works on it.
     */
    public static List<Work> toWorks(JsonObject page) {
        List<Work> works = new ArrayList<>();
        if (page.has("results") && page.get("results").isJsonArray()) {
            JsonArray results = page.getAsJsonArray("results");
            for (JsonElement element : results) {
                works.add(toWork(element.getAsJsonObject()));
            }
        }
        return works;
    }

    /**
     * The cursor for the next page, or null when the page is the last one.
     */
    public static String nextCursor(JsonObject page) {
        if (!page.has("meta") || !page.get("meta").isJsonObject()) {
            return null;
        }
        return text(page.getAsJsonObject("meta"), "next_cursor");
    }

    /**
     * Parse a response body, failing with a readable message rather than a
     * parser stack trace when the source returned something unexpected.
     */
    public static JsonObject parse(String body) throws SourceException {
        try {
            JsonElement element = JsonParser.parseString(body);
            if (!element.isJsonObject()) {
                throw new SourceException("the source returned something that is not an object");
            }
            JsonObject object = element.getAsJsonObject();
            if (object.has("error")) {
                throw new SourceException("the source returned an error: " + text(object, "error")
                        + ". " + String.valueOf(text(object, "message")));
            }
            return object;
        } catch (JsonParseException e) {
            throw new SourceException("the source returned a payload that did not parse", e);
        }
    }

    private static String venueOf(JsonObject json) {
        if (json.has("primary_location") && json.get("primary_location").isJsonObject()) {
            JsonObject location = json.getAsJsonObject("primary_location");
            if (location.has("source") && location.get("source").isJsonObject()) {
                return text(location.getAsJsonObject("source"), "display_name");
            }
        }
        return null;
    }

    /**
     * W3198910543 from https://openalex.org/W3198910543, and null from null.
     */
    static String shortId(String value) {
        if (value == null) {
            return null;
        }
        int slash = value.lastIndexOf('/');
        return slash >= 0 ? value.substring(slash + 1) : value;
    }

    /**
     * 10.1016/s2352-3026(21)00193-9 from https://doi.org/10.1016/s2352-3026(21)00193-9.
     */
    static String bareDoi(String value) {
        if (value == null) {
            return null;
        }
        String lower = value.toLowerCase(java.util.Locale.ROOT);
        int index = lower.indexOf("doi.org/");
        return index >= 0 ? value.substring(index + "doi.org/".length()) : value;
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
