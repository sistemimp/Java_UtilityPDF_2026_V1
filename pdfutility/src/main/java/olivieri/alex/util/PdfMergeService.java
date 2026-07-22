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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
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

    public static final class BatchMergeResult {
        private final Path outputDirectory;
        private final int sourcePdfCount;
        private final int mergedGroupCount;
        private final int groupSize;
        private final List<Path> outputFiles;

        public BatchMergeResult(Path outputDirectory, int sourcePdfCount, int mergedGroupCount, int groupSize,
                List<Path> outputFiles) {
            this.outputDirectory = outputDirectory;
            this.sourcePdfCount = sourcePdfCount;
            this.mergedGroupCount = mergedGroupCount;
            this.groupSize = groupSize;
            this.outputFiles = outputFiles == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(outputFiles));
        }

        public Path getOutputDirectory() {
            return outputDirectory;
        }

        public int getSourcePdfCount() {
            return sourcePdfCount;
        }

        public int getMergedGroupCount() {
            return mergedGroupCount;
        }

        public int getGroupSize() {
            return groupSize;
        }

        public List<Path> getOutputFiles() {
            return outputFiles;
        }
    }

    private static final float A4_WIDTH = PageSize.A4.getWidth();
    private static final float A4_HEIGHT = PageSize.A4.getHeight();
    private static final float A4_TOLERANCE = 2.0f;

    public Path mergeDirectory(Path sourceDirectory, Path outputFile) throws IOException {
        return mergeDirectory(sourceDirectory, outputFile, RotationMode.NONE, false);
    }

    public Path mergeDirectory(Path sourceDirectory, Path outputFile, RotationMode rotationMode) throws IOException {
        return mergeDirectory(sourceDirectory, outputFile, rotationMode, false);
    }

    public Path mergeDirectory(Path sourceDirectory, Path outputFile, RotationMode rotationMode, boolean forceA4PageSize)
            throws IOException {
        RotationMode effectiveRotation = rotationMode == null ? RotationMode.NONE : rotationMode;
        String details = "sourceDir=" + sourceDirectory + ",output=" + outputFile + ",rotation=" + effectiveRotation
                + ",forceA4=" + forceA4PageSize;
        try {
            if (sourceDirectory == null || !Files.isDirectory(sourceDirectory)) {
                throw new IllegalArgumentException("Percorso sorgente non valido o non e una cartella.");
            }

            List<Path> pdfFiles = listPdfFiles(sourceDirectory);
            if (pdfFiles.isEmpty()) {
                throw new IllegalArgumentException("Nessun file PDF trovato nella cartella selezionata.");
            }

            mergeFiles(pdfFiles, outputFile, effectiveRotation, forceA4PageSize);

            AuditLogger.logSuccess("SERVICE_PDF_MERGE", details, outputFile);
            return outputFile;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_MERGE", details, outputFile, ex);
            throw ex;
        }
    }

    public BatchMergeResult mergeDirectoryInBatches(Path sourceDirectory, Path outputDirectory, int groupSize,
            String outputPrefix, RotationMode rotationMode) throws IOException {
        RotationMode effectiveRotation = rotationMode == null ? RotationMode.NONE : rotationMode;
        String details = "sourceDir=" + sourceDirectory + ",outputDir=" + outputDirectory + ",groupSize=" + groupSize
                + ",prefix=" + outputPrefix + ",rotation=" + effectiveRotation;
        try {
            if (sourceDirectory == null || !Files.isDirectory(sourceDirectory)) {
                throw new IllegalArgumentException("Percorso sorgente non valido o non e una cartella.");
            }
            if (outputDirectory == null) {
                throw new IllegalArgumentException("Percorso cartella di output non valido.");
            }
            if (groupSize < 1) {
                throw new IllegalArgumentException("Il numero di PDF per gruppo deve essere almeno 1.");
            }

            List<Path> pdfFiles = listPdfFiles(sourceDirectory);
            if (pdfFiles.isEmpty()) {
                throw new IllegalArgumentException("Nessun file PDF trovato nella cartella selezionata.");
            }

            Files.createDirectories(outputDirectory);
            String sanitizedPrefix = sanitizeOutputPrefix(outputPrefix);
            int totalGroups = (pdfFiles.size() + groupSize - 1) / groupSize;
            List<Path> createdFiles = new ArrayList<>(totalGroups);
            int groupDigits = Integer.toString(totalGroups).length();

            for (int start = 0, groupIndex = 1; start < pdfFiles.size(); start += groupSize, groupIndex++) {
                int endExclusive = Math.min(start + groupSize, pdfFiles.size());
                List<Path> groupFiles = pdfFiles.subList(start, endExclusive);
                Path firstPdf = groupFiles.get(0);
                Path lastPdf = groupFiles.get(groupFiles.size() - 1);
                String outputName = buildBatchOutputName(sanitizedPrefix, groupIndex, groupDigits, firstPdf, lastPdf);
                Path groupOutput = outputDirectory.resolve(outputName);
                mergeFiles(groupFiles, groupOutput, effectiveRotation, false);
                createdFiles.add(groupOutput);
            }

            BatchMergeResult result = new BatchMergeResult(outputDirectory, pdfFiles.size(), createdFiles.size(),
                    groupSize, createdFiles);
            AuditLogger.logSuccess("SERVICE_PDF_MERGE_BATCH", details, outputDirectory);
            return result;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_MERGE_BATCH", details, outputDirectory, ex);
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

    private void copyDocumentPages(PdfDocument source, PdfDocument target, RotationMode rotationMode,
            boolean forceA4PageSize)
            throws IOException {
        int totalPages = source.getNumberOfPages();
        for (int pageIndex = 1; pageIndex <= totalPages; pageIndex++) {
            PdfPage sourcePage = source.getPage(pageIndex);
            copySinglePage(sourcePage, target, rotationMode, forceA4PageSize);
        }
    }

    private void copySinglePage(PdfPage sourcePage, PdfDocument targetDocument, RotationMode rotationMode,
            boolean forceA4PageSize)
            throws IOException {
        boolean rotateRequested = rotationMode != RotationMode.NONE && !isA4Portrait(sourcePage);
        int sourceRotation = normalizeRotation(sourcePage.getRotation());
        int extraRotation = rotateRequested ? rotationMode.getDegrees() : 0;
        int totalRotation = normalizeRotation(sourceRotation + extraRotation);

        Rectangle originalSize = sourcePage.getPageSize();
        PageSize baseSize = new PageSize(originalSize.getWidth(), originalSize.getHeight());
        PageSize finalSize = forceA4PageSize ? new PageSize(A4_WIDTH, A4_HEIGHT)
                : (needsDimensionSwap(totalRotation)
                        ? new PageSize(baseSize.getHeight(), baseSize.getWidth())
                        : baseSize);

        PdfPage targetPage = targetDocument.addNewPage(finalSize);
        PdfCanvas canvas = new PdfCanvas(targetPage);
        PdfFormXObject pageContent = sourcePage.copyAsFormXObject(targetDocument);
        applyPageTransform(canvas, totalRotation, baseSize, finalSize, forceA4PageSize);
        canvas.addXObjectAt(pageContent, 0, 0);
    }

    private void mergeFiles(List<Path> pdfFiles, Path outputFile, RotationMode rotationMode, boolean forceA4PageSize)
            throws IOException {
        if (pdfFiles == null || pdfFiles.isEmpty()) {
            throw new IllegalArgumentException("Nessun file PDF da unire.");
        }

        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (PdfWriter writer = new PdfWriter(outputFile.toString(), App.writerProperties);
                PdfDocument targetDocument = new PdfDocument(writer)) {
            for (Path pdfFile : pdfFiles) {
                try (PdfDocument sourceDocument = new PdfDocument(new PdfReader(pdfFile.toString()))) {
                    copyDocumentPages(sourceDocument, targetDocument, rotationMode, forceA4PageSize);
                }
            }
        }
    }

    private static String buildBatchOutputName(String prefix, int groupIndex, int groupDigits, Path firstFile,
            Path lastFile) {
        String groupLabel = String.format(Locale.ROOT, "%0" + Math.max(groupDigits, 1) + "d", groupIndex);
        String firstName = stripPdfExtension(firstFile.getFileName().toString());
        String lastName = stripPdfExtension(lastFile.getFileName().toString());
        return prefix + "_" + groupLabel + "_" + firstName + "-" + lastName + ".pdf";
    }

    private static String stripPdfExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "pdf";
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return filename.substring(0, filename.length() - 4);
        }
        return filename;
    }

    private static String sanitizeOutputPrefix(String outputPrefix) {
        String trimmed = outputPrefix == null ? "" : outputPrefix.trim();
        if (trimmed.isEmpty()) {
            return "merge";
        }
        String sanitized = trimmed.replaceAll("[\\\\/:*?\"<>|]+", "_");
        return sanitized.isEmpty() ? "merge" : sanitized;
    }

    private static boolean needsDimensionSwap(int rotation) {
        return rotation % 180 != 0;
    }

    private static void applyPageTransform(PdfCanvas canvas, int rotation, PageSize baseSize, PageSize finalSize,
            boolean fitIntoTargetPage) {
        TransformData transform = computeTransformData(rotation, baseSize, finalSize, fitIntoTargetPage);
        canvas.concatMatrix(transform.a, transform.b, transform.c, transform.d, transform.e, transform.f);
    }

    private static TransformData computeTransformData(int rotation, PageSize baseSize, PageSize finalSize,
            boolean fitIntoTargetPage) {
        float a;
        float b;
        float c;
        float d;
        switch (rotation) {
            case 90:
                a = 0f;
                b = 1f;
                c = -1f;
                d = 0f;
                break;
            case 180:
                a = -1f;
                b = 0f;
                c = 0f;
                d = -1f;
                break;
            case 270:
                a = 0f;
                b = -1f;
                c = 1f;
                d = 0f;
                break;
            default:
                a = 1f;
                b = 0f;
                c = 0f;
                d = 1f;
                break;
        }

        float width = baseSize.getWidth();
        float height = baseSize.getHeight();

        float[] xs = new float[] { 0f, width, 0f, width };
        float[] ys = new float[] { 0f, 0f, height, height };

        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < xs.length; i++) {
            float tx = a * xs[i] + c * ys[i];
            float ty = b * xs[i] + d * ys[i];
            minX = Math.min(minX, tx);
            maxX = Math.max(maxX, tx);
            minY = Math.min(minY, ty);
            maxY = Math.max(maxY, ty);
        }

        float rotatedWidth = Math.max(maxX - minX, 0f);
        float rotatedHeight = Math.max(maxY - minY, 0f);

        float scale = 1f;
        if (fitIntoTargetPage && rotatedWidth > 0f && rotatedHeight > 0f) {
            float scaleX = finalSize.getWidth() / rotatedWidth;
            float scaleY = finalSize.getHeight() / rotatedHeight;
            scale = Math.min(scaleX, scaleY);
            if (!Float.isFinite(scale) || scale <= 0f) {
                scale = 1f;
            } else if (scale > 1f) {
                scale = 1f;
            }
        }

        float drawnWidth = rotatedWidth * scale;
        float drawnHeight = rotatedHeight * scale;
        float offsetX = (finalSize.getWidth() - drawnWidth) / 2f;
        float offsetY = (finalSize.getHeight() - drawnHeight) / 2f;

        float scaledA = a * scale;
        float scaledB = b * scale;
        float scaledC = c * scale;
        float scaledD = d * scale;
        float e = offsetX - (minX * scale);
        float f = offsetY - (minY * scale);

        return new TransformData(scaledA, scaledB, scaledC, scaledD, e, f);
    }

    private static final class TransformData {
        private final float a;
        private final float b;
        private final float c;
        private final float d;
        private final float e;
        private final float f;

        private TransformData(float a, float b, float c, float d, float e, float f) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
            this.f = f;
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
