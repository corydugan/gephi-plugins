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
import org.openide.util.NbBundle;

/**
 * The one screen of the import wizard.
 *
 * <p>Every label and hint comes from the module's Bundle.properties.</p>
 */
public class CitationNetworkPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JComboBox<String> sourceBox = new JComboBox<>(new String[]{
            text("CitationNetworkPanel.source.openalex"),
            text("CitationNetworkPanel.source.semanticscholar")});
    private final JTextField queryField = new JTextField(28);
    private final JTextField seedField = new JTextField(28);
    private final JSpinner seedCount = new JSpinner(new SpinnerNumberModel(10, 1, 200, 1));
    private final JComboBox<String> directionBox = new JComboBox<>(new String[]{
            text("CitationNetworkPanel.direction.citing"),
            text("CitationNetworkPanel.direction.references"),
            text("CitationNetworkPanel.direction.both")});
    private final JSpinner depth = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
    private final JSpinner maximumWorks = new JSpinner(new SpinnerNumberModel(500, 10, 20000, 50));
    private final JSpinner neighbours = new JSpinner(new SpinnerNumberModel(25, 1, 200, 5));
    private final JTextField mailtoField = new JTextField(28);
    private final JPasswordField apiKeyField = new JPasswordField(28);

    public CitationNetworkPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        int row = 0;
        addRow(row++, "source", sourceBox, true);
        addRow(row++, "query", queryField, true);
        addRow(row++, "seed", seedField, true);
        addRow(row++, "seedCount", seedCount, false);
        addRow(row++, "direction", directionBox, false);
        addRow(row++, "depth", depth, false);
        addRow(row++, "maximum", maximumWorks, false);
        addRow(row++, "neighbours", neighbours, false);
        addRow(row++, "mailto", mailtoField, true);
        addRow(row, "apiKey", apiKeyField, true);
    }

    private static String text(String key) {
        return NbBundle.getMessage(CitationNetworkPanel.class, key);
    }

    private void addRow(int row, String key, java.awt.Component field, boolean withHint) {
        String label = text("CitationNetworkPanel." + key + ".label");
        String hint = withHint ? text("CitationNetworkPanel." + key + ".hint") : null;
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
        sourceBox.setSelectedIndex(
                ImportSettings.SOURCE_SEMANTIC_SCHOLAR.equals(settings.getSourceId()) ? 1 : 0);
        mailtoField.setText(settings.getMailto());
        apiKeyField.setText(settings.getApiKey());
    }

    public void write(ImportSettings settings) {
        settings.setSourceId(sourceIdOf(sourceBox.getSelectedIndex()));
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

    static String sourceIdOf(int index) {
        return index == 1 ? ImportSettings.SOURCE_SEMANTIC_SCHOLAR : ImportSettings.SOURCE_OPENALEX;
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
