package olivieri.alex.tab;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import olivieri.alex.App;

public final class AlternatingMixTab {
    private AlternatingMixTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField firstPdfField = new JTextField(25);
        JButton firstBrowseButton = new JButton("Sfoglia primo PDF");
        JTextField secondPdfField = new JTextField(25);
        JButton secondBrowseButton = new JButton("Sfoglia secondo PDF");
        JTextField outputField = new JTextField(25);
        JButton outputBrowseButton = new JButton("Salva come...");

        JSpinner firstChunkSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        ((JSpinner.DefaultEditor) firstChunkSpinner.getEditor()).getTextField().setColumns(4);

        JSpinner secondChunkSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        ((JSpinner.DefaultEditor) secondChunkSpinner.getEditor()).getTextField().setColumns(4);

        JButton mixButton = new JButton("Crea miscelazione");

        firstBrowseButton.addActionListener(event -> TabFileChooser.browseForPdfFile(parent, firstPdfField,
                outputField, controller::buildDefaultAlternatingMixName));
        secondBrowseButton.addActionListener(event -> TabFileChooser.browseForPdfFile(parent, secondPdfField,
                outputField, controller::buildDefaultAlternatingMixName));
        outputBrowseButton.addActionListener(event -> {
            String suggestion = controller.buildDefaultAlternatingMixName(firstPdfField.getText());
            TabFileChooser.browseForOutputPdf(parent, outputField, suggestion);
        });

        mixButton.addActionListener(event -> controller.startAlternatingMix(parent, firstPdfField.getText(),
                secondPdfField.getText(), ((Number) firstChunkSpinner.getValue()).intValue(),
                ((Number) secondChunkSpinner.getValue()).intValue(), outputField.getText(), mixButton));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Primo PDF:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(firstPdfField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(firstBrowseButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Secondo PDF:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(secondPdfField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(secondBrowseButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Pagine primo per blocco:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(firstChunkSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Pagine secondo per blocco:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(secondChunkSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("File di output:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(outputField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(outputBrowseButton, gbc);

        JLabel descriptionLabel = new JLabel(
                "<html>Alterna blocchi costanti di pagine tra i due PDF selezionati per creare un nuovo file.</html>");

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(descriptionLabel, BorderLayout.NORTH);
        container.add(formPanel, BorderLayout.CENTER);
        container.add(mixButton, BorderLayout.PAGE_END);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
