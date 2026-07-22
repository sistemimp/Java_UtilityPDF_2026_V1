package olivieri.alex.util;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import olivieri.alex.App;
import olivieri.alex.quality.AuditLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Combines two PDFs by copying fixed-size page blocks alternately from each source document.
 */
public class PdfAlternatingMergeService {

    /**
     * Produces a PDF that takes {@code firstChunkSize} pages from the first document, then {@code secondChunkSize}
     * pages from the second document, repeating until both sources are exhausted.
     *
     * @param firstFile       the first PDF
     * @param secondFile      the second PDF
     * @param outputFile      where the mixed PDF will be saved
     * @param firstChunkSize  pages taken from the first document per cycle (must be >= 1)
     * @param secondChunkSize pages taken from the second document per cycle (must be >= 1)
     * @return the output file
     * @throws IOException              if an IO error occurs
     * @throws IllegalArgumentException if inputs are invalid
     */
    public Path mergeAlternating(Path firstFile, Path secondFile, Path outputFile, int firstChunkSize, int secondChunkSize)
            throws IOException {
        String details = "first=" + firstFile + ",second=" + secondFile + ",firstChunk=" + firstChunkSize
                + ",secondChunk=" + secondChunkSize + ",output=" + outputFile;
        try {
            if (firstFile == null || !Files.isRegularFile(firstFile)) {
                throw new IllegalArgumentException("Primo PDF non valido.");
            }
            if (secondFile == null || !Files.isRegularFile(secondFile)) {
                throw new IllegalArgumentException("Secondo PDF non valido.");
            }
            if (firstChunkSize < 1 || secondChunkSize < 1) {
                throw new IllegalArgumentException("Il numero di pagine per blocco deve essere almeno 1.");
            }
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (PdfDocument firstDoc = new PdfDocument(App.newPdfReader(firstFile));
                    PdfDocument secondDoc = new PdfDocument(App.newPdfReader(secondFile));
                    PdfWriter writer = new PdfWriter(outputFile.toString(), App.writerProperties);
                    PdfDocument target = new PdfDocument(writer)) {
                copyAlternatingBlocks(firstDoc, secondDoc, target, firstChunkSize, secondChunkSize);
            }

            AuditLogger.logSuccess("SERVICE_PDF_ALTERNATING_MIX", details, outputFile);
            return outputFile;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_ALTERNATING_MIX", details, outputFile, ex);
            throw ex;
        }
    }

    private void copyAlternatingBlocks(PdfDocument firstDoc, PdfDocument secondDoc, PdfDocument target,
            int firstChunkSize, int secondChunkSize) throws IOException {
        int firstPages = firstDoc.getNumberOfPages();
        int secondPages = secondDoc.getNumberOfPages();
        int firstIndex = 1;
        int secondIndex = 1;

        while (firstIndex <= firstPages || secondIndex <= secondPages) {
            if (firstIndex <= firstPages) {
                int firstEnd = Math.min(firstIndex + firstChunkSize - 1, firstPages);
                firstDoc.copyPagesTo(firstIndex, firstEnd, target);
                firstIndex = firstEnd + 1;
            }
            if (secondIndex <= secondPages) {
                int secondEnd = Math.min(secondIndex + secondChunkSize - 1, secondPages);
                secondDoc.copyPagesTo(secondIndex, secondEnd, target);
                secondIndex = secondEnd + 1;
            }
        }
    }
}
