package olivieri.alex.tab;

import static olivieri.alex.tab.TabFileChooser.browseForDirectory;
import static olivieri.alex.tab.TabFileChooser.browseForWordOutput;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import olivieri.alex.App;

public final class PdfToWordTab {
    private PdfToWordTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField inputField = new JTextField(25);
        JButton browseInputButton = new JButton("Seleziona PDF/Cartella");
        JTextField outputField = new JTextField(25);
        JButton browseOutputButton = new JButton("Output");
        JButton convertButton = new JButton("Crea documento Word");

        browseInputButton.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setAcceptAllFileFilterUsed(true);
            chooser.setFileFilter(new FileNameExtensionFilter("PDF o cartelle", "pdf"));
            if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                Path selected = chooser.getSelectedFile().toPath();
                inputField.setText(selected.toAbsolutePath().toString());
                if (Files.isDirectory(selected)) {
                    if (outputField.getText().trim().isEmpty()) {
                        outputField.setText(controller.buildDefaultWordDirectoryName(selected));
                    }
                } else if (outputField.getText().trim().isEmpty()) {
                    outputField.setText(controller.buildDefaultWordName(selected));
                }
            }
        });

        browseOutputButton.addActionListener(event -> {
            String inputText = inputField.getText().trim();
            Path inputPath = null;
            boolean directorySelected = false;
            if (!inputText.isEmpty()) {
                try {
                    inputPath = Paths.get(inputText);
                    directorySelected = Files.isDirectory(inputPath);
                } catch (Exception ignored) {
                    directorySelected = false;
                }
            }
            if (directorySelected) {
                browseForDirectory(parent, outputField);
            } else {
                String suggestion = outputField.getText().trim();
                if (suggestion.isEmpty()) {
                    suggestion = controller.buildDefaultWordName(inputField.getText());
                }
                browseForWordOutput(parent, outputField, suggestion);
            }
        });

        convertButton.addActionListener(
                event -> controller.startPdfToWordConversion(parent, inputField.getText(), outputField.getText(),
                        convertButton));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Sorgente (PDF o cartella):"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(inputField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseInputButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Output (file o cartella):"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(outputField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseOutputButton, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(convertButton, BorderLayout.PAGE_END);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
