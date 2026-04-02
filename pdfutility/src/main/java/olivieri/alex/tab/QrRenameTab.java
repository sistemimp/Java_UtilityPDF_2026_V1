package olivieri.alex.tab;

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

public final class QrRenameTab {
    private QrRenameTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField inputField = new JTextField(25);
        JButton browseInputButton = new JButton("Cartella input");
        JTextField outputField = new JTextField(25);
        JButton browseOutputButton = new JButton("Cartella output");
        JButton renameButton = new JButton("Rinomina da QR");

        browseInputButton.addActionListener(event -> browseForDirectory(parent, inputField));
        browseOutputButton.addActionListener(event -> browseForDirectory(parent, outputField));
        renameButton.addActionListener(event -> controller.startQrRename(parent, inputField.getText(),
                outputField.getText(), renameButton));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Cartella input:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(inputField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseInputButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Cartella output:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(outputField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseOutputButton, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        JLabel descriptionLabel = new JLabel(
                "Rinomina e copia i PDF usando il QR code trovato nella prima pagina.");
        descriptionLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));
        container.add(descriptionLabel, BorderLayout.NORTH);
        container.add(formPanel, BorderLayout.CENTER);
        container.add(renameButton, BorderLayout.PAGE_END);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
