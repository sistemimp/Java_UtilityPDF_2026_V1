package olivieri.alex;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.itextpdf.kernel.pdf.CompressionConstants;
import com.itextpdf.kernel.pdf.PdfVersion;
import com.itextpdf.kernel.pdf.WriterProperties;

import olivieri.alex.quality.AuditLogger;
import olivieri.alex.util.CsvToExcelConverter;
import olivieri.alex.util.CsvTxtMerger;
import olivieri.alex.util.PdfBlankPageInserter;
import olivieri.alex.util.PdfConditionalBlankPageInserter;
import olivieri.alex.util.PdfCsvRenamer;
import olivieri.alex.util.PdfFolderStamper;
import olivieri.alex.util.PdfKeywordStamper;
import olivieri.alex.util.PdfMarkerSplitter;
import olivieri.alex.util.PdfMergeService;
import olivieri.alex.util.PdfOptimizer;
import olivieri.alex.util.PdfPageFilter;
import olivieri.alex.util.PdfPairMerger;
import olivieri.alex.util.PdfRepeater;
import olivieri.alex.util.PdfStringPageRemover;
import olivieri.alex.util.PdfToWordConverter;
import olivieri.alex.util.RisoGl9730Optimizer;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class App {
    private final PdfMergeService mergeService = new PdfMergeService();
    private final PdfOptimizer optimizer = new PdfOptimizer();
    private final RisoGl9730Optimizer risoOptimizer = new RisoGl9730Optimizer();
    private final PdfBlankPageInserter blankPageInserter = new PdfBlankPageInserter();
    private final PdfConditionalBlankPageInserter conditionalBlankPageInserter = new PdfConditionalBlankPageInserter();
    private final PdfRepeater repeater = new PdfRepeater();
    private final PdfPageFilter pageFilter = new PdfPageFilter();
    private final PdfPairMerger pairMerger = new PdfPairMerger();
    private final PdfCsvRenamer csvRenamer = new PdfCsvRenamer();
    private final PdfFolderStamper folderStamper = new PdfFolderStamper();
    private final PdfKeywordStamper keywordStamper = new PdfKeywordStamper();
    private final PdfMarkerSplitter markerSplitter = new PdfMarkerSplitter();
    private final PdfStringPageRemover stringPageRemover = new PdfStringPageRemover();
    private final PdfToWordConverter pdfToWordConverter = new PdfToWordConverter();
    private final CsvToExcelConverter csvToExcelConverter = new CsvToExcelConverter();
    private final CsvTxtMerger csvTxtMerger = new CsvTxtMerger();

    public static final WriterProperties writerProperties = new WriterProperties().setPdfVersion(PdfVersion.PDF_1_7)
            .useSmartMode().setFullCompressionMode(true).setCompressionLevel(CompressionConstants.BEST_COMPRESSION);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PdfUtilityGui.setSystemLookAndFeel();
            new App().showGui();
        });
    }

    void showGui() {
        new PdfUtilityGui(this).createAndShowGui();
    }

    public void updateFilteredOutputSuggestion(JTextField inputField, JTextField outputField, PdfPageFilter.Mode mode) {
        if (!outputField.getText().trim().isEmpty()) {
            return;
        }
        String input = inputField.getText().trim();
        if (input.isEmpty()) {
            return;
        }
        try {
            Path path = Paths.get(input);
            outputField.setText(buildDefaultFilteredName(path, mode));
        } catch (Exception ignored) {
            // ignore invalid path
        }
    }

    public void updateKeywordStampOutputSuggestion(JTextField inputField, JTextField outputField, JTextField keywordField) {
        if (!outputField.getText().trim().isEmpty()) {
            return;
        }
        String input = inputField.getText().trim();
        if (input.isEmpty()) {
            return;
        }
        try {
            Path path = Paths.get(input);
            outputField.setText(buildDefaultKeywordStampName(path, keywordField.getText()));
        } catch (Exception ignored) {
            // ignore invalid path
        }
    }

    public void startMerge(JFrame parent, String directoryText, String outputText, JButton mergeButton) {
        String trimmedDirectory = directoryText == null ? "" : directoryText.trim();
        if (trimmedDirectory.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Seleziona una cartella che contiene i PDF.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path directory;
        try {
            directory = Paths.get(trimmedDirectory);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso cartella non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Path outputPath = resolvePdfOutput(directory.toAbsolutePath(), outputText, "merged.pdf");

        mergeButton.setEnabled(false);

        Path mergeDirectory = directory.toAbsolutePath();
        Path mergeOutput = outputPath;
        String mergeDetails = "source=" + mergeDirectory;

        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return mergeService.mergeDirectory(directory, outputPath);
            }

            @Override
            protected void done() {
                try {
                    Path result = get();
                    JOptionPane.showMessageDialog(parent, "Unione completata!\nFile creato: " + result.toString(),
                            "Successo", JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_MERGE", mergeDetails, result);
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_MERGE", mergeDetails, mergeOutput, ex, "Errore durante l'unione: ");
                } finally {
                    mergeButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startInsertBlankPages(JFrame parent, String inputText, String outputText, JButton processButton) {
        String trimmedInput = inputText == null ? "" : inputText.trim();
        if (trimmedInput.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Seleziona un file PDF da elaborare.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path inputPath;
        try {
            inputPath = Paths.get(trimmedInput);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.isRegularFile(inputPath)) {
            JOptionPane.showMessageDialog(parent, "Il file specificato non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultBlankPagesName(inputPath));

        processButton.setEnabled(false);

        Path blankInput = inputPath.toAbsolutePath();
        Path blankOutput = outputPath;
        String blankDetails = "input=" + blankInput;

        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return blankPageInserter.insertBlankPages(inputPath, outputPath);
            }

            @Override
            protected void done() {
                try {
                    Path result = get();
                    JOptionPane.showMessageDialog(parent, "Operazione completata!\nFile creato: " + result.toString(),
                            "Successo", JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_INSERT_BLANK", blankDetails, result);
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_INSERT_BLANK", blankDetails, blankOutput, ex,
                            "Errore durante l'elaborazione: ");
                } finally {
                    processButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startInsertBlankAfterPhrase(JFrame parent, String inputText, String phraseText, boolean caseSensitive,
            String outputText, JButton processButton) {
        String trimmedInput = inputText == null ? "" : inputText.trim();
        if (trimmedInput.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Seleziona un file PDF da elaborare.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String trimmedPhrase = phraseText == null ? "" : phraseText.trim();
        if (trimmedPhrase.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Inserisci la parola o frase da cercare.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path inputPath;
        try {
            inputPath = Paths.get(trimmedInput);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.isRegularFile(inputPath)) {
            JOptionPane.showMessageDialog(parent, "Il file specificato non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultBlankAfterPhraseName(inputPath, trimmedPhrase));

        processButton.setEnabled(false);

        Path phraseInput = inputPath.toAbsolutePath();
        Path phraseOutput = outputPath;
        String phraseDetails = "input=" + phraseInput + ",phrase=" + trimmedPhrase + ",caseSensitive=" + caseSensitive;

        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return conditionalBlankPageInserter.insertAfterPhrase(inputPath, outputPath, trimmedPhrase,
                        caseSensitive);
            }

            @Override
            protected void done() {
                try {
                    Path result = get();
                    JOptionPane.showMessageDialog(parent, "Operazione completata!\nFile creato: " + result.toString(),
                            "Successo", JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_INSERT_AFTER_PHRASE", phraseDetails, result);
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_INSERT_AFTER_PHRASE", phraseDetails, phraseOutput, ex,
                            "Errore durante l'elaborazione: ");
                } finally {
                    processButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startRepeatPdf(JFrame parent, String inputText, int repetitions, String outputText,
            JButton processButton) {
        String trimmedInput = inputText == null ? "" : inputText.trim();
        if (trimmedInput.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Seleziona un file PDF da ripetere.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (repetitions < 1) {
            JOptionPane.showMessageDialog(parent, "Le ripetizioni devono essere almeno 1.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path inputPath;
        try {
            inputPath = Paths.get(trimmedInput);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.isRegularFile(inputPath)) {
            JOptionPane.showMessageDialog(parent, "Il file specificato non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultRepeatedName(inputPath, repetitions));

        processButton.setEnabled(false);

        Path repeatInput = inputPath.toAbsolutePath();
        Path repeatOutput = outputPath;
        String repeatDetails = "input=" + repeatInput + ",repetitions=" + repetitions;

        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return repeater.repeat(inputPath, outputPath, repetitions);
            }

            @Override
            protected void done() {
                try {
                    Path result = get();
                    JOptionPane.showMessageDialog(parent, "Operazione completata!\nFile creato: " + result.toString(),
                            "Successo", JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_REPEAT", repeatDetails, result);
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_REPEAT", repeatDetails, repeatOutput, ex,
                            "Errore durante l'elaborazione: ");
                } finally {
                    processButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startRemovePages(JFrame parent, String inputText, PdfPageFilter.Mode mode, String outputText,
            JButton processButton) {
        String trimmedInput = inputText == null ? "" : inputText.trim();
        if (trimmedInput.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Seleziona un file PDF da filtrare.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path inputPath;
        try {
            inputPath = Paths.get(trimmedInput);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.isRegularFile(inputPath)) {
            JOptionPane.showMessageDialog(parent, "Il file specificato non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultFilteredName(inputPath, mode));

        processButton.setEnabled(false);

        Path filterInput = inputPath.toAbsolutePath();
        Path filterOutput = outputPath;
        String filterDetails = "input=" + filterInput + ",mode=" + mode;

        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return pageFilter.removePages(inputPath, outputPath, mode);
            }

            @Override
            protected void done() {
                try {
                    Path result = get();
                    JOptionPane.showMessageDialog(parent, "Operazione completata!\nFile creato: " + result.toString(),
                            "Successo", JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_PAGE_FILTER", filterDetails, result);
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_PAGE_FILTER", filterDetails, filterOutput, ex,
                            "Errore durante l'elaborazione: ");
                } finally {
                    processButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startPairMerge(JFrame parent, String firstDirText, String secondDirText, JButton mergeButton) {
        String trimmedFirst = firstDirText == null ? "" : firstDirText.trim();
        String trimmedSecond = secondDirText == null ? "" : secondDirText.trim();

        if (trimmedFirst.isEmpty() || trimmedSecond.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Seleziona entrambe le cartelle.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path firstDir;
        Path secondDir;
        try {
            firstDir = Paths.get(trimmedFirst);
            secondDir = Paths.get(trimmedSecond);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso cartella non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.isDirectory(firstDir)) {
            JOptionPane.showMessageDialog(parent, "La prima cartella non esiste.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!Files.isDirectory(secondDir)) {
            JOptionPane.showMessageDialog(parent, "La seconda cartella non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        mergeButton.setEnabled(false);

        Path firstAbsolute = firstDir.toAbsolutePath();
        Path secondAbsolute = secondDir.toAbsolutePath();
        String pairDetails = "first=" + firstAbsolute + ",second=" + secondAbsolute;

        SwingWorker<PdfPairMerger.Result, Void> worker = new SwingWorker<>() {
            @Override
            protected PdfPairMerger.Result doInBackground() throws Exception {
                return pairMerger.mergeMatchingPairs(firstDir, secondDir);
            }

            @Override
            protected void done() {
                try {
                    PdfPairMerger.Result result = get();
                    StringBuilder message = new StringBuilder().append("Unione completata!\nFile generati: ")
                            .append(result.getMergedCount()).append("\nCartella risultante: ")
                            .append(result.getOutputDirectory());
                    if (result.hasMissingReport()) {
                        message.append("\nFile mancanti elencati in: ").append(result.getMissingReport());
                    }
                    JOptionPane.showMessageDialog(parent, message.toString(), "Successo",
                            JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_PAIR_MERGE", pairDetails, result.getOutputDirectory());
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_PAIR_MERGE", pairDetails, firstAbsolute, ex,
                            "Errore durante l'unione: ");
                } finally {
                    mergeButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startCsvRename(JFrame parent, String directoryText, String csvText, JButton renameButton) {
        String trimmedDir = directoryText == null ? "" : directoryText.trim();
        String trimmedCsv = csvText == null ? "" : csvText.trim();
        if (trimmedDir.isEmpty() || trimmedCsv.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Seleziona la cartella PDF e il file CSV.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path directory;
        Path csvFile;
        try {
            directory = Paths.get(trimmedDir);
            csvFile = Paths.get(trimmedCsv);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorsi non validi.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.isDirectory(directory)) {
            JOptionPane.showMessageDialog(parent, "La cartella specificata non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!Files.isRegularFile(csvFile)) {
            JOptionPane.showMessageDialog(parent, "Il file CSV specificato non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        renameButton.setEnabled(false);

        Path renameDirectory = directory.toAbsolutePath();
        Path renameCsvPath = csvFile.toAbsolutePath();
        String renameDetails = "directory=" + renameDirectory + ",csv=" + renameCsvPath;

        SwingWorker<PdfCsvRenamer.Result, Void> worker = new SwingWorker<>() {
            @Override
            protected PdfCsvRenamer.Result doInBackground() throws Exception {
                return csvRenamer.renameFromCsv(directory, csvFile);
            }

            @Override
            protected void done() {
                try {
                    PdfCsvRenamer.Result result = get();
                    StringBuilder message = new StringBuilder().append("Rinomina completata!\nFile rinominati: ")
                            .append(result.getRenamedCount());
                    if (result.hasWarnings()) {
                        message.append("\nAvvisi:");
                        for (String warning : result.getWarnings()) {
                            message.append("\n- ").append(warning);
                        }
                    }
                    JOptionPane.showMessageDialog(parent, message.toString(), "Successo",
                            JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_CSV_RENAME", renameDetails, renameDirectory);
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_CSV_RENAME", renameDetails, renameDirectory, ex,
                            "Errore durante la rinomina: ");
                } finally {
                    renameButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startCsvToExcelConversion(JFrame parent, String csvText, String excelText, JButton convertButton) {
        String trimmedCsv = csvText == null ? "" : csvText.trim();
        String trimmedExcel = excelText == null ? "" : excelText.trim();

        if (trimmedCsv.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Seleziona un file CSV.", "Attenzione", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path csvPath;
        try {
            csvPath = Paths.get(trimmedCsv);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso CSV non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.isRegularFile(csvPath)) {
            JOptionPane.showMessageDialog(parent, "Il file CSV specificato non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Path excelPath;
        try {
            excelPath = resolveExcelOutput(csvPath, trimmedExcel);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso Excel non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        convertButton.setEnabled(false);

        Path csvAbsolute = csvPath.toAbsolutePath();
        Path excelAbsolute = excelPath.toAbsolutePath();
        String conversionDetails = "csv=" + csvAbsolute + ",excel=" + excelAbsolute;

        Path targetExcel = excelPath;
        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return csvToExcelConverter.convert(csvPath, targetExcel);
            }

            @Override
            protected void done() {
                try {
                    Path result = get();
                    JOptionPane.showMessageDialog(parent,
                            "Conversione completata!\nFile creato: " + result.toAbsolutePath(), "Successo",
                            JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("CSV_TO_EXCEL", conversionDetails, result);
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "CSV_TO_EXCEL", conversionDetails, excelAbsolute, ex,
                            "Errore durante la conversione: ");
                } finally {
                    convertButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startCsvTxtMerge(JFrame parent, DefaultListModel<String> fileListModel, String outputText,
            JButton mergeButton, String[] lastSuggestion) {
        if (fileListModel == null || fileListModel.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Aggiungi almeno due file CSV o TXT da unire.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (fileListModel.getSize() < 2) {
            JOptionPane.showMessageDialog(parent, "Seleziona almeno due file per procedere.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Path> inputs = new ArrayList<>();
        for (int i = 0; i < fileListModel.getSize(); i++) {
            String value = fileListModel.getElementAt(i);
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            Path path;
            try {
                path = Paths.get(value.trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Percorso non valido: " + value, "Errore",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!Files.isRegularFile(path)) {
                JOptionPane.showMessageDialog(parent, "File non trovato: " + path, "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!isCsvOrTxt(path)) {
                JOptionPane.showMessageDialog(parent,
                        "Formato non supportato per " + path.getFileName() + ". Utilizza file CSV o TXT.", "Errore",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            inputs.add(path);
        }

        if (inputs.size() < 2) {
            JOptionPane.showMessageDialog(parent, "Aggiungi almeno due file validi.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path reference = inputs.get(0);
        Path baseDir = reference.toAbsolutePath().getParent();
        String defaultName = buildDefaultMergedTextName(reference);

        Path outputPath;
        try {
            outputPath = resolveTextOutput(baseDir, outputText, defaultName);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso di output non valido.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        mergeButton.setEnabled(false);

        Path textOutput = outputPath;
        String csvTxtDetails = "inputs=" + inputs;

        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return csvTxtMerger.merge(inputs, outputPath);
            }

            @Override
            protected void done() {
                try {
                    Path result = get();
                    JOptionPane.showMessageDialog(parent, "Unione completata!\nFile creato: " + result.toAbsolutePath(),
                            "Successo", JOptionPane.INFORMATION_MESSAGE);
                    if (lastSuggestion != null && lastSuggestion.length > 0) {
                        lastSuggestion[0] = result.toAbsolutePath().toString();
                    }
                    auditSuccess("CSV_TXT_MERGE", csvTxtDetails, result);
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "CSV_TXT_MERGE", csvTxtDetails, textOutput, ex,
                            "Errore durante l'unione: ");
                } finally {
                    mergeButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startFolderStamp(JFrame parent, String directoryText, String stampText, JSpinner xSpinner,
            JSpinner ySpinner, JButton stampButton) {
        String trimmedDir = directoryText == null ? "" : directoryText.trim();
        String trimmedText = stampText == null ? "" : stampText.trim();

        if (trimmedDir.isEmpty() || trimmedText.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Compila la cartella e il testo del timbro.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path directory;
        try {
            directory = Paths.get(trimmedDir);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso cartella non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.isDirectory(directory)) {
            JOptionPane.showMessageDialog(parent, "La cartella specificata non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        float x = ((Number) xSpinner.getValue()).floatValue();
        float y = ((Number) ySpinner.getValue()).floatValue();

        stampButton.setEnabled(false);

        Path folderBase = directory.toAbsolutePath();
        String folderDetails = "directory=" + folderBase + ",text=" + trimmedText + ",x=" + x + ",y=" + y;

        SwingWorker<PdfFolderStamper.Result, Void> worker = new SwingWorker<>() {
            @Override
            protected PdfFolderStamper.Result doInBackground() throws Exception {
                return folderStamper.stampDirectory(directory, trimmedText, x, y);
            }

            @Override
            protected void done() {
                try {
                    PdfFolderStamper.Result result = get();
                    StringBuilder message = new StringBuilder().append("Timbro completato!\nFile elaborati: ")
                            .append(result.getStampedCount()).append("\nCartella output: ")
                            .append(result.getOutputDirectory());
                    if (result.hasWarnings()) {
                        message.append("\nAvvisi:");
                        for (String warning : result.getWarnings()) {
                            message.append("\n- ").append(warning);
                        }
                    }
                    JOptionPane.showMessageDialog(parent, message.toString(), "Successo",
                            JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_FOLDER_STAMP", folderDetails, result.getOutputDirectory());
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_FOLDER_STAMP", folderDetails, folderBase, ex,
                            "Errore durante l'applicazione del timbro: ");
                } finally {
                    stampButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startKeywordStamp(JFrame parent, String inputText, String outputText, String keywordText,
            String stampText, boolean caseSensitive, JSpinner xSpinner, JSpinner ySpinner, JButton stampButton) {
        String trimmedInput = inputText == null ? "" : inputText.trim();
        String trimmedKeyword = keywordText == null ? "" : keywordText.trim();
        String trimmedStamp = stampText == null ? "" : stampText.trim();

        if (trimmedInput.isEmpty() || trimmedKeyword.isEmpty() || trimmedStamp.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Compila file di input, parola da cercare e testo del timbro.",
                    "Attenzione", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path inputPath;
        try {
            inputPath = Paths.get(trimmedInput);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso del PDF non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.isRegularFile(inputPath)) {
            JOptionPane.showMessageDialog(parent, "Il file PDF specificato non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultKeywordStampName(inputPath, trimmedKeyword));

        float x = ((Number) xSpinner.getValue()).floatValue();
        float y = ((Number) ySpinner.getValue()).floatValue();

        stampButton.setEnabled(false);

        Path keywordInput = inputPath.toAbsolutePath();
        Path keywordOutput = outputPath;
        String keywordDetails = "input=" + keywordInput + ",keyword=" + trimmedKeyword + ",caseSensitive="
                + caseSensitive;

        SwingWorker<PdfKeywordStamper.Result, Void> worker = new SwingWorker<>() {
            @Override
            protected PdfKeywordStamper.Result doInBackground() throws Exception {
                return keywordStamper.stampPagesContaining(inputPath, outputPath, trimmedKeyword, trimmedStamp,
                        caseSensitive, x, y);
            }

            @Override
            protected void done() {
                try {
                    PdfKeywordStamper.Result result = get();
                    StringBuilder message = new StringBuilder().append("Timbro completato!\nPagine timbrate: ")
                            .append(result.getStampedPages()).append(" su ").append(result.getTotalPages())
                            .append("\nFile output: ").append(result.getOutputFile());
                    JOptionPane.showMessageDialog(parent, message.toString(), "Successo",
                            JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_KEYWORD_STAMP", keywordDetails, result.getOutputFile());
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_KEYWORD_STAMP", keywordDetails, keywordOutput, ex,
                            "Errore durante l'applicazione del timbro: ");
                } finally {
                    stampButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startMarkerSplit(JFrame parent, String pdfText, String markerText, boolean caseSensitive,
            String baseDirText, String folderNameText, boolean appendToExisting, JButton splitButton) {
        String trimmedPdf = pdfText == null ? "" : pdfText.trim();
        String trimmedMarker = markerText == null ? "" : markerText.trim();
        String trimmedBaseDir = baseDirText == null ? "" : baseDirText.trim();
        String trimmedFolderName = folderNameText == null ? "" : folderNameText.trim();

        if (trimmedPdf.isEmpty() || trimmedBaseDir.isEmpty() || trimmedMarker.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Compila tutti i campi richiesti.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path pdfPath;
        Path baseDirPath;
        try {
            pdfPath = Paths.get(trimmedPdf);
            baseDirPath = Paths.get(trimmedBaseDir);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorsi non validi.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.isRegularFile(pdfPath)) {
            JOptionPane.showMessageDialog(parent, "Il file PDF selezionato non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.isDirectory(baseDirPath)) {
            JOptionPane.showMessageDialog(parent, "La cartella base selezionata non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String folderRaw = trimmedFolderName.isEmpty() ? buildDefaultMarkerFolderName(trimmedMarker)
                : trimmedFolderName;
        String sanitizedFolder = sanitizeForFolderName(folderRaw);
        if (sanitizedFolder.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Nome cartella risultati non valido.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        splitButton.setEnabled(false);

        Path markerInput = pdfPath.toAbsolutePath();
        Path markerBase = baseDirPath.toAbsolutePath();
        Path markerOutput = markerBase.resolve(sanitizedFolder);
        String markerDetails = "input=" + markerInput + ",marker=" + trimmedMarker + ",folder=" + sanitizedFolder
                + ",append=" + appendToExisting;

        SwingWorker<PdfMarkerSplitter.Result, Void> worker = new SwingWorker<>() {
            @Override
            protected PdfMarkerSplitter.Result doInBackground() throws Exception {
                return markerSplitter.splitByMarker(pdfPath, baseDirPath, sanitizedFolder, trimmedMarker, caseSensitive,
                        appendToExisting);
            }

            @Override
            protected void done() {
                try {
                    PdfMarkerSplitter.Result result = get();
                    JOptionPane.showMessageDialog(parent,
                            "Split completato!\nDocumenti generati: " + result.getDocumentCount()
                                    + "\nCartella risultati: " + result.getOutputDirectory().toAbsolutePath(),
                            "Successo", JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_MARKER_SPLIT", markerDetails, result.getOutputDirectory());
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_MARKER_SPLIT", markerDetails, markerOutput, ex,
                            "Errore durante lo split: ");
                } finally {
                    splitButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startStringRemoval(JFrame parent, String inputText, String outputText, String searchText,
            boolean caseSensitive, JButton removeButton) {
        String trimmedInput = inputText == null ? "" : inputText.trim();
        String trimmedSearch = searchText == null ? "" : searchText.trim();
        String trimmedOutput = outputText == null ? "" : outputText.trim();

        if (trimmedInput.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Seleziona il PDF sorgente.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (trimmedSearch.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Inserisci la stringa da ricercare.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path inputPath;
        try {
            inputPath = Paths.get(trimmedInput);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso PDF non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!Files.isRegularFile(inputPath)) {
            JOptionPane.showMessageDialog(parent, "Il PDF specificato non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Path baseDirectory = inputPath.getParent() != null ? inputPath.getParent() : Paths.get("").toAbsolutePath();
        Path outputPath = resolvePdfOutput(baseDirectory, trimmedOutput, buildDefaultRemovalName(inputPath));

        removeButton.setEnabled(false);

        Path removalInput = inputPath.toAbsolutePath();
        Path removalOutput = outputPath;
        String removalDetails = "input=" + removalInput + ",query=" + trimmedSearch + ",caseSensitive=" + caseSensitive;

        SwingWorker<PdfStringPageRemover.Result, Void> worker = new SwingWorker<>() {
            @Override
            protected PdfStringPageRemover.Result doInBackground() throws Exception {
                return stringPageRemover.removePagesContaining(inputPath, outputPath, trimmedSearch, caseSensitive);
            }

            @Override
            protected void done() {
                try {
                    PdfStringPageRemover.Result result = get();
                    StringBuilder message = new StringBuilder().append("Rimozione completata!\nPagine rimosse: ")
                            .append(result.getRemovedPages()).append("\nPagine mantenute: ")
                            .append(result.getRetainedPages()).append("\nFile creato: ").append(result.getOutputFile());
                    JOptionPane.showMessageDialog(parent, message.toString(), "Successo",
                            JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_STRING_REMOVAL", removalDetails, result.getOutputFile());
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_STRING_REMOVAL", removalDetails, removalOutput, ex,
                            "Errore durante la rimozione: ");
                } finally {
                    removeButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startOptimization(JFrame parent, String inputText, String outputText, JButton optimizeButton) {
        String trimmedInput = inputText == null ? "" : inputText.trim();
        if (trimmedInput.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Seleziona un file o una cartella da ottimizzare.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path inputPath;
        try {
            inputPath = Paths.get(trimmedInput);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean isDirectory = Files.isDirectory(inputPath);
        boolean isFile = Files.isRegularFile(inputPath);

        if (!isDirectory && !isFile) {
            JOptionPane.showMessageDialog(parent, "Il percorso selezionato non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        optimizeButton.setEnabled(false);

        if (isDirectory) {
            Path dirInput = inputPath.toAbsolutePath();
            String dirDetails = "directory=" + dirInput;
            SwingWorker<Path, Void> worker = new SwingWorker<>() {
                @Override
                protected Path doInBackground() throws Exception {
                    return optimizer.optimizeDirectory(inputPath);
                }

                @Override
                protected void done() {
                    try {
                        Path result = get();
                        JOptionPane.showMessageDialog(parent,
                                "Ottimizzazione completata!\nCartella creata: " + result.toString(), "Successo",
                                JOptionPane.INFORMATION_MESSAGE);
                        auditSuccess("PDF_OPTIMIZATION", dirDetails, result);
                    } catch (Exception ex) {
                        showErrorWithAudit(parent, "PDF_OPTIMIZATION", dirDetails, dirInput, ex,
                                "Errore durante l'ottimizzazione: ");
                    } finally {
                        optimizeButton.setEnabled(true);
                    }
                }
            };
            worker.execute();
            return;
        }

        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultOptimizedName(inputPath));

        Path fileInput = inputPath.toAbsolutePath();
        Path optimizedOutput = outputPath;
        String fileDetails = "input=" + fileInput + ",output=" + optimizedOutput;

        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return optimizer.optimizePdf(inputPath, outputPath);
            }

            @Override
            protected void done() {
                try {
                    Path result = get();
                    JOptionPane.showMessageDialog(parent,
                            "Ottimizzazione completata!\nFile creato: " + result.toString(), "Successo",
                            JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_OPTIMIZATION", fileDetails, result);
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_OPTIMIZATION", fileDetails, optimizedOutput, ex,
                            "Errore durante l'ottimizzazione: ");
                } finally {
                    optimizeButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startRisoOptimization(JFrame parent, String inputText, String outputText, String recordIdText,
            JButton optimizeButton) {
        String trimmedInput = inputText == null ? "" : inputText.trim();
        if (trimmedInput.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Seleziona un PDF da convertire.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path inputPath;
        try {
            inputPath = Paths.get(trimmedInput);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso del PDF non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Files.isRegularFile(inputPath)) {
            JOptionPane.showMessageDialog(parent, "Il file selezionato non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sanitizedRecordId = recordIdText == null ? "" : recordIdText.trim();
        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultRisoOptimizedName(inputPath));
        Path inputAbsolute = inputPath.toAbsolutePath();
        Path outputAbsolute = outputPath;
        String details = "input=" + inputAbsolute + ",output=" + outputAbsolute + ",recordId=" + sanitizedRecordId;

        optimizeButton.setEnabled(false);

        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return risoOptimizer.optimize(inputPath, outputPath, sanitizedRecordId);
            }

            @Override
            protected void done() {
                try {
                    Path result = get();
                    JOptionPane.showMessageDialog(parent, "Conversione completata!\nFile creato: " + result.toString(),
                            "Successo", JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_RISO_OPTIMIZATION", details, result);
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_RISO_OPTIMIZATION", details, outputAbsolute, ex,
                            "Errore durante l'ottimizzazione Riso: ");
                } finally {
                    optimizeButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public void startPdfToWordConversion(JFrame parent, String inputText, String outputText, JButton convertButton) {
        String trimmedInput = inputText == null ? "" : inputText.trim();
        if (trimmedInput.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Seleziona un PDF o una cartella da convertire.", "Attenzione",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path inputPath;
        try {
            inputPath = Paths.get(trimmedInput);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso del PDF non valido.", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean isDirectory = Files.isDirectory(inputPath);
        boolean isFile = Files.isRegularFile(inputPath);

        if (!isDirectory && !isFile) {
            JOptionPane.showMessageDialog(parent, "Il percorso selezionato non esiste.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        convertButton.setEnabled(false);

        if (isDirectory) {
            Path outputDirectory;
            try {
                outputDirectory = resolveDocxDirectoryOutput(inputPath.toAbsolutePath(), outputText,
                        buildDefaultWordDirectoryName(inputPath));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Percorso della cartella di output non valido.", "Errore",
                        JOptionPane.ERROR_MESSAGE);
                convertButton.setEnabled(true);
                return;
            }
            if (Files.exists(outputDirectory) && !Files.isDirectory(outputDirectory)) {
                JOptionPane.showMessageDialog(parent, "Specificare una cartella di output valida.", "Errore",
                        JOptionPane.ERROR_MESSAGE);
                convertButton.setEnabled(true);
                return;
            }

            Path directoryAbsolute = inputPath.toAbsolutePath();
            Path wordOutput = outputDirectory.normalize();
            String details = "directory=" + directoryAbsolute + ",output=" + wordOutput;

            SwingWorker<PdfToWordConverter.BatchResult, Void> worker = new SwingWorker<>() {
                @Override
                protected PdfToWordConverter.BatchResult doInBackground() throws Exception {
                    return pdfToWordConverter.convertDirectory(inputPath, outputDirectory);
                }

                @Override
                protected void done() {
                    try {
                        PdfToWordConverter.BatchResult result = get();
                        String message = "Conversione completata!\nDocumenti creati: " + result.getConvertedCount()
                                + "\nCartella output: " + result.getOutputDirectory().toAbsolutePath();
                        JOptionPane.showMessageDialog(parent, message, "Successo", JOptionPane.INFORMATION_MESSAGE);
                        auditSuccess("PDF_TO_WORD_DIR", details, result.getOutputDirectory());
                    } catch (Exception ex) {
                        showErrorWithAudit(parent, "PDF_TO_WORD_DIR", details, wordOutput, ex,
                                "Errore durante la conversione: ");
                    } finally {
                        convertButton.setEnabled(true);
                    }
                }
            };

            worker.execute();
            return;
        }

        Path outputPath;
        try {
            outputPath = resolveDocxOutput(inputPath.toAbsolutePath().getParent(), outputText,
                    buildDefaultWordName(inputPath));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Percorso del file Word non valido.", "Errore",
                    JOptionPane.ERROR_MESSAGE);
            convertButton.setEnabled(true);
            return;
        }

        Path pdfAbsolute = inputPath.toAbsolutePath();
        Path wordOutput = outputPath;
        String details = "input=" + pdfAbsolute + ",output=" + wordOutput;

        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return pdfToWordConverter.convert(inputPath, outputPath);
            }

            @Override
            protected void done() {
                try {
                    Path result = get();
                    JOptionPane.showMessageDialog(parent,
                            "Conversione completata!\nFile creato: " + result.toAbsolutePath(), "Successo",
                            JOptionPane.INFORMATION_MESSAGE);
                    auditSuccess("PDF_TO_WORD", details, result);
                } catch (Exception ex) {
                    showErrorWithAudit(parent, "PDF_TO_WORD", details, wordOutput, ex,
                            "Errore durante la conversione: ");
                } finally {
                    convertButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    Path resolvePdfOutput(Path baseDirectory, String outputText, String defaultName) {
        Path base = baseDirectory != null ? baseDirectory : Paths.get("").toAbsolutePath();
        String sanitized = outputText == null ? "" : outputText.trim();
        Path candidate = sanitized.isEmpty() ? base.resolve(defaultName) : Paths.get(sanitized);
        Path resolved = candidate.isAbsolute() ? candidate : base.resolve(candidate);
        String filename = resolved.getFileName().toString();
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            resolved = resolved.resolveSibling(filename + ".pdf");
        }
        return resolved.normalize();
    }

    Path resolveTextOutput(Path baseDirectory, String outputText, String defaultName) {
        Path base = baseDirectory != null ? baseDirectory : Paths.get("").toAbsolutePath();
        String sanitized = outputText == null ? "" : outputText.trim();
        Path candidate = sanitized.isEmpty() ? base.resolve(defaultName) : Paths.get(sanitized);
        return (candidate.isAbsolute() ? candidate : base.resolve(candidate)).normalize();
    }

    Path resolveExcelOutput(Path csvPath, String outputText) {
        Path base = csvPath.toAbsolutePath().getParent();
        String sanitized = outputText == null ? "" : outputText.trim();
        Path candidate;
        if (sanitized.isEmpty()) {
            String suggestion = buildDefaultExcelName(csvPath);
            if (suggestion.isEmpty()) {
                throw new IllegalArgumentException("Percorso Excel non valido.");
            }
            candidate = Paths.get(suggestion);
        } else {
            candidate = Paths.get(sanitized);
        }
        if (!candidate.isAbsolute()) {
            candidate = (base != null ? base : Paths.get("").toAbsolutePath()).resolve(candidate);
        }
        String filename = candidate.getFileName().toString();
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            candidate = candidate.resolveSibling(filename + ".xlsx");
        }
        return candidate.normalize();
    }

    Path resolveDocxOutput(Path baseDirectory, String outputText, String defaultName) {
        Path base = baseDirectory != null ? baseDirectory : Paths.get("").toAbsolutePath();
        String sanitized = outputText == null ? "" : outputText.trim();
        Path candidate = sanitized.isEmpty() ? base.resolve(defaultName) : Paths.get(sanitized);
        Path resolved = candidate.isAbsolute() ? candidate : base.resolve(candidate);
        String filename = resolved.getFileName().toString();
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            resolved = resolved.resolveSibling(filename + ".docx");
        }
        return resolved.normalize();
    }

    Path resolveDocxDirectoryOutput(Path baseDirectory, String outputText, String defaultName) {
        Path base = baseDirectory != null ? baseDirectory : Paths.get("").toAbsolutePath();
        String sanitized = outputText == null ? "" : outputText.trim();
        Path candidate = sanitized.isEmpty() ? base.resolve(defaultName) : Paths.get(sanitized);
        Path resolved = candidate.isAbsolute() ? candidate : base.resolve(candidate);
        return resolved.normalize();
    }

    public String buildDefaultOptimizedName(Path inputPath) {
        String filename = inputPath.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        return baseName + "_optimized.pdf";
    }

    public String buildDefaultRisoOptimizedName(Path inputPath) {
        String filename = inputPath.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        return baseName + "_riso_gl9730.pdf";
    }

    public String buildDefaultRisoOptimizedName(String inputText) {
        if (inputText == null) {
            return "";
        }
        String trimmed = inputText.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        try {
            return buildDefaultRisoOptimizedName(Paths.get(trimmed));
        } catch (Exception ex) {
            return "";
        }
    }

    public String buildDefaultMergedTextName(Path inputPath) {
        Path absolute = inputPath.toAbsolutePath();
        String filename = absolute.getFileName() != null ? absolute.getFileName().toString() : absolute.toString();
        if (filename == null || filename.isEmpty()) {
            filename = "merged";
        }
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        if (baseName.isEmpty()) {
            baseName = "merged";
        }
        Path parent = absolute.getParent();
        Path suggestion = parent != null ? parent.resolve(baseName + "_merged.csv")
                : Paths.get(baseName + "_merged.csv").toAbsolutePath();
        return suggestion.toAbsolutePath().toString();
    }

    public String buildDefaultExcelName(String csvPathText) {
        if (csvPathText == null) {
            return "";
        }
        String trimmed = csvPathText.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        try {
            Path csvPath = Paths.get(trimmed);
            return buildDefaultExcelName(csvPath);
        } catch (Exception ex) {
            return "";
        }
    }

    public String buildDefaultExcelName(Path csvPath) {
        Path absolute = csvPath.toAbsolutePath();
        String filename = absolute.getFileName() != null ? absolute.getFileName().toString() : absolute.toString();
        if (filename == null || filename.isEmpty()) {
            filename = "csv";
        }
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        if (baseName.isEmpty()) {
            baseName = "csv";
        }
        Path parent = absolute.getParent();
        Path suggestion = parent != null ? parent.resolve(baseName + ".xlsx")
                : Paths.get(baseName + ".xlsx").toAbsolutePath();
        return suggestion.toAbsolutePath().toString();
    }

    public String buildDefaultWordName(Path inputPath) {
        String filename = inputPath.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        if (baseName.isEmpty()) {
            baseName = "document";
        }
        return baseName + "_word.docx";
    }

    public String buildDefaultWordName(String inputText) {
        if (inputText == null) {
            return "";
        }
        String trimmed = inputText.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        try {
            return buildDefaultWordName(Paths.get(trimmed));
        } catch (Exception ex) {
            return "";
        }
    }

    public String buildDefaultWordDirectoryName(Path inputPath) {
        Path absolute = inputPath.toAbsolutePath();
        String folderName = absolute.getFileName() != null ? absolute.getFileName().toString() : "converted";
        if (folderName == null || folderName.trim().isEmpty()) {
            folderName = "converted";
        }
        return folderName + "_word";
    }

    public String buildDefaultWordDirectoryName(String inputText) {
        if (inputText == null) {
            return "";
        }
        String trimmed = inputText.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        try {
            return buildDefaultWordDirectoryName(Paths.get(trimmed));
        } catch (Exception ex) {
            return "";
        }
    }

    public String buildDefaultBlankPagesName(Path inputPath) {
        String filename = inputPath.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        return baseName + "_with_blank_pages.pdf";
    }

    public String buildDefaultBlankAfterPhraseName(Path inputPath, String phrase) {
        String filename = inputPath.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        String sanitizedPhrase = sanitizeForFileName(phrase);
        return sanitizedPhrase.isEmpty() ? baseName + "_with_blank_after_phrase.pdf"
                : baseName + "_after_" + sanitizedPhrase + ".pdf";
    }

    public String buildDefaultRepeatedName(Path inputPath, int repetitions) {
        String filename = inputPath.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        return baseName + "_x" + Math.max(repetitions, 1) + ".pdf";
    }

    public String buildDefaultFilteredName(Path inputPath, PdfPageFilter.Mode mode) {
        String filename = inputPath.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        String suffix = mode == PdfPageFilter.Mode.ODD ? "without_odd" : "without_even";
        return baseName + "_" + suffix + ".pdf";
    }

    public String buildDefaultRemovalName(Path inputPath) {
        if (inputPath == null) {
            return "filtered_text.pdf";
        }
        String filename = inputPath.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        return baseName + "_filtered_text.pdf";
    }

    public String buildDefaultKeywordStampName(Path inputPath, String keyword) {
        if (inputPath == null) {
            return "keyword_stamp.pdf";
        }
        String filename = inputPath.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        String sanitizedKeyword = sanitizeForFileName(keyword);
        if (sanitizedKeyword.isEmpty()) {
            return baseName + "_keyword_stamp.pdf";
        }
        return baseName + "_keyword_" + sanitizedKeyword + ".pdf";
    }

    private boolean isCsvOrTxt(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return filename.endsWith(".csv") || filename.endsWith(".txt");
    }

    public String suggestRemovalOutputName(String inputPathText, String query) {
        if (inputPathText == null || inputPathText.trim().isEmpty()) {
            return "filtered_text.pdf";
        }
        try {
            Path path = Paths.get(inputPathText.trim());
            String base = buildDefaultRemovalName(path);
            if (query == null || query.trim().isEmpty()) {
                return base;
            }
            String sanitizedQuery = sanitizeForFileName(query).trim();
            if (sanitizedQuery.isEmpty()) {
                return base;
            }
            int dotIndex = base.lastIndexOf('.');
            String prefix = dotIndex > 0 ? base.substring(0, dotIndex) : base;
            return prefix + "_" + sanitizedQuery + ".pdf";
        } catch (Exception ex) {
            return "filtered_text.pdf";
        }
    }

    public String buildDefaultMarkerFolderName(String marker) {
        String sanitized = sanitizeForFolderName(marker);
        if (sanitized.isEmpty()) {
            return "split_result";
        }
        return sanitized + "_split";
    }

    String sanitizeForFolderName(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.replaceAll("[^a-z0-9_-]+", "_");
    }

    String sanitizeForFileName(String phrase) {
        if (phrase == null) {
            return "";
        }
        String trimmed = phrase.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.replaceAll("[^a-zA-Z0-9_-]+", "_");
    }

    void openAuditLog(JFrame parent) {
        Path logPath = AuditLogger.getLogFilePath().toAbsolutePath();
        try {
            Path parentDir = logPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            if (!Files.exists(logPath)) {
                Files.createFile(logPath);
            }
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(logPath.toFile());
                    auditSuccess("AUDIT_LOG_OPEN", "path=" + logPath, logPath);
                    return;
                }
            }
            JOptionPane.showMessageDialog(parent,
                    "Apertura automatica non supportata. Puoi consultare il log qui:\n" + logPath.toString(),
                    "Informazione", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showErrorWithAudit(parent, "AUDIT_LOG_OPEN", "path=" + logPath, logPath, ex, "Impossibile aprire il log: ");
        }
    }

    private void auditSuccess(String action, String details, Path output) {
        try {
            AuditLogger.logSuccess(action, details, output);
        } catch (Exception ignored) {
            // Ignore logging errors to keep the UI responsive
        }
    }

    private void auditFailure(String action, String details, Path output, Throwable error) {
        try {
            AuditLogger.logFailure(action, details, output, error);
        } catch (Exception ignored) {
            // Ignore logging errors to keep the UI responsive
        }
    }

    private void showErrorWithAudit(JFrame parent, String action, String details, Path output, Exception ex,
            String messagePrefix) {
        Throwable rootCause = ex.getCause() != null ? ex.getCause() : ex;
        auditFailure(action, details, output, rootCause);
        String message = (rootCause instanceof IOException || rootCause instanceof IllegalArgumentException)
                ? rootCause.getMessage()
                : ex.getMessage();
        JOptionPane.showMessageDialog(parent, messagePrefix + message, "Errore", JOptionPane.ERROR_MESSAGE);
    }
}
