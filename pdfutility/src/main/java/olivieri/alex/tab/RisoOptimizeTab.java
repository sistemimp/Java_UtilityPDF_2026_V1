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
import javax.swing.JTextField;

import olivieri.alex.App;

public final class RisoOptimizeTab {
    private RisoOptimizeTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField inputField = new JTextField(25);
        JButton browseInputButton = new JButton("Seleziona PDF");
        JTextField recordIdField = new JTextField(20);
        JTextField outputField = new JTextField("", 20);
        JButton browseOutputButton = new JButton("Scegli output");
        JButton optimizeButton = new JButton("Ottimizza per Riso GL9730");

        browseInputButton.addActionListener(
                event -> browseForPdfFile(parent, inputField, outputField, controller::buildDefaultRisoOptimizedName));

        browseOutputButton.addActionListener(event -> {
            String suggestion = controller.buildDefaultRisoOptimizedName(inputField.getText());
            browseForOutputPdf(parent, outputField, suggestion);
        });

        optimizeButton.addActionListener(event -> controller.startRisoOptimization(parent, inputField.getText(),
                outputField.getText(), recordIdField.getText(), optimizeButton));

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
        formPanel.add(browseInputButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Record ID (opzionale):"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(recordIdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("File PDF/A:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(outputField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseOutputButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        JLabel infoLabel = new JLabel("Converte il PDF in PDF/A-3B con intento colore sRGB per la Riso GL9730.");
        formPanel.add(infoLabel, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(optimizeButton, BorderLayout.PAGE_END);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
