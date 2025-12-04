package olivieri.alex.tab;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Function;

public final class TabFileChooser {
    private TabFileChooser() {
    }

    public static void browseForDirectory(JFrame parent, JTextField directoryField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            directoryField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public static void browseForPdfOrDirectory(JFrame parent, JTextField inputField, JTextField outputField,
            Function<Path, String> defaultNameSupplier) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setAcceptAllFileFilterUsed(true);
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Path selected = chooser.getSelectedFile().toPath();
            inputField.setText(selected.toAbsolutePath().toString());
            if (Files.isRegularFile(selected) && outputField.getText().trim().isEmpty()) {
                outputField.setText(defaultNameSupplier.apply(selected));
            } else if (Files.isDirectory(selected)) {
                outputField.setText("");
            }
        }
    }

    public static void browseForPdfFile(JFrame parent, JTextField inputField, JTextField outputField,
            Function<Path, String> defaultNameSupplier) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(true);
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Path selected = chooser.getSelectedFile().toPath();
            inputField.setText(selected.toAbsolutePath().toString());
            if (outputField.getText().trim().isEmpty()) {
                outputField.setText(defaultNameSupplier.apply(selected));
            }
        }
    }

    public static void browseForCsv(JFrame parent, JTextField csvField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV", "csv"));
        chooser.setAcceptAllFileFilterUsed(true);
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            csvField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public static void browseForExcelOutput(JFrame parent, JTextField excelField, String suggestedName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        if (suggestedName != null && !suggestedName.trim().isEmpty()) {
            chooser.setSelectedFile(new File(suggestedName.trim()));
        }
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Path selected = chooser.getSelectedFile().toPath();
            String filename = selected.getFileName().toString();
            if (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
                selected = selected.resolveSibling(filename + ".xlsx");
            }
            excelField.setText(selected.toAbsolutePath().toString());
        }
    }

    public static void browseForOutputPdf(JFrame parent, JTextField outputField, String suggestedName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PDF", "pdf"));
        if (suggestedName != null && !suggestedName.trim().isEmpty()) {
            chooser.setSelectedFile(new File(suggestedName.trim()));
        }
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Path selected = chooser.getSelectedFile().toPath();
            String filename = selected.getFileName().toString();
            if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                selected = selected.resolveSibling(filename + ".pdf");
            }
            outputField.setText(selected.toAbsolutePath().toString());
        }
    }

    public static void browseForWordOutput(JFrame parent, JTextField outputField, String suggestedName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Word (*.docx)", "docx"));
        if (suggestedName != null && !suggestedName.trim().isEmpty()) {
            chooser.setSelectedFile(new File(suggestedName.trim()));
        }
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Path selected = chooser.getSelectedFile().toPath();
            String filename = selected.getFileName().toString();
            if (!filename.toLowerCase(Locale.ROOT).endsWith(".docx")) {
                selected = selected.resolveSibling(filename + ".docx");
            }
            outputField.setText(selected.toAbsolutePath().toString());
        }
    }

    public static void browseForTextOutput(JFrame parent, JTextField outputField, String suggestedName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV o TXT", "csv", "txt"));
        if (suggestedName != null && !suggestedName.trim().isEmpty()) {
            chooser.setSelectedFile(new File(suggestedName.trim()));
        }
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Path selected = chooser.getSelectedFile().toPath();
            String filename = selected.getFileName().toString();
            String lower = filename.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".csv") && !lower.endsWith(".txt")) {
                selected = selected.resolveSibling(filename + ".csv");
            }
            outputField.setText(selected.toAbsolutePath().toString());
        }
    }
}
