/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.graph;

import com.drcorydugan.citationnetwork.source.CitationSource;
import com.drcorydugan.citationnetwork.source.SourceException;
import com.drcorydugan.citationnetwork.source.Work;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A small citation network held in memory, so a walk can be checked against a
 * shape that is known exactly.
 */
final class FakeSource implements CitationSource {

    private final Map<String, Work> works = new LinkedHashMap<>();
    private final Map<String, List<String>> citedBy = new LinkedHashMap<>();
    private final Set<String> failing = new LinkedHashSet<>();
    private int requests;

    void addWork(String id, List<String> references) {
        works.put(id, new Work(id, null, "Title of " + id, 2020, "A journal", 0,
                false, false, List.of("An author"), List.of(), references));
        for (String reference : references) {
            citedBy.computeIfAbsent(reference, key -> new ArrayList<>()).add(id);
        }
    }

    /** Make one work's neighbourhood fail, as a refused request would. */
    void failOn(String id) {
        failing.add(id);
    }

    int getRequests() {
        return requests;
    }

    @Override
    public String getId() {
        return "fake";
    }

    @Override
    public String getDisplayName() {
        return "Fake";
    }

    @Override
    public Work fetchWork(String identifier) throws SourceException {
        requests++;
        Work work = works.get(identifier);
        if (work == null) {
            throw new SourceException("no such work: " + identifier);
        }
        return work;
    }

    @Override
    public List<Work> search(String query, int limit) {
        requests++;
        List<Work> found = new ArrayList<>(works.values());
        return found.subList(0, Math.min(limit, found.size()));
    }

    @Override
    public List<Work> fetchCiting(String workId, int limit) throws SourceException {
        requests++;
        if (failing.contains(workId)) {
            throw new SourceException("the source refused " + workId);
        }
        List<Work> found = new ArrayList<>();
        for (String id : citedBy.getOrDefault(workId, List.of())) {
            found.add(works.get(id));
        }
        return found.subList(0, Math.min(limit, found.size()));
    }

    @Override
    public List<Work> fetchReferences(String workId, int limit) throws SourceException {
        requests++;
        if (failing.contains(workId)) {
            throw new SourceException("the source refused " + workId);
        }
        List<Work> found = new ArrayList<>();
        Work work = works.get(workId);
        if (work != null) {
            for (String id : work.getReferencedWorks()) {
                Work referenced = works.get(id);
                if (referenced != null) {
                    found.add(referenced);
                }
            }
        }
        return found.subList(0, Math.min(limit, found.size()));
    }
}
