/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.importer;

import java.awt.Component;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

/**
 * The wizard step that shows {@link CitationNetworkPanel}.
 */
public class CitationNetworkWizardPanel implements WizardDescriptor.Panel<WizardDescriptor> {

    private CitationNetworkPanel component;

    @Override
    public Component getComponent() {
        if (component == null) {
            component = new CitationNetworkPanel();
            component.setName("Citation network");
        }
        return component;
    }

    /**
     * The panel itself, for the wizard user interface to read settings out of.
     */
    public CitationNetworkPanel getPanel() {
        getComponent();
        return component;
    }

    @Override
    public HelpCtx getHelp() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public void readSettings(WizardDescriptor settings) {
        // The wizard carries no state of its own; the importer holds it.
    }

    @Override
    public void storeSettings(WizardDescriptor settings) {
        // Written in the wizard user interface's unsetup, which has the importer.
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        // The single panel is always valid enough to finish on, so no events.
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        // See addChangeListener.
    }
}
