/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.importer;

import org.gephi.io.importer.spi.ImporterBuilder;
import org.gephi.io.importer.spi.WizardImporter;
import org.gephi.io.importer.spi.WizardImporterBuilder;
import org.openide.util.lookup.ServiceProvider;

/**
 * Registers the importer with Gephi.
 */
@ServiceProvider(service = ImporterBuilder.class)
public class CitationNetworkImporterBuilder implements WizardImporterBuilder {

    /** The identifier Gephi uses to pair this builder with its wizard. */
    public static final String IDENTIFIER = "citation-network-importer";

    @Override
    public WizardImporter buildImporter() {
        return new CitationNetworkImporter();
    }

    @Override
    public String getName() {
        return IDENTIFIER;
    }
}
