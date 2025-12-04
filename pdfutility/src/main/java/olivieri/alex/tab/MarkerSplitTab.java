package olivieri.alex.tab;

import static olivieri.alex.tab.TabFileChooser.browseForDirectory;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import olivieri.alex.App;

public final class MarkerSplitTab {
    private MarkerSplitTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField pdfField = new JTextField(25);
        JButton browsePdfButton = new JButton("Seleziona PDF");
        JTextField markerField = new JTextField(20);
        JCheckBox caseSensitiveCheck = new JCheckBox("Rispetta maiuscole/minuscole");
        JTextField outputDirField = new JTextField(25);
        JButton browseOutputButton = new JButton("Cartella base");
        JTextField folderNameField = new JTextField(controller.buildDefaultMarkerFolderName(markerField.getText()), 20);
        JCheckBox appendCheck = new JCheckBox("Se esiste, aggiungi pagine al file generato");
        JButton splitButton = new JButton("Esegui split");

        browsePdfButton.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("PDF", "pdf"));
            chooser.setAcceptAllFileFilterUsed(true);
            if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                pdfField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        browseOutputButton.addActionListener(event -> browseForDirectory(parent, outputDirField));
        splitButton.addActionListener(
                event -> controller.startMarkerSplit(parent, pdfField.getText(), markerField.getText(),
                        caseSensitiveCheck.isSelected(), outputDirField.getText(), folderNameField.getText(),
                        appendCheck.isSelected(), splitButton));

        markerField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFolderSuggestion();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFolderSuggestion();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFolderSuggestion();
            }

            private void updateFolderSuggestion() {
                if (folderNameField.getText().trim().isEmpty()) {
                    String suggestion = controller.buildDefaultMarkerFolderName(markerField.getText());
                    folderNameField.setText(suggestion);
                }
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
        formPanel.add(new JLabel("Stringa marker:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(markerField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        formPanel.add(caseSensitiveCheck, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("Cartella base:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(outputDirField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseOutputButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Nome cartella risultati:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(folderNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        formPanel.add(appendCheck, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(splitButton, BorderLayout.PAGE_END);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
