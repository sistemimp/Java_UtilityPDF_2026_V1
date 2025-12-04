package olivieri.alex.fx;

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
import olivieri.alex.util.PdfPairMerger;
import olivieri.alex.util.PdfPageFilter;
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

/**
 * Provides core PDF operations and helpers for the JavaFX UI, keeping logging consistent with the legacy app.
 */
public final class PdfUtilityFxController {
    private final PdfMergeService mergeService = new PdfMergeService();
    private final PdfOptimizer optimizer = new PdfOptimizer();
    private final RisoGl9730Optimizer risoOptimizer = new RisoGl9730Optimizer();
    private final PdfRepeater repeater = new PdfRepeater();
    private final PdfPageFilter pageFilter = new PdfPageFilter();
    private final PdfPairMerger pairMerger = new PdfPairMerger();
    private final PdfCsvRenamer csvRenamer = new PdfCsvRenamer();
    private final CsvToExcelConverter csvToExcelConverter = new CsvToExcelConverter();
    private final CsvTxtMerger csvTxtMerger = new CsvTxtMerger();
    private final PdfFolderStamper folderStamper = new PdfFolderStamper();
    private final PdfKeywordStamper keywordStamper = new PdfKeywordStamper();
    private final PdfMarkerSplitter markerSplitter = new PdfMarkerSplitter();
    private final PdfStringPageRemover stringPageRemover = new PdfStringPageRemover();
    private final PdfToWordConverter pdfToWordConverter = new PdfToWordConverter();
    private final PdfBlankPageInserter blankPageInserter = new PdfBlankPageInserter();
    private final PdfConditionalBlankPageInserter conditionalBlankPageInserter = new PdfConditionalBlankPageInserter();

    public static final WriterProperties WRITER_PROPERTIES = new WriterProperties().setPdfVersion(PdfVersion.PDF_1_7)
            .useSmartMode().setFullCompressionMode(true).setCompressionLevel(CompressionConstants.BEST_COMPRESSION);

    public Path mergeDirectory(String directoryText, String outputText) throws Exception {
        Path directory = requireDirectory(directoryText, "Seleziona una cartella che contiene i PDF.");
        Path outputPath = resolvePdfOutput(directory.toAbsolutePath(), outputText, "merged.pdf");
        String details = "source=" + directory.toAbsolutePath();
        try {
            Path result = mergeService.mergeDirectory(directory, outputPath);
            auditSuccess("PDF_MERGE", details, result);
            return result;
        } catch (Exception ex) {
            auditFailure("PDF_MERGE", details, outputPath, ex);
            throw ex;
        }
    }

