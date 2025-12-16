package olivieri.alex.util;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;

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

    public enum RotationMode {
        NONE(0),
        CLOCKWISE(90),
        COUNTERCLOCKWISE(270);

        private final int degrees;

        RotationMode(int degrees) {
            this.degrees = degrees;
        }

        public int getDegrees() {
            return degrees;
        }
    }

    private static final float A4_WIDTH = PageSize.A4.getWidth();
    private static final float A4_HEIGHT = PageSize.A4.getHeight();
    private static final float A4_TOLERANCE = 2.0f;

    public Path mergeDirectory(Path sourceDirectory, Path outputFile) throws IOException {
        return mergeDirectory(sourceDirectory, outputFile, RotationMode.NONE);
    }

    public Path mergeDirectory(Path sourceDirectory, Path outputFile, RotationMode rotationMode) throws IOException {
        RotationMode effectiveRotation = rotationMode == null ? RotationMode.NONE : rotationMode;
        String details = "sourceDir=" + sourceDirectory + ",output=" + outputFile + ",rotation=" + effectiveRotation;
        try {
            rotationMode = effectiveRotation;
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
                for (Path pdfFile : pdfFiles) {
                    try (PdfDocument sourceDocument = new PdfDocument(new PdfReader(pdfFile.toString()))) {
                        copyDocumentPages(sourceDocument, targetDocument, rotationMode);
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

    private static boolean isA4Portrait(PdfPage page) {
        Rectangle size = page.getPageSize();
        return isA4Portrait(size);
    }

    private static boolean isA4Portrait(Rectangle size) {
        if (size == null) {
            return false;
        }
        float width = size.getWidth();
        float height = size.getHeight();
        if (height < width) {
            return false;
        }
        return approxEquals(width, A4_WIDTH) && approxEquals(height, A4_HEIGHT);
    }

    private static boolean approxEquals(float actual, float expected) {
        return Math.abs(actual - expected) <= A4_TOLERANCE;
    }

    private static int normalizeRotation(int rotation) {
        int normalized = rotation % 360;
        if (normalized < 0) {
            normalized += 360;
        }
        return normalized;
    }

    private void copyDocumentPages(PdfDocument source, PdfDocument target, RotationMode rotationMode)
            throws IOException {
        int totalPages = source.getNumberOfPages();
        for (int pageIndex = 1; pageIndex <= totalPages; pageIndex++) {
            PdfPage sourcePage = source.getPage(pageIndex);
            copySinglePage(sourcePage, target, rotationMode);
        }
    }

    private void copySinglePage(PdfPage sourcePage, PdfDocument targetDocument, RotationMode rotationMode)
            throws IOException {
        boolean rotateRequested = rotationMode != RotationMode.NONE && !isA4Portrait(sourcePage);
        int sourceRotation = normalizeRotation(sourcePage.getRotation());
        int extraRotation = rotateRequested ? rotationMode.getDegrees() : 0;
        int totalRotation = normalizeRotation(sourceRotation + extraRotation);

        Rectangle originalSize = sourcePage.getPageSize();
        PageSize baseSize = new PageSize(originalSize.getWidth(), originalSize.getHeight());
        PageSize finalSize = needsDimensionSwap(totalRotation)
                ? new PageSize(baseSize.getHeight(), baseSize.getWidth())
                : baseSize;

        PdfPage targetPage = targetDocument.addNewPage(finalSize);
        PdfCanvas canvas = new PdfCanvas(targetPage);
        applyRotationTransform(canvas, totalRotation, baseSize, finalSize);

        PdfFormXObject pageContent = sourcePage.copyAsFormXObject(targetDocument);
        canvas.addXObjectAt(pageContent, 0, 0);
    }

    private static boolean needsDimensionSwap(int rotation) {
        return rotation % 180 != 0;
    }

    private static void applyRotationTransform(PdfCanvas canvas, int rotation, PageSize baseSize, PageSize finalSize) {
        switch (rotation) {
            case 90:
                canvas.concatMatrix(0, 1, -1, 0, finalSize.getWidth(), 0);
                break;
            case 180:
                canvas.concatMatrix(-1, 0, 0, -1, baseSize.getWidth(), baseSize.getHeight());
                break;
            case 270:
                canvas.concatMatrix(0, -1, 1, 0, 0, finalSize.getHeight());
                break;
            default:
                break;
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
