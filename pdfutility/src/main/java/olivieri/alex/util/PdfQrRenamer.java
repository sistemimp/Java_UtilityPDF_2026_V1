package olivieri.alex.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renames and copies PDF files based on the QR code detected on the first page.
 */
public class PdfQrRenamer {
    private static final int RENDER_DPI = 200;
    private static final Map<DecodeHintType, Object> QR_HINTS;
    private static final Map<DecodeHintType, Object> C39_HINTS;
    private static final Map<DecodeHintType, Object> I25_HINTS;

    static {
        Map<DecodeHintType, Object> qrHints = new EnumMap<>(DecodeHintType.class);
        qrHints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
        qrHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        QR_HINTS = Collections.unmodifiableMap(qrHints);

        Map<DecodeHintType, Object> c39Hints = new EnumMap<>(DecodeHintType.class);
        c39Hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.CODE_39));
        c39Hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        C39_HINTS = Collections.unmodifiableMap(c39Hints);

        Map<DecodeHintType, Object> i25Hints = new EnumMap<>(DecodeHintType.class);
        i25Hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.ITF));
        i25Hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        I25_HINTS = Collections.unmodifiableMap(i25Hints);
    }

    public Result renameByQr(Path inputDirectory, Path outputDirectory) throws IOException {
        if (inputDirectory == null || outputDirectory == null) {
            throw new IllegalArgumentException("Cartelle di input e output obbligatorie.");
        }
        if (!Files.isDirectory(inputDirectory)) {
            throw new IllegalArgumentException("La cartella di input non esiste.");
        }
        Files.createDirectories(outputDirectory);
        Path scartiDirectory = outputDirectory.resolve("scarti");
        Files.createDirectories(scartiDirectory);

        List<String> warnings = new ArrayList<>();
        int scanned = 0;
        int copied = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputDirectory)) {
            for (Path candidate : stream) {
                if (!Files.isRegularFile(candidate)) {
                    continue;
                }
                String filename = candidate.getFileName().toString();
                if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                    continue;
                }
                scanned++;
                String qrValue = extractQrFromFirstPage(candidate);
                if (qrValue == null || qrValue.trim().isEmpty()) {
                    warnings.add("QR/C39 non individuato: " + filename + " → spostato in scarti");
                    moveToScarti(candidate, scartiDirectory);
                    continue;
                }
                String safeName = buildCombinedBaseName(filename, qrValue);
                Path targetPath = resolveTargetPath(outputDirectory, safeName);
                Files.copy(candidate, targetPath, StandardCopyOption.REPLACE_EXISTING);
                copied++;
            }
        }

        if (scanned == 0) {
            warnings.add("Nessun PDF trovato nella cartella di input.");
        }

        return new Result(scanned, copied, warnings);
    }

    private String extractQrFromFirstPage(Path pdfPath) throws IOException {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            if (document.getNumberOfPages() < 1) {
                return null;
            }
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, RENDER_DPI, ImageType.RGB);
            try {
                return decodeFromImage(image);
            } finally {
                image.flush();
            }
        }
    }

    private String decodeFromImage(BufferedImage image) {
        String result = decodeWithHints(image, QR_HINTS);
        if (result != null) {
            return result;
        }
        result = decodeWithHints(image, C39_HINTS);
        if (result != null) {
            return result;
        }
        return decodeWithHints(image, I25_HINTS);
    }

    private String decodeWithHints(BufferedImage image, Map<DecodeHintType, Object> hints) {
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            return new MultiFormatReader().decode(bitmap, hints).getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    private void moveToScarti(Path original, Path scartiDirectory) throws IOException {
        Path target = scartiDirectory.resolve(original.getFileName());
        int counter = 1;
        while (Files.exists(target)) {
            String filename = stripExtension(original.getFileName().toString());
            String sanitized = buildSafeName(filename);
            if (sanitized.isEmpty()) {
                sanitized = "scarto";
            }
            target = scartiDirectory.resolve(sanitized + "_" + counter + ".pdf");
            counter++;
        }
        Files.move(original, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private Path resolveTargetPath(Path outputDirectory, String baseName) {
        String suffix = ".pdf";
        Path candidate = outputDirectory.resolve(baseName + suffix);
        int counter = 1;
        while (Files.exists(candidate)) {
            candidate = outputDirectory.resolve(baseName + "_" + counter + suffix);
            counter++;
        }
        return candidate;
    }

    private String buildSafeName(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.replaceAll("[^a-zA-Z0-9_-]+", "_");
    }

    private String buildCombinedBaseName(String originalFilename, String qrValue) {
        String originalBase = buildSafeName(stripExtension(originalFilename));
        String qrBase = buildSafeName(qrValue);
        if (!qrBase.isEmpty() && !originalBase.isEmpty()) {
            return qrBase + "_" + originalBase;
        }
        if (!originalBase.isEmpty()) {
            return originalBase;
        }
        if (!qrBase.isEmpty()) {
            return qrBase;
        }
        return "qr_documento";
    }

    private String stripExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0) {
            return filename;
        }
        return filename.substring(0, dotIndex);
    }

    public static final class Result {
        private final int scannedCount;
        private final int copiedCount;
        private final List<String> warnings;

        public Result(int scannedCount, int copiedCount, List<String> warnings) {
            this.scannedCount = scannedCount;
            this.copiedCount = copiedCount;
            this.warnings = List.copyOf(warnings);
        }

        public int getScannedCount() {
            return scannedCount;
        }

        public int getCopiedCount() {
            return copiedCount;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}
