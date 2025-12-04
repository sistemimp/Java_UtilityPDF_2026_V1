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
 * Duplicates a PDF content multiple times into a single output document.
 */
public class PdfRepeater {

    /**
     * Concatenates the source PDF onto itself the specified number of times.
     *
     * @param inputFile   source PDF
     * @param outputFile  destination PDF
     * @param repetitions number of times to repeat (must be >= 1)
     * @return output file path
     * @throws IOException              if IO errors occur
     * @throws IllegalArgumentException if parameters are invalid
     */
    public Path repeat(Path inputFile, Path outputFile, int repetitions) throws IOException {
        String details = "input=" + inputFile + ",repetitions=" + repetitions + ",output=" + outputFile;
        try {
            if (inputFile == null || !Files.isRegularFile(inputFile)) {
                throw new IllegalArgumentException("Percorso del PDF non valido.");
            }
            if (repetitions < 1) {
                throw new IllegalArgumentException("Il numero di ripetizioni deve essere almeno 1.");
            }

            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (PdfDocument source = new PdfDocument(new PdfReader(inputFile.toString()));
                    PdfWriter writer = new PdfWriter(outputFile.toString(), App.writerProperties);
                    PdfDocument target = new PdfDocument(writer)) {

                int totalPages = source.getNumberOfPages();
                for (int i = 0; i < repetitions; i++) {
                    source.copyPagesTo(1, totalPages, target);
                }
            }

            AuditLogger.logSuccess("SERVICE_PDF_REPEAT", details, outputFile);
            return outputFile;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_REPEAT", details, outputFile, ex);
            throw ex;
        }
    }
}
