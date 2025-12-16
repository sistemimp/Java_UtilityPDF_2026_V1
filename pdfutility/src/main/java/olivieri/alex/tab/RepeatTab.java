package olivieri.alex.tab;

import static olivieri.alex.tab.TabFileChooser.browseForPdfFile;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import olivieri.alex.App;

public final class RepeatTab {
    private RepeatTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField inputField = new JTextField(25);
        JButton browseButton = new JButton("Seleziona PDF");
        SpinnerNumberModel model = new SpinnerNumberModel(2, 1, 500, 1);
        JSpinner repetitionsSpinner = new JSpinner(model);
        JTextField outputField = new JTextField("", 20);
        JButton processButton = new JButton("Ripeti PDF");

        browseButton.addActionListener(event -> browseForPdfFile(parent, inputField, outputField,
                path -> controller.buildDefaultRepeatedName(path,
                        ((Number) repetitionsSpinner.getValue()).intValue())));

        repetitionsSpinner.addChangeListener(event -> {
            if (outputField.getText().trim().isEmpty()) {
                String input = inputField.getText().trim();
                if (!input.isEmpty()) {
                    try {
                        Path path = Paths.get(input);
                        outputField.setText(
                                controller.buildDefaultRepeatedName(path,
                                        ((Number) repetitionsSpinner.getValue()).intValue()));
                    } catch (Exception ignored) {
                        // Ignore invalid path until user fixes it.
                    }
                }
            }
        });

        processButton.addActionListener(event -> controller.startRepeatPdf(parent, inputField.getText(),
                ((Number) repetitionsSpinner.getValue()).intValue(), outputField.getText(), processButton));

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
        formPanel.add(new JLabel("Ripetizioni:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(repetitionsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("File di output:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(outputField, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(processButton, BorderLayout.PAGE_END);
        JLabel descriptionLabel = new JLabel("Duplica tutto il PDF per il numero di ripetizioni desiderato.");
        descriptionLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));
        container.add(descriptionLabel, BorderLayout.NORTH);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
