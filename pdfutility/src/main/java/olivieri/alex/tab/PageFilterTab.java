package olivieri.alex.tab;

import static olivieri.alex.tab.TabFileChooser.browseForPdfFile;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import olivieri.alex.App;
import olivieri.alex.util.PdfPageFilter;

public final class PageFilterTab {
    private PageFilterTab() {
    }

    public static JPanel create(App controller, JFrame parent) {
        JTextField inputField = new JTextField(25);
        JButton browseButton = new JButton("Seleziona PDF");
        JRadioButton removeOdd = new JRadioButton("Rimuovi pagine dispari", true);
        JRadioButton removeEven = new JRadioButton("Rimuovi pagine pari");
        ButtonGroup group = new ButtonGroup();
        group.add(removeOdd);
        group.add(removeEven);
        JTextField outputField = new JTextField("", 20);
        JButton processButton = new JButton("Filtra pagine");

        browseButton.addActionListener(
                event -> browseForPdfFile(parent, inputField, outputField,
                        path -> controller.buildDefaultFilteredName(path,
                                removeOdd.isSelected() ? PdfPageFilter.Mode.ODD : PdfPageFilter.Mode.EVEN)));

        processButton.addActionListener(event -> controller.startRemovePages(parent, inputField.getText(),
                removeOdd.isSelected() ? PdfPageFilter.Mode.ODD : PdfPageFilter.Mode.EVEN, outputField.getText(),
                processButton));

        removeOdd.addActionListener(event -> controller.updateFilteredOutputSuggestion(inputField, outputField,
                removeOdd.isSelected() ? PdfPageFilter.Mode.ODD : PdfPageFilter.Mode.EVEN));
        removeEven.addActionListener(event -> controller.updateFilteredOutputSuggestion(inputField, outputField,
                removeOdd.isSelected() ? PdfPageFilter.Mode.ODD : PdfPageFilter.Mode.EVEN));

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
        gbc.gridwidth = 3;
        formPanel.add(removeOdd, gbc);

        gbc.gridy = 2;
        formPanel.add(removeEven, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        formPanel.add(new JLabel("File di output:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        formPanel.add(outputField, gbc);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.add(formPanel, BorderLayout.CENTER);
        container.add(processButton, BorderLayout.PAGE_END);
        container.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return container;
    }
}
