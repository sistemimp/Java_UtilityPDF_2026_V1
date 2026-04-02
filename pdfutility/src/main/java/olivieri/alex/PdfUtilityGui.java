package olivieri.alex;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import olivieri.alex.tab.*;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public final class PdfUtilityGui {
    private final App controller;

    public PdfUtilityGui(App controller) {
        this.controller = controller;
    }

    public static void setSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
            // Continue with default look and feel if setting the system one fails.
        }
    }

    public void createAndShowGui() {
        JFrame frame = new JFrame("Utility PDF");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Unione PDF", MergeTab.create(controller, frame));
        tabbedPane.addTab("Ottimizzazione PDF", OptimizeTab.create(controller, frame));
        tabbedPane.addTab("Riso GL9730", RisoOptimizeTab.create(controller, frame));
        tabbedPane.addTab("Riso ComColor GD9630", RisoComcolorGd9630OptimizeTab.create(controller, frame));
        tabbedPane.addTab("Pagine Bianche", BlankPagesTab.create(controller, frame));
        tabbedPane.addTab("Pagine Bianche dopo testo", ConditionalBlankTab.create(controller, frame));
        tabbedPane.addTab("Ripeti PDF", RepeatTab.create(controller, frame));
        tabbedPane.addTab("Miscelazione alternata", AlternatingMixTab.create(controller, frame));
        tabbedPane.addTab("Filtro pagine", PageFilterTab.create(controller, frame));
        tabbedPane.addTab("Unisci per nome", PairMergeTab.create(controller, frame));
        tabbedPane.addTab("Rinomina da CSV", CsvRenameTab.create(controller, frame));
        tabbedPane.addTab("Rinomina QR", QrRenameTab.create(controller, frame));
        tabbedPane.addTab("CSV in Excel", CsvToExcelTab.create(controller, frame));
        tabbedPane.addTab("Unisci CSV/TXT", CsvTxtMergeTab.create(controller, frame));
        tabbedPane.addTab("Timbro cartella", FolderStampTab.create(controller, frame));
        tabbedPane.addTab("Timbro per parola", KeywordStampTab.create(controller, frame));
        // tabbedPane.addTab("OCR PDF", createOcrPanel(frame));
        tabbedPane.addTab("Rimuovi pagine", RemovePagesTab.create(controller, frame));
        tabbedPane.addTab("Split per stringa", MarkerSplitTab.create(controller, frame));
        tabbedPane.addTab("PDF in Word", PdfToWordTab.create(controller, frame));
        installTabFontHighlight(tabbedPane);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.add(createToolbar(frame), BorderLayout.NORTH);
        contentPanel.add(tabbedPane, BorderLayout.CENTER);

        frame.setContentPane(contentPanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void installTabFontHighlight(JTabbedPane tabbedPane) {
        Font baseFont = UIManager.getFont("TabbedPane.font");
        if (baseFont == null) {
            baseFont = tabbedPane.getFont();
        }
        if (baseFont == null) {
            baseFont = new JLabel().getFont();
        }
        Font plainFont = baseFont.deriveFont(Font.PLAIN);
        Font boldFont = baseFont.deriveFont(Font.BOLD);
        List<JLabel> labels = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            String title = tabbedPane.getTitleAt(i);
            JLabel label = new JLabel(title);
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 4, 2, 4));
            label.setFont(plainFont);
            tabbedPane.setTabComponentAt(i, label);
            labels.add(label);
        }
        Runnable updater = () -> {
            int selected = tabbedPane.getSelectedIndex();
            for (int i = 0; i < labels.size(); i++) {
                labels.get(i).setFont(i == selected ? boldFont : plainFont);
            }
        };
        tabbedPane.addChangeListener(event -> updater.run());
        updater.run();
    }

    private JPanel createToolbar(JFrame parent) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JLabel title = new JLabel("Utility PDF - monitoraggio ISO 9001");
        JButton openLogButton = new JButton("Apri log audit");
        openLogButton.setToolTipText("Apre il file dei log utilizzato per la conformit… ISO 9001.");
        openLogButton.addActionListener(event -> controller.openAuditLog(parent));

        panel.add(title, BorderLayout.WEST);
        panel.add(openLogButton, BorderLayout.EAST);
        return panel;
    }
}
