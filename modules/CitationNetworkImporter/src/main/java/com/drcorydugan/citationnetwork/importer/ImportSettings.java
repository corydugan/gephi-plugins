/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.importer;

import com.drcorydugan.citationnetwork.graph.WalkSettings;
import java.util.prefs.Preferences;

/**
 * Everything the wizard collects: what to walk, and how to reach the source.
 *
 * <p>The email address and the optional key are remembered between sessions in
 * the user's own preferences, because retyping them on every import is the
 * kind of friction that stops people using the polite pool at all. Neither is
 * ever written into the plugin's source.</p>
 */
public final class ImportSettings {

    private static final String PREFERENCE_MAILTO = "mailto";
    private static final String PREFERENCE_API_KEY = "apiKey";

    private final WalkSettings walk = new WalkSettings();
    private String mailto = "";
    private String apiKey = "";

    public WalkSettings getWalk() {
        return walk;
    }

    public String getMailto() {
        return mailto;
    }

    public void setMailto(String mailto) {
        this.mailto = mailto == null ? "" : mailto.trim();
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    /**
     * Load the remembered address and key. Anything else is deliberately not
     * remembered, since a query and a depth belong to one import.
     */
    public void loadRemembered() {
        Preferences preferences = preferences();
        mailto = preferences.get(PREFERENCE_MAILTO, "");
        apiKey = preferences.get(PREFERENCE_API_KEY, "");
    }

    public void remember() {
        Preferences preferences = preferences();
        preferences.put(PREFERENCE_MAILTO, mailto);
        preferences.put(PREFERENCE_API_KEY, apiKey);
    }

    private static Preferences preferences() {
        return Preferences.userNodeForPackage(ImportSettings.class);
    }
}
