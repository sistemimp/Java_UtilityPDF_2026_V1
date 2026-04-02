package olivieri.alex.util;

import com.itextpdf.kernel.pdf.PdfAConformance;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfOutputIntent;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.pdfa.PdfADocument;

import olivieri.alex.quality.AuditLogger;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Converte i PDF in PDF/A-3B includendo i metadati richiesti dalla stampante
 * Riso ComColor GD9630.
 */
public class RisoComcolorGd9630Optimizer {

    private static final String OUTPUT_INTENT_NAME = "sRGB IEC61966-2.1";

    public Path optimize(Path inputFile, Path outputFile, String recordId) throws IOException {
        String sanitizedRecordId = recordId == null ? "" : recordId.trim();
        String details = "input=" + inputFile + ",output=" + outputFile + ",recordId=" + sanitizedRecordId;
        try {
            if (inputFile == null || !Files.isRegularFile(inputFile)) {
                throw new IllegalArgumentException("Percorso del PDF non valido.");
            }
            if (outputFile == null) {
                throw new IllegalArgumentException("Percorso di output non valido.");
            }

            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            PdfOutputIntent intent = createSrgbIntent();
            try (PdfDocument sourceDocument = new PdfDocument(new PdfReader(inputFile.toString()));
                    PdfWriter writer = new PdfWriter(outputFile.toString());
                    PdfADocument targetDocument = new PdfADocument(writer, PdfAConformance.PDF_A_3B, intent)) {

                int totalPages = sourceDocument.getNumberOfPages();
                if (totalPages > 0) {
                    sourceDocument.copyPagesTo(1, totalPages, targetDocument);
                }

                if (!sanitizedRecordId.isEmpty()) {
                    targetDocument.getCatalog().put(new PdfName("RecordID"), new PdfString(sanitizedRecordId));
                }
            }

            AuditLogger.logSuccess("SERVICE_RISO_GD9630_OPTIMIZE", details, outputFile);
            return outputFile;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_RISO_GD9630_OPTIMIZE", details, outputFile, ex);
            throw ex;
        }
    }

    private PdfOutputIntent createSrgbIntent() throws IOException {
        byte[] profileData;
        try {
            profileData = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
        } catch (Exception ex) {
            throw new IOException("Impossibile caricare il profilo colore sRGB di sistema.", ex);
        }
        return new PdfOutputIntent("Custom", "", null, OUTPUT_INTENT_NAME, new ByteArrayInputStream(profileData));
    }
}

