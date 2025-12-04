package olivieri.alex.util;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;

import olivieri.alex.App;
import olivieri.alex.quality.AuditLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Applies a textual stamp to the pages of a PDF that contain a specific
 * keyword.
 */
public class PdfKeywordStamper {

    public static class Result {
        private final Path outputFile;
        private final int stampedPages;
        private final int totalPages;

        public Result(Path outputFile, int stampedPages, int totalPages) {
            this.outputFile = outputFile;
            this.stampedPages = stampedPages;
            this.totalPages = totalPages;
        }

        public Path getOutputFile() {
            return outputFile;
        }

        public int getStampedPages() {
            return stampedPages;
        }

        public int getTotalPages() {
            return totalPages;
        }
    }

    public Result stampPagesContaining(Path inputFile, Path outputFile, String keyword, String stampText,
            boolean caseSensitive, float x, float y) throws IOException {
        String details = "input=" + inputFile + ",keyword=" + keyword + ",caseSensitive=" + caseSensitive + ",output="
                + outputFile;
        try {
            if (inputFile == null || !Files.isRegularFile(inputFile)) {
                throw new IllegalArgumentException("File PDF di input non valido.");
            }
            if (keyword == null || keyword.trim().isEmpty()) {
                throw new IllegalArgumentException("Parola da cercare non valida.");
            }
            if (stampText == null || stampText.trim().isEmpty()) {
                throw new IllegalArgumentException("Testo del timbro non valido.");
            }

            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            String preparedKeyword = caseSensitive ? keyword : keyword.toLowerCase(Locale.ROOT);
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            int stamped = 0;

            try (PdfDocument source = new PdfDocument(new PdfReader(inputFile.toString()));
                    PdfDocument target = new PdfDocument(new PdfWriter(outputFile.toString(), App.writerProperties))) {

                int totalPages = source.getNumberOfPages();
                for (int pageIndex = 1; pageIndex <= totalPages; pageIndex++) {
                    PdfPage current = source.getPage(pageIndex);
                    String pageText = PdfTextExtractor.getTextFromPage(current, new SimpleTextExtractionStrategy());
                    String comparable = caseSensitive ? pageText : pageText.toLowerCase(Locale.ROOT);
                    boolean matches = comparable.contains(preparedKeyword);

                    source.copyPagesTo(pageIndex, pageIndex, target);
                    if (matches) {
                        PdfPage copiedPage = target.getPage(target.getNumberOfPages());
                        PdfCanvas canvas = new PdfCanvas(copiedPage.newContentStreamAfter(), copiedPage.getResources(),
                                target);
                        canvas.beginText();
                        canvas.setFontAndSize(font, 1f);
                        canvas.moveText(x, y);
                        canvas.showText(stampText);
                        canvas.endText();
                        canvas.release();
                        stamped++;
                    }
                }

                Result result = new Result(outputFile.toAbsolutePath(), stamped, totalPages);
                AuditLogger.logSuccess("SERVICE_PDF_KEYWORD_STAMP", details, outputFile);
                return result;
            }
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_KEYWORD_STAMP", details, outputFile, ex);
            throw ex;
        }
    }
}
