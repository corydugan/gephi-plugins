/*
 * Copyright 2026 Cory Dugan
 *
 * Licensed under the MIT License. See the LICENSE file distributed with this
 * plugin for the full text.
 */
package com.drcorydugan.citationnetwork.importer;

import com.drcorydugan.citationnetwork.graph.WalkSettings;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * The one screen of the import wizard.
 *
 * <p>Built in code rather than in a form file so the whole panel can be read
 * in one place, which is what a plugin reviewer has to do.</p>
 */
public class CitationNetworkPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextField queryField = new JTextField(28);
    private final JTextField seedField = new JTextField(28);
    private final JSpinner seedCount = new JSpinner(new SpinnerNumberModel(10, 1, 200, 1));
    private final JComboBox<String> directionBox =
            new JComboBox<>(new String[]{"Works citing these", "Works cited by these", "Both"});
    private final JSpinner depth = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
    private final JSpinner maximumWorks = new JSpinner(new SpinnerNumberModel(500, 10, 20000, 50));
    private final JSpinner neighbours = new JSpinner(new SpinnerNumberModel(25, 1, 200, 5));
    private final JTextField mailtoField = new JTextField(28);
    private final JPasswordField apiKeyField = new JPasswordField(28);

    public CitationNetworkPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        int row = 0;
        addRow(row++, "Search query", queryField,
                "Leave empty when a work identifier is given below.");
        addRow(row++, "Or one work", seedField,
                "An OpenAlex identifier such as W3198910543, or a digital object identifier.");
        addRow(row++, "Seed works from the search", seedCount, null);
        addRow(row++, "Follow", directionBox, null);
        addRow(row++, "Generations", depth, null);
        addRow(row++, "Maximum works", maximumWorks, null);
        addRow(row++, "Neighbours per work", neighbours, null);
        addRow(row++, "Your email address", mailtoField,
                "OpenAlex serves a faster pool to requests that carry an address. Optional.");
        addRow(row, "OpenAlex key", apiKeyField,
                "Optional. A free key removes the rate limit on searches.");
    }

    private void addRow(int row, String label, java.awt.Component field, String hint) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row * 2;
        left.anchor = GridBagConstraints.LINE_END;
        left.insets = new Insets(4, 4, 4, 8);
        add(new JLabel(label), left);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row * 2;
        right.anchor = GridBagConstraints.LINE_START;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.weightx = 1.0;
        right.insets = new Insets(4, 0, 4, 4);
        add(field, right);

        if (hint != null) {
            GridBagConstraints note = new GridBagConstraints();
            note.gridx = 1;
            note.gridy = row * 2 + 1;
            note.anchor = GridBagConstraints.LINE_START;
            note.insets = new Insets(0, 0, 6, 4);
            JLabel hintLabel = new JLabel(hint);
            hintLabel.setFont(hintLabel.getFont().deriveFont(hintLabel.getFont().getSize2D() - 1f));
            add(hintLabel, note);
        }
    }

    /**
     * Whether the panel holds enough to run: a query or a work identifier.
     */
    public boolean isComplete() {
        return !queryField.getText().trim().isEmpty() || !seedField.getText().trim().isEmpty();
    }

    public void read(ImportSettings settings) {
        mailtoField.setText(settings.getMailto());
        apiKeyField.setText(settings.getApiKey());
    }

    public void write(ImportSettings settings) {
        settings.setMailto(mailtoField.getText());
        settings.setApiKey(new String(apiKeyField.getPassword()));
        WalkSettings walk = settings.getWalk();
        walk.setQuery(queryField.getText());
        walk.setSeedIdentifier(seedField.getText());
        walk.setSeedCount((Integer) seedCount.getValue());
        walk.setDepth((Integer) depth.getValue());
        walk.setMaximumWorks((Integer) maximumWorks.getValue());
        walk.setNeighboursPerWork((Integer) neighbours.getValue());
        walk.setDirection(directionOf(directionBox.getSelectedIndex()));
    }

    static WalkSettings.Direction directionOf(int index) {
        switch (index) {
            case 1:
                return WalkSettings.Direction.REFERENCES;
            case 2:
                return WalkSettings.Direction.BOTH;
            default:
                return WalkSettings.Direction.CITING;
        }
    }
}
