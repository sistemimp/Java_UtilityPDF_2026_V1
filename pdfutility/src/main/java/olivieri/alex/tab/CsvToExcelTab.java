package olivieri.alex.tab;

import static olivieri.alex.tab.TabFileChooser.browseForCsv;
import static olivieri.alex.tab.TabFileChooser.browseForExcelOutput;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import olivieri.alex.App;

public final class CsvToExcelTab {
    private CsvToExcelTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField csvField = new JTextField(25);
        JButton browseCsvButton = new JButton("File CSV");
        JTextField excelField = new JTextField(25);
        JButton browseExcelButton = new JButton("File Excel");
        JButton convertButton = new JButton("Converti");

        final String[] lastSuggestion = { "" };
        DocumentListener suggestionListener = new DocumentListener() {
            private void update() {
                String suggestion = controller.buildDefaultExcelName(csvField.getText());
                if (suggestion.isEmpty()) {
                    return;
                }
                String current = excelField.getText().trim();
                if (current.isEmpty() || current.equals(lastSuggestion[0])) {
                    excelField.setText(suggestion);
                    lastSuggestion[0] = suggestion;
                }
            }

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
        };
        csvField.getDocument().addDocumentListener(suggestionListener);

        browseCsvButton.addActionListener(event -> browseForCsv(parent, csvField));
        browseExcelButton.addActionListener(event -> {
            String suggestion = controller.buildDefaultExcelName(csvField.getText());
            browseForExcelOutput(parent, excelField, suggestion);
        });
        convertButton.addActionListener(
                event -> controller.startCsvToExcelConversion(parent, csvField.getText(), excelField.getText(),
                        convertButton));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("File CSV:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(csvField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseCsvButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Excel di output:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(excelField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseExcelButton, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(convertButton, BorderLayout.PAGE_END);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
