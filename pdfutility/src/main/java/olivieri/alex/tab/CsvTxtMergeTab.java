package olivieri.alex.tab;

import static olivieri.alex.tab.TabFileChooser.browseForTextOutput;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import olivieri.alex.App;

public final class CsvTxtMergeTab {
    private CsvTxtMergeTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        DefaultListModel<String> fileListModel = new DefaultListModel<>();
        JList<String> fileList = new JList<>(fileListModel);
        fileList.setVisibleRowCount(8);
        JScrollPane scrollPane = new JScrollPane(fileList);
        JButton addButton = new JButton("Aggiungi file");
        JButton removeButton = new JButton("Rimuovi selezionati");
        JTextField outputField = new JTextField(25);
        JButton browseOutputButton = new JButton("File di output");
        JButton mergeButton = new JButton("Unisci");

        final String[] lastSuggestion = { "" };

        addButton.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(true);
            chooser.setFileFilter(new FileNameExtensionFilter("CSV o TXT", "csv", "txt"));

            chooser.setAcceptAllFileFilterUsed(true);
            if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                File[] selected = chooser.getSelectedFiles();
                if (selected != null) {
                    for (File file : selected) {
                        if (file != null) {
                            fileListModel.addElement(file.getAbsolutePath());
                        }
                    }
                    updateCsvTxtSuggestion(controller, fileListModel, outputField, lastSuggestion);
                }
            }
        });

        removeButton.addActionListener(event -> {
            List<String> selected = fileList.getSelectedValuesList();
            if (selected != null && !selected.isEmpty()) {
                for (String value : selected) {
                    fileListModel.removeElement(value);
                }
                updateCsvTxtSuggestion(controller, fileListModel, outputField, lastSuggestion);
            }
        });

        browseOutputButton.addActionListener(event -> {
            String suggestion = "";
            if (!fileListModel.isEmpty()) {
                suggestion = controller.buildDefaultMergedTextName(Paths.get(fileListModel.getElementAt(0)));
            }
            browseForTextOutput(parent, outputField, suggestion);
        });

        mergeButton.addActionListener(
                event -> controller.startCsvTxtMerge(parent, fileListModel, outputField.getText(), mergeButton,
                        lastSuggestion));

        JPanel listPanel = new JPanel(new BorderLayout(5, 5));
        listPanel.add(new JLabel("File da unire:"), BorderLayout.PAGE_START);
        listPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));
        actionsPanel.add(addButton);
        actionsPanel.add(removeButton);

        JPanel outputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        outputPanel.add(new JLabel("File di output:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        outputPanel.add(outputField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        outputPanel.add(browseOutputButton, gbc);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(outputPanel, BorderLayout.CENTER);
        bottomPanel.add(mergeButton, BorderLayout.PAGE_END);

        JLabel descriptionLabel = new JLabel(
                "Unisce più file CSV o TXT in un unico file consolidato di testo.");
        descriptionLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(listPanel, BorderLayout.CENTER);
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(descriptionLabel, BorderLayout.NORTH);
        northPanel.add(actionsPanel, BorderLayout.SOUTH);
        container.add(northPanel, BorderLayout.PAGE_START);
        container.add(bottomPanel, BorderLayout.PAGE_END);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }

    private static void updateCsvTxtSuggestion(App controller, DefaultListModel<String> model,
            JTextField outputField, String[] lastSuggestion) {
        if (model == null || outputField == null || lastSuggestion == null || lastSuggestion.length == 0) {
            return;
        }
        if (model.isEmpty()) {
            lastSuggestion[0] = "";
            return;
        }
        String candidatePath = model.getElementAt(0);
        if (candidatePath == null || candidatePath.trim().isEmpty()) {
            return;
        }
        String suggestion = controller.buildDefaultMergedTextName(Paths.get(candidatePath));
        if (suggestion.isEmpty()) {
            return;
        }
        String current = outputField.getText().trim();
        if (current.isEmpty() || current.equals(lastSuggestion[0])) {
            outputField.setText(suggestion);
            lastSuggestion[0] = suggestion;
        }
    }
}
