package olivieri.alex.util;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
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
 * Provides single PDF optimization producing PDF 2.0 output.
 */
public class PdfOptimizer {

    public Path optimizePdf(Path inputFile, Path outputFile) throws IOException {
        String details = "input=" + inputFile + ",output=" + outputFile;
        try {
            if (inputFile == null || !Files.isRegularFile(inputFile)) {
                throw new IllegalArgumentException("Percorso del PDF non valido.");
            }

            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (PdfDocument sourceDocument = new PdfDocument(new PdfReader(inputFile.toString()));
                    PdfWriter writer = new PdfWriter(outputFile.toString(), App.writerProperties);
                    PdfDocument targetDocument = new PdfDocument(writer)) {

                sourceDocument.copyPagesTo(1, sourceDocument.getNumberOfPages(), targetDocument);
            }

            AuditLogger.logSuccess("SERVICE_PDF_OPTIMIZE_FILE", details, outputFile);
            return outputFile;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_OPTIMIZE_FILE", details, outputFile, ex);
            throw ex;
        }
    }

    /**
     * Optimizes every PDF contained directly inside the provided directory, writing
     * the results into a sibling folder named
     * <code>{nome_cartella}_optimized</code> while preserving the original
     * filenames.
     *
     * @param sourceDirectory directory with the PDFs to optimize
     * @return the path to the folder that contains the optimized files
     * @throws IOException              if any IO errors occur during processing
     * @throws IllegalArgumentException if the source path is invalid or empty
     */
    public Path optimizeDirectory(Path sourceDirectory) throws IOException {
        String details = "directory=" + sourceDirectory;
        Path targetDirectory = null;
        try {
            if (sourceDirectory == null || !Files.isDirectory(sourceDirectory)) {
                throw new IllegalArgumentException("Percorso cartella non valido.");
            }

            List<Path> pdfFiles = listPdfFiles(sourceDirectory);
            if (pdfFiles.isEmpty()) {
                throw new IllegalArgumentException("Nessun file PDF trovato nella cartella selezionata.");
            }

            Path absoluteSource = sourceDirectory.toAbsolutePath();
            Path parent = absoluteSource.getParent() != null ? absoluteSource.getParent() : absoluteSource;
            targetDirectory = parent.resolve(absoluteSource.getFileName().toString() + "_optimized");
            Files.createDirectories(targetDirectory);

            for (Path pdfFile : pdfFiles) {
                Path destination = targetDirectory.resolve(pdfFile.getFileName().toString());
                optimizePdf(pdfFile, destination);
            }

            AuditLogger.logSuccess("SERVICE_PDF_OPTIMIZE_DIR", details, targetDirectory);
            return targetDirectory;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_OPTIMIZE_DIR", details, targetDirectory, ex);
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
