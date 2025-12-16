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

public final class MergeTab {
    private MergeTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField directoryField = new JTextField(25);
        JButton browseButton = new JButton("Sfoglia cartella");
        JTextField outputField = new JTextField("#_merge.pdf", 20);
        JButton mergeButton = new JButton("Unisci");

        browseButton.addActionListener(event -> browseForDirectory(parent, directoryField));
        mergeButton.addActionListener(
                event -> controller.startMerge(parent, directoryField.getText(), outputField.getText(), mergeButton));

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
        formPanel.add(browseButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("File di output:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(outputField, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(mergeButton, BorderLayout.PAGE_END);
        JLabel descriptionLabel = new JLabel("Combina tutti i PDF in una cartella in un unico documento.");
        descriptionLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));
        container.add(descriptionLabel, BorderLayout.NORTH);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
