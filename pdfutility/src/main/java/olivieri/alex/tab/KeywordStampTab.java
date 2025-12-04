package olivieri.alex.tab;

import static olivieri.alex.tab.TabFileChooser.browseForOutputPdf;
import static olivieri.alex.tab.TabFileChooser.browseForPdfFile;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import olivieri.alex.App;

public final class KeywordStampTab {
    private KeywordStampTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField pdfField = new JTextField(25);
        JButton browsePdfButton = new JButton("Seleziona PDF");
        JTextField outputField = new JTextField(25);
        JButton browseOutputButton = new JButton("File di output");
        JTextField keywordField = new JTextField(20);
        JCheckBox caseSensitiveCheck = new JCheckBox("Rispetta maiuscole/minuscole");
        JTextField stampTextField = new JTextField(25);
        JSpinner xSpinner = new JSpinner(new SpinnerNumberModel(0.0, -10000.0, 10000.0, 1.0));
        JSpinner ySpinner = new JSpinner(new SpinnerNumberModel(0.0, -10000.0, 10000.0, 1.0));
        JButton stampButton = new JButton("Applica timbro");

        browsePdfButton.addActionListener(event -> browseForPdfFile(parent, pdfField, outputField,
                path -> controller.buildDefaultKeywordStampName(path, keywordField.getText())));
        browseOutputButton.addActionListener(event -> {
            String suggestion = outputField.getText().trim();
            if (suggestion.isEmpty()) {
                String input = pdfField.getText().trim();
                if (!input.isEmpty()) {
                    try {
                        suggestion = controller.buildDefaultKeywordStampName(Paths.get(input), keywordField.getText());
                    } catch (Exception ignored) {
                        // ignore invalid suggestion
                    }
                }
            }
            browseForOutputPdf(parent, outputField, suggestion);
        });
        stampButton.addActionListener(
                event -> controller.startKeywordStamp(parent, pdfField.getText(), outputField.getText(),
                        keywordField.getText(),
                        stampTextField.getText(), caseSensitiveCheck.isSelected(), xSpinner, ySpinner, stampButton));

        keywordField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update() {
                controller.updateKeywordStampOutputSuggestion(pdfField, outputField, keywordField);
            }
        });

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("PDF sorgente:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(pdfField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browsePdfButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("PDF output:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(outputField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseOutputButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Parola da cercare:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(keywordField, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0;

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        formPanel.add(caseSensitiveCheck, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Testo timbro:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(stampTextField, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0;

        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(new JLabel("Posizione X (pt):"), gbc);

        gbc.gridx = 1;
        formPanel.add(xSpinner, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("Posizione Y (pt):"), gbc);

        gbc.gridx = 3;
        formPanel.add(ySpinner, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(stampButton, BorderLayout.PAGE_END);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
