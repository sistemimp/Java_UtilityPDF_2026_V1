package olivieri.alex.util;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;

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
 * Applies a textual stamp to the first page of every PDF contained in a
 * directory.
 */
public class PdfFolderStamper {

    public static class Result {
        private final int stampedCount;
        private final List<String> warnings;
        private final Path outputDirectory;

        public Result(int stampedCount, List<String> warnings, Path outputDirectory) {
            this.stampedCount = stampedCount;
            this.warnings = warnings;
            this.outputDirectory = outputDirectory;
        }

        public int getStampedCount() {
            return stampedCount;
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

    public Result stampDirectory(Path directory, String stampText, float x, float y) throws IOException {
        String details = "directory=" + directory + ",text=" + stampText + ",x=" + x + ",y=" + y;
        Path outputDirectory = null;
        try {
            if (directory == null || !Files.isDirectory(directory)) {
                throw new IllegalArgumentException("Cartella PDF non valida.");
            }
            if (stampText == null || stampText.trim().isEmpty()) {
                throw new IllegalArgumentException("Testo del timbro non valido.");
            }

            List<Path> pdfFiles = listPdfFiles(directory);
            if (pdfFiles.isEmpty()) {
                throw new IllegalArgumentException("Nessun file PDF trovato nella cartella selezionata.");
            }

            outputDirectory = createOutputDirectory(directory);
            Files.createDirectories(outputDirectory);

            List<String> warnings = new ArrayList<>();
            int stamped = 0;

            for (Path pdfPath : pdfFiles) {
                Path outputFile = outputDirectory.resolve(pdfPath.getFileName());
                boolean success = false;
                try (PdfDocument document = new PdfDocument(new PdfReader(pdfPath.toString()),
                        new PdfWriter(outputFile.toString(), App.writerProperties))) {
                    if (document.getNumberOfPages() < 1) {
                        warnings.add("File senza pagine: " + pdfPath.getFileName());
                        Files.deleteIfExists(outputFile);
                        continue;
                    }
                    PdfPage firstPage = document.getPage(1);
                    PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                    PdfCanvas canvas = new PdfCanvas(firstPage.newContentStreamAfter(), firstPage.getResources(),
                            document);
                    canvas.beginText();
                    canvas.setFontAndSize(font, 1f);
                    canvas.moveText(x, y);
                    canvas.showText(stampText);
                    canvas.endText();
                    canvas.release();
                    success = true;
                } catch (Exception ex) {
                    warnings.add("Errore su " + pdfPath.getFileName() + ": " + ex.getMessage());
                } finally {
                    if (success) {
                        stamped++;
                    } else {
                        Files.deleteIfExists(outputFile);
                    }
                }
            }

            Result result = new Result(stamped, warnings, outputDirectory);
            AuditLogger.logSuccess("SERVICE_PDF_FOLDER_STAMP", details, outputDirectory);
            return result;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_FOLDER_STAMP", details, outputDirectory, ex);
            throw ex;
        }
    }

    private Path createOutputDirectory(Path baseDirectory) throws IOException {
        String baseName = "Stamped";
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
