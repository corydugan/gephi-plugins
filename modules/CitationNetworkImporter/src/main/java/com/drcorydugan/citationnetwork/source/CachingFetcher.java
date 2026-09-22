/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.HexFormat;

/**
 * Wraps another {@link HttpFetcher} and keeps every response on disk, keyed by
 * a hash of the uniform resource locator.
 *
 * <p>A citation walk revisits the same work many times, and a cached run can
 * be repeated with the network unplugged, which is what makes a reported graph
 * reproducible.</p>
 *
 * <p>The key is the uniform resource locator alone. Headers carry a key, which
 * changes what a source allows rather than what it answers.</p>
 */
public final class CachingFetcher implements HttpFetcher {

    private final HttpFetcher delegate;
    private final Path directory;

    public CachingFetcher(HttpFetcher delegate, Path directory) throws IOException {
        this.delegate = delegate;
        this.directory = directory;
        Files.createDirectories(directory);
    }

    @Override
    public String get(String url, Map<String, String> headers) throws IOException {
        Path file = directory.resolve(key(url) + ".json");
        if (Files.isRegularFile(file)) {
            return Files.readString(file, StandardCharsets.UTF_8);
        }
        String body = delegate.get(url, headers);
        // One partial file per key, so a crash leaves at most what is in flight.
        Path temp = directory.resolve(key(url) + ".partial");
        Files.writeString(temp, body, StandardCharsets.UTF_8);
        Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return body;
    }

    /**
     * Whether this uniform resource locator has already been fetched.
     */
    public boolean isCached(String url) {
        return Files.isRegularFile(directory.resolve(key(url) + ".json"));
    }

    private static String key(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required and was not available", e);
        }
    }
}
