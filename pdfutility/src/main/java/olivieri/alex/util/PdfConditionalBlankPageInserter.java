package olivieri.alex.util;

import com.itextpdf.kernel.geom.PageSize;
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

/**
 * Adds a blank page after each page that contains the provided text.
 */
public class PdfConditionalBlankPageInserter {

    /**
     * Inserts a blank page after every page whose extracted text contains the
     * supplied phrase.
     *
     * @param inputFile     source PDF
     * @param outputFile    destination PDF
     * @param phrase        text to search
     * @param caseSensitive true to use exact casing, false for case-insensitive
     *                      search
     * @return the produced file path
     * @throws IOException              when IO problems occur
     * @throws IllegalArgumentException if inputs are invalid
     */
    public Path insertAfterPhrase(Path inputFile, Path outputFile, String phrase, boolean caseSensitive)
            throws IOException {
        String details = "input=" + inputFile + ",phrase=" + phrase + ",caseSensitive=" + caseSensitive + ",output="
                + outputFile;
        try {
            if (inputFile == null || !Files.isRegularFile(inputFile)) {
                throw new IllegalArgumentException("Percorso del PDF non valido.");
            }
            if (phrase == null || phrase.trim().isEmpty()) {
                throw new IllegalArgumentException("Frase di ricerca non valida.");
            }

            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            String normalizedPhrase = caseSensitive ? phrase : phrase.toLowerCase();

            try (PdfDocument source = new PdfDocument(new PdfReader(inputFile.toString()));
                    PdfWriter writer = new PdfWriter(outputFile.toString(), App.writerProperties);
                    PdfDocument target = new PdfDocument(writer)) {

                int totalPages = source.getNumberOfPages();
                for (int pageIndex = 1; pageIndex <= totalPages; pageIndex++) {
                    source.copyPagesTo(pageIndex, pageIndex, target);

                    String pageContent = PdfTextExtractor.getTextFromPage(source.getPage(pageIndex),
                            new SimpleTextExtractionStrategy());
                    String textToCheck = caseSensitive ? pageContent : pageContent.toLowerCase();
                    if (textToCheck.contains(normalizedPhrase)) {
                        PageSize pageSize = new PageSize(source.getPage(pageIndex).getPageSize());
                        target.addNewPage(pageSize);
                    }
                }
            }

            AuditLogger.logSuccess("SERVICE_PDF_INSERT_AFTER_PHRASE", details, outputFile);
            return outputFile;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_INSERT_AFTER_PHRASE", details, outputFile, ex);
            throw ex;
        }
    }
}
