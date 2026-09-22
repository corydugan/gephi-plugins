/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.importer;

import org.gephi.io.importer.spi.Importer;
import org.gephi.io.importer.spi.ImporterWizardUI;
import org.gephi.io.importer.spi.WizardImporter;
import org.openide.WizardDescriptor;
import org.openide.util.lookup.ServiceProvider;

/**
 * The wizard Gephi shows under File, Import, and which hands the collected
 * settings to the importer.
 */
@ServiceProvider(service = ImporterWizardUI.class)
public class CitationNetworkWizardUI implements ImporterWizardUI {

    private final CitationNetworkWizardPanel panel = new CitationNetworkWizardPanel();

    @Override
    public String getDisplayName() {
        return "Citation network";
    }

    @Override
    public String getCategory() {
        return "Bibliographic sources";
    }

    @Override
    public String getDescription() {
        return "Import a citation network from OpenAlex, following citations "
                + "for as many generations as you ask for.";
    }

    // The extension point itself declares a raw Panel array, so these three
    // methods cannot be generic without breaking the interface.
    @Override
    @SuppressWarnings("rawtypes")
    public WizardDescriptor.Panel[] getPanels() {
        return new WizardDescriptor.Panel[]{panel};
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void setup(WizardDescriptor.Panel panel) {
        ImportSettings settings = new ImportSettings();
        settings.loadRemembered();
        this.panel.getPanel().read(settings);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void unsetup(WizardImporter importer, WizardDescriptor.Panel panel) {
        if (importer instanceof CitationNetworkImporter) {
            ImportSettings settings = ((CitationNetworkImporter) importer).getSettings();
            this.panel.getPanel().write(settings);
            settings.remember();
        }
    }

    @Override
    public boolean isUIForImporter(Importer importer) {
        return importer instanceof CitationNetworkImporter;
    }
}
