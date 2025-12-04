package olivieri.alex.util;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.utils.PdfMerger;

import olivieri.alex.App;
import olivieri.alex.quality.AuditLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides PDF merge utilities using iText.
 */
public class PdfMergeService {

    /**
     * Merges all PDF files that are direct children of the supplied directory into
     * a single PDF. The resulting file is written using the Adobe PDF 1.7
     * specification.
     *
     * @param sourceDirectory directory that contains input PDFs
     * @param outputFile      destination PDF file
     * @return the path to the merged PDF
     * @throws IOException              if any IO errors occur while accessing the
     *                                  files
     * @throws IllegalArgumentException if the path is invalid or no PDF files are
     *                                  found
     */
    public Path mergeDirectory(Path sourceDirectory, Path outputFile) throws IOException {
        String details = "sourceDir=" + sourceDirectory + ",output=" + outputFile;
        try {
            if (sourceDirectory == null || !Files.isDirectory(sourceDirectory)) {
                throw new IllegalArgumentException("Percorso sorgente non valido o non e una cartella.");
            }

            List<Path> pdfFiles = listPdfFiles(sourceDirectory);
            if (pdfFiles.isEmpty()) {
                throw new IllegalArgumentException("Nessun file PDF trovato nella cartella selezionata.");
            }

            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (PdfWriter writer = new PdfWriter(outputFile.toString(), App.writerProperties);
                    PdfDocument targetDocument = new PdfDocument(writer)) {

                PdfMerger merger = new PdfMerger(targetDocument);

                for (Path pdfFile : pdfFiles) {
                    try (PdfDocument sourceDocument = new PdfDocument(new PdfReader(pdfFile.toString()))) {
                        merger.merge(sourceDocument, 1, sourceDocument.getNumberOfPages());
                    }
                }
            }

            AuditLogger.logSuccess("SERVICE_PDF_MERGE", details, outputFile);
            return outputFile;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_MERGE", details, outputFile, ex);
            throw ex;
        }
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
