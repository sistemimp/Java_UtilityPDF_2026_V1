package olivieri.alex.tab;

import static olivieri.alex.tab.TabFileChooser.browseForPdfFile;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import olivieri.alex.App;

public final class ConditionalBlankTab {
    private ConditionalBlankTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField inputField = new JTextField(25);
        JButton browseButton = new JButton("Seleziona PDF");
        JTextField phraseField = new JTextField(20);
        JCheckBox caseSensitiveCheck = new JCheckBox("Rispetta maiuscole/minuscole");
        JCheckBox oddPageOnlyCheck = new JCheckBox("Solo se la frase è su una pagina dispari");
        JTextField outputField = new JTextField("", 20);
        JButton processButton = new JButton("Inserisci pagine dopo testo");

        browseButton.addActionListener(event -> browseForPdfFile(parent, inputField, outputField,
                path -> controller.buildDefaultBlankAfterPhraseName(path, phraseField.getText())));
        processButton.addActionListener(event -> controller.startInsertBlankAfterPhrase(parent, inputField.getText(),
                phraseField.getText(), caseSensitiveCheck.isSelected(), oddPageOnlyCheck.isSelected(),
                outputField.getText(), processButton));

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
        formPanel.add(inputField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Frase/Parola:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(phraseField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        formPanel.add(caseSensitiveCheck, gbc);

        gbc.gridy = 3;
        formPanel.add(oddPageOnlyCheck, gbc);

        gbc.gridy = 4;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("File di output:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(outputField, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(processButton, BorderLayout.PAGE_END);
        JLabel descriptionLabel = new JLabel("Inserisce una pagina bianca subito dopo la frase o parola indicata.");
        descriptionLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));
        container.add(descriptionLabel, BorderLayout.NORTH);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
