/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.source;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An {@link HttpFetcher} on the Java Development Kit's own client, with the
 * back off behaviour a public bibliographic interface asks for.
 *
 * <p>Two retry signals are honoured: status 429 with a Retry-After header, and
 * a body of the form {@code {"error":"Rate limit exceeded","retryAfter":39}}.
 * Status codes in the 500 range are retried as transient.</p>
 */
public final class JdkHttpFetcher implements HttpFetcher {

    private static final Pattern RETRY_AFTER_IN_BODY =
            Pattern.compile("\"retryAfter\"\\s*:\\s*(\\d+)");

    private final HttpClient client;
    private final String userAgent;
    private final int maxAttempts;
    private final Duration maxWait;

    public JdkHttpFetcher(String userAgent) {
        this(userAgent, 4, Duration.ofSeconds(60));
    }

    public JdkHttpFetcher(String userAgent, int maxAttempts, Duration maxWait) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.userAgent = userAgent;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.maxWait = maxWait;
    }

    @Override
    public String get(String url) throws IOException {
        IOException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("the request was interrupted", e);
            }

            int status = response.statusCode();
            String body = response.body();

            if (status >= 200 && status < 300 && !isRateLimitBody(body)) {
                return body;
            }

            boolean retryable = status == 429 || status >= 500 || isRateLimitBody(body);
            last = new IOException("request failed with status " + status
                    + " for " + url + ": " + firstLine(body));
            if (!retryable || attempt == maxAttempts) {
                throw last;
            }
            sleep(waitSeconds(response, body, attempt));
        }
        throw last;
    }

    private static boolean isRateLimitBody(String body) {
        return body != null && body.length() < 1000 && body.contains("\"retryAfter\"");
    }

    private long waitSeconds(HttpResponse<String> response, String body, int attempt) {
        return response.headers().firstValue("Retry-After")
                .map(JdkHttpFetcher::parseLongOrZero)
                .filter(seconds -> seconds > 0)
                .orElseGet(() -> {
                    Matcher m = RETRY_AFTER_IN_BODY.matcher(body == null ? "" : body);
                    if (m.find()) {
                        return Long.parseLong(m.group(1));
                    }
                    return (long) Math.pow(2, attempt);
                });
    }

    private static long parseLongOrZero(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void sleep(long seconds) throws IOException {
        long capped = Math.min(seconds, maxWait.toSeconds());
        try {
            Thread.sleep(capped * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("the wait before retrying was interrupted", e);
        }
    }

    private static String firstLine(String body) {
        if (body == null) {
            return "";
        }
        int cut = Math.min(body.length(), 200);
        return body.substring(0, cut).replace('\n', ' ');
    }
}
