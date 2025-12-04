package olivieri.alex.util;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;

import olivieri.alex.App;
import olivieri.alex.quality.AuditLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Removes pages that contain a specific string from a PDF document.
 */
public class PdfStringPageRemover {

    public static class Result {
        private final Path outputFile;
        private final int removedPages;
        private final int retainedPages;

        public Result(Path outputFile, int removedPages, int retainedPages) {
            this.outputFile = outputFile;
            this.removedPages = removedPages;
            this.retainedPages = retainedPages;
        }

        public Path getOutputFile() {
            return outputFile;
        }

        public int getRemovedPages() {
            return removedPages;
        }

        public int getRetainedPages() {
            return retainedPages;
        }
    }

    public Result removePagesContaining(Path inputFile, Path outputFile, String query, boolean caseSensitive)
            throws IOException {
        String details = "input=" + inputFile + ",query=" + query + ",caseSensitive=" + caseSensitive + ",output="
                + outputFile;
        try {
            if (inputFile == null || !Files.isRegularFile(inputFile)) {
                throw new IllegalArgumentException("File PDF di input non valido.");
            }
            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("Stringa di ricerca non valida.");
            }

            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            String preparedQuery = caseSensitive ? query : query.toLowerCase(Locale.ROOT);
            int removed = 0;
            int retained = 0;

            try (PdfDocument source = new PdfDocument(new PdfReader(inputFile.toString()));
                    PdfDocument target = new PdfDocument(new PdfWriter(outputFile.toString(), App.writerProperties))) {

                int total = source.getNumberOfPages();
                for (int pageIndex = 1; pageIndex <= total; pageIndex++) {
                    String pageText = PdfTextExtractor.getTextFromPage(source.getPage(pageIndex),
                            new SimpleTextExtractionStrategy());
                    String pageComparison = caseSensitive ? pageText : pageText.toLowerCase(Locale.ROOT);
                    if (pageComparison.contains(preparedQuery)) {
                        removed++;
                    } else {
                        source.copyPagesTo(pageIndex, pageIndex, target);
                        retained++;
                    }
                }
            }

            if (retained == 0) {
                Files.deleteIfExists(outputFile);
                throw new IllegalArgumentException(
                        "Tutte le pagine contengono la stringa indicata. Nessun documento generato.");
            }

            Result result = new Result(outputFile.toAbsolutePath(), removed, retained);
            AuditLogger.logSuccess("SERVICE_PDF_STRING_REMOVAL", details, outputFile);
            return result;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_STRING_REMOVAL", details, outputFile, ex);
            throw ex;
        }
    }
}
