package olivieri.alex.util;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import olivieri.alex.App;
import olivieri.alex.quality.AuditLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Removes odd or even pages from a PDF.
 */
public class PdfPageFilter {

    public enum Mode {
        ODD, EVEN
    }

    /**
     * Creates a new PDF by copying only the pages that do not match the supplied
     * mode.
     *
     * @param inputFile  source PDF
     * @param outputFile destination PDF
     * @param mode       indicates which pages should be removed
     * @return path to the produced PDF
     * @throws IOException              if IO operations fail
     * @throws IllegalArgumentException when parameters are invalid
     */
    public Path removePages(Path inputFile, Path outputFile, Mode mode) throws IOException {
        String details = "input=" + inputFile + ",mode=" + mode + ",output=" + outputFile;
        try {
            if (inputFile == null || !Files.isRegularFile(inputFile)) {
                throw new IllegalArgumentException("Percorso del PDF non valido.");
            }
            if (mode == null) {
                throw new IllegalArgumentException("Modalita di filtro non valida.");
            }

            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (PdfDocument source = new PdfDocument(new PdfReader(inputFile.toString()));
                    PdfWriter writer = new PdfWriter(outputFile.toString(), App.writerProperties);
                    PdfDocument target = new PdfDocument(writer)) {

                int totalPages = source.getNumberOfPages();
                for (int page = 1; page <= totalPages; page++) {
                    boolean isOdd = page % 2 != 0;
                    if ((mode == Mode.ODD && isOdd) || (mode == Mode.EVEN && !isOdd)) {
                        continue;
                    }
                    source.copyPagesTo(page, page, target);
                }
            }

            AuditLogger.logSuccess("SERVICE_PDF_PAGE_FILTER", details, outputFile);
            return outputFile;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_PAGE_FILTER", details, outputFile, ex);
            throw ex;
        }
    }
}
