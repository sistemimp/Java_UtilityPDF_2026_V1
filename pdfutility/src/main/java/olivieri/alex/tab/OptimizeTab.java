package olivieri.alex.tab;

import static olivieri.alex.tab.TabFileChooser.browseForPdfOrDirectory;

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

public final class OptimizeTab {
    private OptimizeTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField inputField = new JTextField(25);
        JButton browseInputButton = new JButton("Seleziona PDF/Cartella");
        JTextField outputField = new JTextField("", 20);
        JButton optimizeButton = new JButton("Ottimizza");

        browseInputButton.addActionListener(event -> browseForPdfOrDirectory(parent, inputField, outputField,
                controller::buildDefaultOptimizedName));
        optimizeButton.addActionListener(
                event -> controller.startOptimization(parent, inputField.getText(), outputField.getText(),
                        optimizeButton));

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
        formPanel.add(new JLabel("File ottimizzato:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(outputField, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(optimizeButton, BorderLayout.PAGE_END);
        JLabel descriptionLabel = new JLabel("Riduce dimensione e ottimizza le risorse del PDF mantenendo la qualità.");
        descriptionLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));
        container.add(descriptionLabel, BorderLayout.NORTH);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
