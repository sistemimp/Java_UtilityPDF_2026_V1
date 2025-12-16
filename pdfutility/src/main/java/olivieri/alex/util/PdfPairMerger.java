package olivieri.alex.util;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;

import olivieri.alex.App;
import olivieri.alex.quality.AuditLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Merges PDF files from two directories when they share the same filename.
 */
public class PdfPairMerger {

    public static final String RESULT_DIRECTORY_NAME = "Result";

    public static class Result {
        private final Path outputDirectory;
        private final int mergedCount;
        private final Path missingReport;

        public Result(Path outputDirectory, int mergedCount, Path missingReport) {
            this.outputDirectory = outputDirectory;
            this.mergedCount = mergedCount;
            this.missingReport = missingReport;
        }

        public Path getOutputDirectory() {
            return outputDirectory;
        }

        public int getMergedCount() {
            return mergedCount;
        }

        public Path getMissingReport() {
            return missingReport;
        }

        public boolean hasMissingReport() {
            return missingReport != null && Files.exists(missingReport);
        }
    }

    /**
     * Merges files from {@code firstDirectory} with files of the same name found in
     * {@code secondDirectory}.
     */
    public Result mergeMatchingPairs(Path firstDirectory, Path secondDirectory) throws IOException {
        return mergeMatchingPairs(firstDirectory, secondDirectory, false);
    }

    public Result mergeMatchingPairs(Path firstDirectory, Path secondDirectory, boolean normalizeFR)
            throws IOException {
        String details = "firstDir=" + firstDirectory + ",secondDir=" + secondDirectory + ",normalizeFR=" + normalizeFR;
        Path plannedOutput = null;
        try {
            if (firstDirectory == null || !Files.isDirectory(firstDirectory)) {
                throw new IllegalArgumentException("La prima cartella non e valida.");
            }
            if (secondDirectory == null || !Files.isDirectory(secondDirectory)) {
                throw new IllegalArgumentException("La seconda cartella non e valida.");
            }

            List<Path> firstPdfs = listPdfFiles(firstDirectory);
            if (firstPdfs.isEmpty()) {
                throw new IllegalArgumentException("Nessun PDF trovato nella prima cartella.");
            }

            Map<String, Path> secondPdfMap = listPdfFiles(secondDirectory).stream()
                    .collect(Collectors.toMap(path -> path.getFileName().toString().toLowerCase(Locale.ROOT),
                            path -> path, (existing, replacement) -> replacement, HashMap::new));

            List<Pair> pairsToMerge = new ArrayList<>();
            List<MissingEntry> missingEntries = new ArrayList<>();
            Set<String> firstNames = new HashSet<>();
            for (Path firstPdf : firstPdfs) {
                String key = firstPdf.getFileName().toString().toLowerCase(Locale.ROOT);
                firstNames.add(key);
                Path match = secondPdfMap.get(key);
                if (match != null) {
                    pairsToMerge.add(new Pair(firstPdf, match));
                } else {
                    missingEntries.add(new MissingEntry("Cartella 1", firstDirectory, firstPdf.getFileName().toString(),
                            "Nessun corrispondente in cartella 2"));
                }
            }

            for (Map.Entry<String, Path> entry : secondPdfMap.entrySet()) {
                if (!firstNames.contains(entry.getKey())) {
                    missingEntries.add(new MissingEntry("Cartella 2", secondDirectory,
                            entry.getValue().getFileName().toString(),
                            "Nessun corrispondente in cartella 1"));
                }
            }

            Path absoluteFirst = firstDirectory.toAbsolutePath();
            Path resultBase = absoluteFirst.getParent() != null ? absoluteFirst.getParent() : absoluteFirst;
            Path resultDirectory = resultBase.resolve(RESULT_DIRECTORY_NAME);
            plannedOutput = resultDirectory;
            Files.createDirectories(resultDirectory);

            Path missingReportPath = null;
            if (!missingEntries.isEmpty()) {
                missingReportPath = resultDirectory.resolve("missing_pairs.txt");
                Files.write(missingReportPath, formatMissingEntries(missingEntries), StandardCharsets.UTF_8);
            }

            for (Pair pair : pairsToMerge) {
                Path outputFile = resultDirectory.resolve(pair.first().getFileName());
                mergeTwoPdfs(pair.first(), pair.second(), outputFile, normalizeFR);
            }

            Result result = new Result(resultDirectory, pairsToMerge.size(), missingReportPath);
            AuditLogger.logSuccess("SERVICE_PDF_PAIR_MERGE", details, resultDirectory);
            return result;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_PAIR_MERGE", details, plannedOutput, ex);
            throw ex;
        }
    }

    private void mergeTwoPdfs(Path first, Path second, Path outputFile, boolean normalizeFR) throws IOException {
        try (PdfWriter writer = new PdfWriter(outputFile.toString(), App.writerProperties);
                PdfDocument outputDocument = new PdfDocument(writer);
                PdfDocument firstDocument = new PdfDocument(new PdfReader(first.toString()));
                PdfDocument secondDocument = new PdfDocument(new PdfReader(second.toString()))) {

            PdfMerger merger = new PdfMerger(outputDocument);
            merger.merge(firstDocument, 1, firstDocument.getNumberOfPages());
            if (normalizeFR && (firstDocument.getNumberOfPages() % 2 != 0)) {
                addBlankPage(outputDocument, firstDocument);
            }
            merger.merge(secondDocument, 1, secondDocument.getNumberOfPages());
        }
    }

    private void addBlankPage(PdfDocument target, PdfDocument reference) {
        if (target == null || reference == null) {
            return;
        }
        PdfPage lastPage = reference.getLastPage();
        PageSize pageSize = lastPage != null ? new PageSize(lastPage.getPageSize()) : PageSize.A4;
        target.addNewPage(pageSize);
    }

    private List<Path> listPdfFiles(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .collect(Collectors.toList());
        }
    }

    private record Pair(Path first, Path second) {
    }

    private record MissingEntry(String folderLabel, Path folder, String fileName, String detail) {
    }

    private List<String> formatMissingEntries(List<MissingEntry> entries) {
        List<String> lines = new ArrayList<>();
        lines.add("Cartella\tPercorso\tFile\tDettaglio");
        for (MissingEntry entry : entries) {
            String folderPath = entry.folder() != null ? entry.folder().toAbsolutePath().toString() : "";
            String detail = entry.detail() != null ? entry.detail() : "";
            lines.add(entry.folderLabel() + "\t" + folderPath + "\t" + entry.fileName() + "\t" + detail);
        }
        return lines;
    }
}
