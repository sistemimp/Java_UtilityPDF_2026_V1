package olivieri.alex.tab;

import static olivieri.alex.tab.TabFileChooser.browseForOutputPdf;
import static olivieri.alex.tab.TabFileChooser.browseForPdfFile;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import olivieri.alex.App;

public final class RemovePagesTab {
    private RemovePagesTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField pdfField = new JTextField(25);
        JButton browsePdfButton = new JButton("Seleziona PDF");
        JTextField outputField = new JTextField(25);
        JButton browseOutputButton = new JButton("File di output");
        JTextField searchField = new JTextField(20);
        JCheckBox caseSensitiveCheck = new JCheckBox("Rispetta maiuscole/minuscole");
        JButton removeButton = new JButton("Rimuovi pagine");

        browsePdfButton.addActionListener(
                event -> browseForPdfFile(parent, pdfField, outputField, controller::buildDefaultRemovalName));
        browseOutputButton.addActionListener(event -> {
            String suggestion = outputField.getText().trim();
            if (suggestion.isEmpty()) {
                suggestion = controller.suggestRemovalOutputName(pdfField.getText(), searchField.getText());
            }
            browseForOutputPdf(parent, outputField, suggestion);
        });
        removeButton.addActionListener(
                event -> controller.startStringRemoval(parent, pdfField.getText(), outputField.getText(),
                        searchField.getText(), caseSensitiveCheck.isSelected(), removeButton));

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
        formPanel.add(new JLabel("Stringa da cercare:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(searchField, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0;

        gbc.gridx = 1;
        gbc.gridy = 3;
        formPanel.add(caseSensitiveCheck, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(removeButton, BorderLayout.PAGE_END);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
