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
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import olivieri.alex.App;

public final class FolderStampTab {
    private FolderStampTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField directoryField = new JTextField(25);
        JButton browseDirButton = new JButton("Cartella PDF");
        JTextField stampTextField = new JTextField(25);
        JSpinner xSpinner = new JSpinner(new SpinnerNumberModel(0.0, -10000.0, 10000.0, 1.0));
        JSpinner ySpinner = new JSpinner(new SpinnerNumberModel(0.0, -10000.0, 10000.0, 1.0));
        JButton stampButton = new JButton("Applica timbro");

        browseDirButton.addActionListener(event -> browseForDirectory(parent, directoryField));
        stampButton.addActionListener(event -> controller.startFolderStamp(parent, directoryField.getText(),
                stampTextField.getText(), xSpinner, ySpinner, stampButton));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Cartella PDF:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(directoryField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        formPanel.add(browseDirButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Testo timbro:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        formPanel.add(stampTextField, gbc);
        gbc.gridwidth = 1;
        gbc.weightx = 0;

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Posizione X (pt):"), gbc);

        gbc.gridx = 1;
        formPanel.add(xSpinner, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("Posizione Y (pt):"), gbc);

        gbc.gridx = 3;
        formPanel.add(ySpinner, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(stampButton, BorderLayout.PAGE_END);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
