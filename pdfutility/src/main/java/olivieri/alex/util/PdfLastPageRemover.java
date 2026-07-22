package olivieri.alex.util;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import olivieri.alex.App;
import olivieri.alex.quality.AuditLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Removes the last page from each PDF contained directly in a directory.
 */
public class PdfLastPageRemover {

    public static class Result {
        private final int processedCount;
        private final List<String> warnings;
        private final Path outputDirectory;

        public Result(int processedCount, List<String> warnings, Path outputDirectory) {
            this.processedCount = processedCount;
            this.warnings = warnings;
            this.outputDirectory = outputDirectory;
        }

        public int getProcessedCount() {
            return processedCount;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public Path getOutputDirectory() {
            return outputDirectory;
        }
    }

    public Result removeLastPageFromDirectory(Path directory) throws IOException {
        String details = "directory=" + directory;
        Path outputDirectory = null;
        try {
            if (directory == null || !Files.isDirectory(directory)) {
                throw new IllegalArgumentException("Cartella PDF non valida.");
            }

            List<Path> pdfFiles = listPdfFiles(directory);
            if (pdfFiles.isEmpty()) {
                throw new IllegalArgumentException("Nessun file PDF trovato nella cartella selezionata.");
            }

            outputDirectory = createOutputDirectory(directory);
            Files.createDirectories(outputDirectory);

            List<String> warnings = new ArrayList<>();
            int processed = 0;

            for (Path pdfPath : pdfFiles) {
                Path outputFile = outputDirectory.resolve(pdfPath.getFileName());
                boolean success = false;
                try (PdfDocument source = new PdfDocument(new PdfReader(pdfPath.toString()))) {
                    int totalPages = source.getNumberOfPages();
                    if (totalPages <= 1) {
                        warnings.add("File con una sola pagina non elaborato: " + pdfPath.getFileName());
                        continue;
                    }

                    try (PdfWriter writer = new PdfWriter(outputFile.toString(), App.writerProperties);
                            PdfDocument target = new PdfDocument(writer)) {
                        source.copyPagesTo(1, totalPages - 1, target);
                    }
                    success = true;
                } catch (Exception ex) {
                    warnings.add("Errore su " + pdfPath.getFileName() + ": " + ex.getMessage());
                } finally {
                    if (success) {
                        processed++;
                    } else {
                        Files.deleteIfExists(outputFile);
                    }
                }
            }

            Result result = new Result(processed, warnings, outputDirectory);
            AuditLogger.logSuccess("SERVICE_PDF_REMOVE_LAST_PAGE", details, outputDirectory);
            return result;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_REMOVE_LAST_PAGE", details, outputDirectory, ex);
            throw ex;
        }
    }

    private Path createOutputDirectory(Path baseDirectory) {
        String baseName = "SenzaUltimaPagina";
        Path candidate = baseDirectory.resolve(baseName);
        int counter = 1;
        while (Files.exists(candidate)) {
            candidate = baseDirectory.resolve(baseName + "_" + counter);
            counter++;
        }
        return candidate;
    }

    private List<Path> listPdfFiles(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        }
    }
}
