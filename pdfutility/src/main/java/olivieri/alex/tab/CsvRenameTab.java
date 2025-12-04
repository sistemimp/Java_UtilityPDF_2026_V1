package olivieri.alex.tab;

import static olivieri.alex.tab.TabFileChooser.browseForCsv;
import static olivieri.alex.tab.TabFileChooser.browseForDirectory;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import olivieri.alex.App;

public final class CsvRenameTab {
    private CsvRenameTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField directoryField = new JTextField(25);
        JButton browseDirButton = new JButton("Cartella PDF");
        JTextField csvField = new JTextField(25);
        JButton browseCsvButton = new JButton("File CSV");
        JButton renameButton = new JButton("Rinomina");

        browseDirButton.addActionListener(event -> browseForDirectory(parent, directoryField));
        browseCsvButton.addActionListener(event -> browseForCsv(parent, csvField));
        renameButton.addActionListener(
                event -> controller.startCsvRename(parent, directoryField.getText(), csvField.getText(), renameButton));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Cartella PDF:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(directoryField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseDirButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("CSV:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(csvField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseCsvButton, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(renameButton, BorderLayout.PAGE_END);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
