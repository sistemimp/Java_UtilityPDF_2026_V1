package olivieri.alex.util;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import olivieri.alex.App;
import olivieri.alex.quality.AuditLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Adds a blank page after every existing page of a PDF.
 */
public class PdfBlankPageInserter {

    /**
     * Generates a new PDF where each original page is followed by an empty page of
     * the same size.
     *
     * @param inputFile  source PDF
     * @param outputFile destination PDF
     * @return the produced file path
     * @throws IOException              when IO problems occur
     * @throws IllegalArgumentException if the input file is invalid
     */
    public Path insertBlankPages(Path inputFile, Path outputFile) throws IOException {
        String details = "input=" + inputFile + ",output=" + outputFile;
        try {
            if (inputFile == null || !Files.isRegularFile(inputFile)) {
                throw new IllegalArgumentException("Percorso del PDF non valido.");
            }

            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (PdfDocument source = new PdfDocument(new PdfReader(inputFile.toString()));
                    PdfWriter writer = new PdfWriter(outputFile.toString(), App.writerProperties);
                    PdfDocument target = new PdfDocument(writer)) {

                int totalPages = source.getNumberOfPages();
                for (int pageIndex = 1; pageIndex <= totalPages; pageIndex++) {
                    source.copyPagesTo(pageIndex, pageIndex, target);
                    PageSize pageSize = new PageSize(source.getPage(pageIndex).getPageSize());
                    target.addNewPage(pageSize);
                }
            }

            AuditLogger.logSuccess("SERVICE_PDF_INSERT_BLANK", details, outputFile);
            return outputFile;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_INSERT_BLANK", details, outputFile, ex);
            throw ex;
        }
    }
}
