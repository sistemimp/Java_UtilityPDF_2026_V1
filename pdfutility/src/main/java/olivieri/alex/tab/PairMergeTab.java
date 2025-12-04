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

public final class PairMergeTab {
    private PairMergeTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField firstDirField = new JTextField(25);
        JButton browseFirstButton = new JButton("Cartella 1");
        JTextField secondDirField = new JTextField(25);
        JButton browseSecondButton = new JButton("Cartella 2");
        JButton mergeButton = new JButton("Unisci corrispondenze");

        browseFirstButton.addActionListener(event -> browseForDirectory(parent, firstDirField));
        browseSecondButton.addActionListener(event -> browseForDirectory(parent, secondDirField));
        mergeButton.addActionListener(
                event -> controller.startPairMerge(parent, firstDirField.getText(), secondDirField.getText(),
                        mergeButton));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Cartella 1:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(firstDirField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseFirstButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Cartella 2:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(secondDirField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseSecondButton, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(mergeButton, BorderLayout.PAGE_END);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