    public Path insertBlankPages(String inputText, String outputText) throws Exception {
        Path inputPath = requireRegularFile(inputText, "Seleziona un file PDF da elaborare.", "Il file specificato non esiste.");
        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultBlankPagesName(inputPath));
        String details = "input=" + inputPath.toAbsolutePath();
        try {
            Path result = blankPageInserter.insertBlankPages(inputPath, outputPath);
            auditSuccess("PDF_INSERT_BLANK", details, result);
            return result;
        } catch (Exception ex) {
            auditFailure("PDF_INSERT_BLANK", details, outputPath, ex);
            throw ex;
        }
    }

    public Path insertBlankAfterPhrase(String inputText, String phrase, boolean caseSensitive, String outputText)
            throws Exception {
        Path inputPath = requireRegularFile(inputText, "Seleziona un file PDF da elaborare.", "Il file specificato non esiste.");
        String trimmedPhrase = safeTrim(phrase);
        if (trimmedPhrase.isEmpty()) {
            throw new IllegalArgumentException("Inserisci la parola o frase da cercare.");
        }
        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultBlankAfterPhraseName(inputPath, trimmedPhrase));
        String details = "input=" + inputPath.toAbsolutePath() + ",phrase=" + trimmedPhrase + ",caseSensitive="
                + caseSensitive;
        try {
            Path result = conditionalBlankPageInserter.insertAfterPhrase(inputPath, outputPath, trimmedPhrase,
                    caseSensitive);
            auditSuccess("PDF_INSERT_AFTER_PHRASE", details, result);
            return result;
        } catch (Exception ex) {
            auditFailure("PDF_INSERT_AFTER_PHRASE", details, outputPath, ex);
            throw ex;
        }
    }

    public Path optimize(String inputText, String outputText) throws Exception {
        Path inputPath = requireFileOrDirectory(inputText, "Seleziona un file o una cartella da ottimizzare.");
        boolean isDirectory = Files.isDirectory(inputPath);
        boolean isFile = Files.isRegularFile(inputPath);
        if (!isDirectory && !isFile) {
            throw new IllegalArgumentException("Il percorso selezionato non esiste.");
        }

        if (isDirectory) {
            Path dirAbsolute = inputPath.toAbsolutePath();
            String details = "directory=" + dirAbsolute;
            Path result = optimizer.optimizeDirectory(inputPath);
            auditSuccess("PDF_OPTIMIZATION", details, result);
            return result;
        }

        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultOptimizedName(inputPath));
        Path inputAbsolute = inputPath.toAbsolutePath();
        String details = "input=" + inputAbsolute + ",output=" + outputPath;
        Path result = optimizer.optimizePdf(inputPath, outputPath);
        auditSuccess("PDF_OPTIMIZATION", details, result);
        return result;
    }

    public Path optimizeRiso(String inputText, String outputText, String recordIdText) throws Exception {
        Path inputPath = requireRegularFile(inputText, "Seleziona un PDF da convertire.", "Il file specificato non esiste.");
        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultRisoOptimizedName(inputPath));
        String sanitizedRecordId = safeTrim(recordIdText);
        Path inputAbsolute = inputPath.toAbsolutePath();
        String details = "input=" + inputAbsolute + ",output=" + outputPath + ",recordId=" + sanitizedRecordId;
        Path result = risoOptimizer.optimize(inputPath, outputPath, sanitizedRecordId);
        auditSuccess("PDF_RISO_OPTIMIZATION", details, result);
        return result;
    }

    public Path repeatPdf(String inputText, int repetitions, String outputText) throws Exception {
        if (repetitions < 1) {
            throw new IllegalArgumentException("Le ripetizioni devono essere almeno 1.");
        }
        Path inputPath = requireRegularFile(inputText, "Seleziona un file PDF da ripetere.", "Il file specificato non esiste.");
        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultRepeatedName(inputPath, repetitions));
        String details = "input=" + inputPath.toAbsolutePath() + ",repetitions=" + Math.max(repetitions, 1);
        Path result = repeater.repeat(inputPath, outputPath, repetitions);
        auditSuccess("PDF_REPEAT", details, result);
        return result;
    }

    public Path filterPages(String inputText, PdfPageFilter.Mode mode, String outputText) throws Exception {
        Path inputPath = requireRegularFile(inputText, "Seleziona un file PDF da filtrare.", "Il file specificato non esiste.");
        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultFilteredName(inputPath, mode));
        String details = "input=" + inputPath.toAbsolutePath() + ",mode=" + mode;
        try {
            Path result = pageFilter.removePages(inputPath, outputPath, mode);
            auditSuccess("PDF_PAGE_FILTER", details, result);
            return result;
        } catch (Exception ex) {
            auditFailure("PDF_PAGE_FILTER", details, outputPath, ex);
            throw ex;
        }
    }

    public PdfPairMerger.Result mergeMatchingPairs(String firstDirText, String secondDirText) throws Exception {
        Path firstDir = requireDirectory(firstDirText, "Seleziona la prima cartella.");
        Path secondDir = requireDirectory(secondDirText, "Seleziona la seconda cartella.");
        String details = "first=" + firstDir.toAbsolutePath() + ",second=" + secondDir.toAbsolutePath();
        try {
            PdfPairMerger.Result result = pairMerger.mergeMatchingPairs(firstDir, secondDir);
            auditSuccess("PDF_PAIR_MERGE", details, result.getOutputDirectory());
            return result;
        } catch (Exception ex) {
            auditFailure("PDF_PAIR_MERGE", details, firstDir.toAbsolutePath(), ex);
            throw ex;
        }
    }

    public PdfStringPageRemover.Result removePagesContaining(String inputText, String outputText, String query,
            boolean caseSensitive) throws Exception {
        Path inputPath = requireRegularFile(inputText, "Seleziona un file PDF sorgente.", "Il file specificato non esiste.");
        String trimmedQuery = safeTrim(query);
        if (trimmedQuery.isEmpty()) {
            throw new IllegalArgumentException("Inserisci la stringa da cercare.");
        }
        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultRemovalName(inputPath));
        String details = "input=" + inputPath.toAbsolutePath() + ",query=" + trimmedQuery + ",caseSensitive="
                + caseSensitive;
        try {
            PdfStringPageRemover.Result result = stringPageRemover.removePagesContaining(inputPath, outputPath, trimmedQuery,
                    caseSensitive);
            auditSuccess("PDF_STRING_REMOVAL", details, result.getOutputFile());
            return result;
        } catch (Exception ex) {
            auditFailure("PDF_STRING_REMOVAL", details, outputPath, ex);
            throw ex;
        }
    }

    public PdfMarkerSplitter.Result splitByMarker(String pdfText, String markerText, boolean caseSensitive,
            String baseDirText, String folderNameText, boolean appendToExisting) throws Exception {
        Path pdfPath = requireRegularFile(pdfText, "Seleziona un file PDF.", "Il file specificato non esiste.");
        Path baseDir = requireDirectory(baseDirText, "Seleziona una cartella base.");
        String sanitizedMarker = safeTrim(markerText);
        if (sanitizedMarker.isEmpty()) {
            throw new IllegalArgumentException("Inserisci la stringa marker.");
        }
        String folderName = safeTrim(folderNameText);
        if (folderName.isEmpty()) {
            folderName = buildDefaultMarkerFolderName(sanitizedMarker);
        }
        folderName = sanitizeForFolderName(folderName);
        if (folderName.isEmpty()) {
            throw new IllegalArgumentException("Nome cartella dei risultati non valido.");
        }
        String details = "input=" + pdfPath.toAbsolutePath() + ",marker=" + sanitizedMarker + ",folder=" + folderName
                + ",append=" + appendToExisting;
        try {
            PdfMarkerSplitter.Result result = markerSplitter.splitByMarker(pdfPath, baseDir, folderName, sanitizedMarker,
                    caseSensitive, appendToExisting);
            auditSuccess("PDF_MARKER_SPLIT", details, result.getOutputDirectory());
            return result;
        } catch (Exception ex) {
            auditFailure("PDF_MARKER_SPLIT", details, baseDir, ex);
            throw ex;
        }
    }

    public PdfCsvRenamer.Result renameFromCsv(String directoryText, String csvText) throws Exception {
        Path directory = requireDirectory(directoryText, "Seleziona la cartella PDF.");
        Path csvPath = requireRegularFile(csvText, "Seleziona il file CSV.", "Il file CSV non esiste.");
        String details = "directory=" + directory.toAbsolutePath() + ",csv=" + csvPath.toAbsolutePath();
        try {
            PdfCsvRenamer.Result result = csvRenamer.renameFromCsv(directory, csvPath);
            auditSuccess("PDF_CSV_RENAME", details, directory.toAbsolutePath());
            return result;
        } catch (Exception ex) {
            auditFailure("PDF_CSV_RENAME", details, directory.toAbsolutePath(), ex);
            throw ex;
        }
    }

    public Path convertCsvToExcel(String csvText, String excelText) throws Exception {
        Path csvPath = requireRegularFile(csvText, "Seleziona il file CSV.", "Il file CSV non esiste.");
        Path outputPath = resolveExcelOutput(csvPath, excelText);
        String details = "csv=" + csvPath.toAbsolutePath() + ",excel=" + outputPath;
        try {
            Path result = csvToExcelConverter.convert(csvPath, outputPath);
            auditSuccess("CSV_TO_EXCEL", details, result);
            return result;
        } catch (Exception ex) {
            auditFailure("CSV_TO_EXCEL", details, outputPath, ex);
            throw ex;
        }
    }

    public Path mergeCsvTxt(List<String> inputs, String outputText) throws Exception {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("Seleziona almeno un file da unire.");
        }
        List<Path> paths = new ArrayList<>();
        List<String> sanitizedInputs = new ArrayList<>();
        for (String candidate : inputs) {
            String trimmed = safeTrim(candidate);
            if (trimmed.isEmpty()) {
                continue;
            }
            Path validated = requireCsvOrTxtFile(trimmed);
            paths.add(validated);
            sanitizedInputs.add(validated.toAbsolutePath().toString());
        }
        if (paths.size() < 2) {
            throw new IllegalArgumentException("Seleziona almeno due file validi da unire.");
        }
        Path firstPath = paths.get(0);
        Path outputPath = resolveTextOutput(firstPath.getParent(), outputText, buildDefaultMergedTextName(firstPath));
        String details = "inputs=" + sanitizedInputs + ",output=" + outputPath;
        try {
            Path result = csvTxtMerger.merge(paths, outputPath);
            auditSuccess("CSV_TXT_MERGE", details, result);
            return result;
        } catch (Exception ex) {
            auditFailure("CSV_TXT_MERGE", details, outputPath, ex);
            throw ex;
        }
    }

    public PdfFolderStamper.Result stampFolder(String directoryText, String stampText, float x, float y)
            throws Exception {
        Path directory = requireDirectory(directoryText, "Seleziona la cartella dei PDF.");
        String trimmedText = safeTrim(stampText);
        if (trimmedText.isEmpty()) {
            throw new IllegalArgumentException("Compila la cartella e il testo del timbro.");
        }
        String details = "directory=" + directory.toAbsolutePath() + ",text=" + trimmedText + ",x=" + x + ",y=" + y;
        try {
            PdfFolderStamper.Result result = folderStamper.stampDirectory(directory, trimmedText, x, y);
            auditSuccess("PDF_FOLDER_STAMP", details, result.getOutputDirectory());
            return result;
        } catch (Exception ex) {
            auditFailure("PDF_FOLDER_STAMP", details, directory.toAbsolutePath(), ex);
            throw ex;
        }
    }

    public PdfKeywordStamper.Result stampKeyword(String inputText, String outputText, String keywordText, String stampText,
            boolean caseSensitive, float x, float y) throws Exception {
        Path inputPath = requireRegularFile(inputText, "Seleziona un file PDF da elaborare.",
                "Il file specificato non esiste.");
        String trimmedKeyword = safeTrim(keywordText);
        if (trimmedKeyword.isEmpty()) {
            throw new IllegalArgumentException("Inserisci la parola da cercare.");
        }
        String trimmedStamp = safeTrim(stampText);
        if (trimmedStamp.isEmpty()) {
            throw new IllegalArgumentException("Inserisci il testo del timbro.");
        }
        Path outputPath = resolvePdfOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultKeywordStampName(inputPath, trimmedKeyword));
        String details = "input=" + inputPath.toAbsolutePath() + ",keyword=" + trimmedKeyword + ",caseSensitive="
                + caseSensitive + ",output=" + outputPath;
        try {
            PdfKeywordStamper.Result result = keywordStamper.stampPagesContaining(inputPath, outputPath, trimmedKeyword,
                    trimmedStamp, caseSensitive, x, y);
            auditSuccess("PDF_KEYWORD_STAMP", details, result.getOutputFile());
            return result;
        } catch (Exception ex) {
            auditFailure("PDF_KEYWORD_STAMP", details, outputPath, ex);
            throw ex;
        }
    }

    public Path convertPdfToWord(String inputText, String outputText) throws Exception {
        Path inputPath = requireRegularFile(inputText, "Seleziona un PDF da convertire.",
                "Il file specificato non esiste.");
        Path outputPath = resolveDocxOutput(inputPath.toAbsolutePath().getParent(), outputText,
                buildDefaultWordName(inputPath));
        String details = "input=" + inputPath.toAbsolutePath() + ",output=" + outputPath;
        try {
            Path result = pdfToWordConverter.convert(inputPath, outputPath);
            auditSuccess("PDF_TO_WORD", details, result);
            return result;
        } catch (Exception ex) {
            auditFailure("PDF_TO_WORD", details, outputPath, ex);
            throw ex;
        }
    }

    public PdfToWordConverter.BatchResult convertDirectoryToWord(String directoryText, String outputText)
            throws Exception {
        Path directory = requireDirectory(directoryText, "Seleziona una cartella da convertire.");
        Path outputDirectory = resolveDocxDirectoryOutput(directory.toAbsolutePath(), outputText,
                buildDefaultWordDirectoryName(directory));
        String details = "directory=" + directory.toAbsolutePath() + ",output=" + outputDirectory;
        try {
            PdfToWordConverter.BatchResult result = pdfToWordConverter.convertDirectory(directory, outputDirectory);
            auditSuccess("PDF_TO_WORD_DIR", details, result.getOutputDirectory());
            return result;
        } catch (Exception ex) {
            auditFailure("PDF_TO_WORD_DIR", details, outputDirectory, ex);
            throw ex;
        }
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
        Path resolved = candidate.isAbsolute() ? candidate : base.resolve(candidate);
        return resolved.normalize();
    }

    Path resolveExcelOutput(Path csvPath, String outputText) {
        Path base = csvPath != null ? csvPath.toAbsolutePath().getParent() : Paths.get("").toAbsolutePath();
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

    public String buildDefaultMarkerFolderName(String marker) {
        String sanitized = sanitizeForFolderName(marker);
        if (sanitized.isEmpty()) {
            return "split_result";
        }
        return sanitized + "_split";
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

    public String buildDefaultExcelName(Path csvPath) {
        if (csvPath == null) {
            return "";
        }
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

    public String buildDefaultExcelName(String csvPathText) {
        if (csvPathText == null) {
            return "";
        }
        String trimmed = csvPathText.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        try {
            return buildDefaultExcelName(Paths.get(trimmed));
        } catch (Exception ex) {
            return "";
        }
    }

    public String buildDefaultWordName(Path inputPath) {
        if (inputPath == null) {
            return "";
        }
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
        if (inputPath == null) {
            return "converted_word";
        }
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

    public String suggestFilteredOutputName(String inputPathText, PdfPageFilter.Mode mode) {
        if (inputPathText == null || inputPathText.trim().isEmpty()) {
            return "";
        }
        try {
            Path path = Paths.get(inputPathText.trim());
            return buildDefaultFilteredName(path, mode);
        } catch (Exception ex) {
            return "";
        }
    }

    public String suggestRemovalOutputName(String inputPathText, String query) {
        if (inputPathText == null || inputPathText.trim().isEmpty()) {
            return "filtered_text.pdf";
        }
        try {
            Path path = Paths.get(inputPathText.trim());
            String base = buildDefaultRemovalName(path);
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

    public void openAuditLog() throws IOException {
        Path logPath = AuditLogger.getLogFilePath().toAbsolutePath();
        Path parent = logPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
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
        throw new UnsupportedOperationException(
                "Apertura automatica non supportata. Puoi consultare il log qui:\n" + logPath.toString());
    }

    private Path requireRegularFile(String inputText, String emptyMessage, String missingMessage) {
        String trimmed = safeTrim(inputText);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        Path path = toPath(trimmed, "Percorso non valido.");
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(missingMessage);
        }
        return path;
    }

    private Path requireDirectory(String inputText, String emptyMessage) {
        String trimmed = safeTrim(inputText);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        Path path = toPath(trimmed, "Percorso cartella non valido.");
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("La cartella specificata non esiste.");
        }
        return path;
    }

    private Path requireFileOrDirectory(String inputText, String emptyMessage) {
        String trimmed = safeTrim(inputText);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(emptyMessage);
        }
        Path path = toPath(trimmed, "Percorso non valido.");
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Il percorso selezionato non esiste.");
        }
        return path;
    }

    private Path toPath(String inputText, String message) {
        try {
            return Paths.get(inputText);
        } catch (Exception ex) {
            throw new IllegalArgumentException(message, ex);
        }
    }

    private String safeTrim(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String sanitizeForFileName(String phrase) {
        if (phrase == null) {
            return "";
        }
        String trimmed = phrase.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.replaceAll("[^a-zA-Z0-9_-]+", "_");
    }

    private Path requireCsvOrTxtFile(String inputText) {
        Path path = requireRegularFile(inputText, "Seleziona un file da unire.",
                "Il file specificato non esiste.");
        if (!isCsvOrTxt(path)) {
            String filename = path.getFileName() != null ? path.getFileName().toString() : path.toString();
            throw new IllegalArgumentException(
                    "Formato non supportato per " + filename + ". Utilizza file CSV o TXT.");
        }
        return path;
    }

    private boolean isCsvOrTxt(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return filename.endsWith(".csv") || filename.endsWith(".txt");
    }

    private String sanitizeForFolderName(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.replaceAll("[^a-z0-9_-]+", "_");
    }

    private void auditSuccess(String action, String details, Path output) {
        try {
            AuditLogger.logSuccess(action, details, output);
        } catch (Exception ignored) {
            // Keep UI responsive
        }
    }

    private void auditFailure(String action, String details, Path output, Throwable error) {
        try {
            AuditLogger.logFailure(action, details, output, error);
        } catch (Exception ignored) {
            // Keep UI responsive
        }
    }
}
