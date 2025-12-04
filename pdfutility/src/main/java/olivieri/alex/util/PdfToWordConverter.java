package olivieri.alex.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;

import olivieri.alex.quality.AuditLogger;

import javax.imageio.ImageIO;
import java.math.BigInteger;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts PDF pages to a DOCX document by embedding each page as a high
 * resolution image, preserving the original layout.
 */
public class PdfToWordConverter {

    private static final float DEFAULT_DPI = 300f;
    private static final double EMU_PER_INCH = 914400d;
    private static final double EMU_PER_TWIP = EMU_PER_INCH / 1440d;
    private static final double EMU_PER_PIXEL = EMU_PER_INCH / DEFAULT_DPI;

    public Path convert(Path pdfPath, Path docxPath) throws IOException {
        String details = "input=" + pdfPath + ",output=" + docxPath;
        try {
            if (pdfPath == null || !Files.isRegularFile(pdfPath)) {
                throw new IllegalArgumentException("Percorso del PDF non valido.");
            }
            if (docxPath == null) {
                throw new IllegalArgumentException("Percorso del file Word non valido.");
            }
            Path parent = docxPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (PDDocument pdfDocument = PDDocument.load(pdfPath.toFile());
                    XWPFDocument wordDocument = new XWPFDocument()) {

                if (pdfDocument.getNumberOfPages() == 0) {
                    throw new IllegalArgumentException("Il PDF selezionato non contiene pagine.");
                }

                PDPage firstPage = pdfDocument.getPage(0);
                PDRectangle firstBox = resolvePageBox(firstPage);
                long availableWidthEmu = configureDocumentLayout(wordDocument, firstBox);
                PDFRenderer renderer = new PDFRenderer(pdfDocument);
                renderer.setSubsamplingAllowed(true);

                for (int pageIndex = 0; pageIndex < pdfDocument.getNumberOfPages(); pageIndex++) {
                    BufferedImage image = renderer.renderImageWithDPI(pageIndex, DEFAULT_DPI, ImageType.RGB);
                    int pixelWidth = image.getWidth();
                    int pixelHeight = image.getHeight();
                    byte[] imageBytes = toPng(image);
                    image.flush();
                    boolean startNewPage = pageIndex > 0;
                    insertImage(wordDocument, imageBytes, pageIndex, availableWidthEmu, pixelWidth, pixelHeight,
                            startNewPage);
                }

                try (OutputStream outputStream = Files.newOutputStream(docxPath)) {
                    wordDocument.write(outputStream);
                }
            }

            AuditLogger.logSuccess("SERVICE_PDF_TO_WORD", details, docxPath);
            return docxPath;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_TO_WORD", details, docxPath, ex);
            throw ex;
        }
    }

    public BatchResult convertDirectory(Path directory, Path outputDirectory) throws IOException {
        Path absoluteDirectory = directory != null ? directory.toAbsolutePath() : null;
        Path absoluteOutput = outputDirectory != null ? outputDirectory.toAbsolutePath() : null;
        String details = "directory=" + absoluteDirectory + ",output=" + absoluteOutput;
        Path targetDirectory = absoluteOutput;
        try {
            if (directory == null || !Files.isDirectory(directory)) {
                throw new IllegalArgumentException("Percorso della cartella non valido.");
            }
            List<Path> pdfFiles = listPdfFiles(directory);
            if (pdfFiles.isEmpty()) {
                throw new IllegalArgumentException("Nessun PDF trovato nella cartella selezionata.");
            }
            if (targetDirectory == null) {
                Path base = absoluteDirectory != null ? absoluteDirectory : directory.toAbsolutePath();
                Path parent = base.getParent() != null ? base.getParent() : base;
                String folderName = base.getFileName() != null ? base.getFileName().toString() : "converted";
                if (folderName.trim().isEmpty()) {
                    folderName = "converted";
                }
                targetDirectory = parent.resolve(folderName + "_word");
            }
            Files.createDirectories(targetDirectory);

            int converted = 0;
            for (Path pdf : pdfFiles) {
                Path outputDocx = targetDirectory.resolve(buildDocxFileName(pdf));
                convert(pdf, outputDocx);
                converted++;
            }

            AuditLogger.logSuccess("SERVICE_PDF_TO_WORD_DIR", details, targetDirectory);
            return new BatchResult(targetDirectory, converted);
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_TO_WORD_DIR", details, targetDirectory, ex);
            throw ex;
        }
    }

    private void insertImage(XWPFDocument document, byte[] imageData, int pageIndex, long availableWidthEmu,
            int pixelWidth, int pixelHeight, boolean pageBreakBefore) throws IOException {
        double intrinsicWidthEmu = pixelWidth * EMU_PER_PIXEL;
        double intrinsicHeightEmu = pixelHeight * EMU_PER_PIXEL;
        double scale = intrinsicWidthEmu > availableWidthEmu && availableWidthEmu > 0
                ? availableWidthEmu / intrinsicWidthEmu
                : 1.0;
        int widthEmu = (int) Math.round(intrinsicWidthEmu * scale);
        int heightEmu = (int) Math.round(intrinsicHeightEmu * scale);
        if (widthEmu <= 0 || heightEmu <= 0) {
            throw new IOException("Dimensioni pagina non valide durante la conversione.");
        }

        XWPFParagraph paragraph = document.createParagraph();
        if (pageBreakBefore) {
            paragraph.setPageBreak(true);
        }
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(0);
        XWPFRun run = paragraph.createRun();
        try (InputStream dataStream = new ByteArrayInputStream(imageData)) {
            run.addPicture(dataStream, XWPFDocument.PICTURE_TYPE_PNG, "page-" + (pageIndex + 1) + ".png", widthEmu,
                    heightEmu);
        } catch (Exception ex) {
            throw new IOException("Impossibile inserire l'immagine della pagina " + (pageIndex + 1), ex);
        }
    }

    private long configureDocumentLayout(XWPFDocument document, PDRectangle pageBox) {
        CTBody body = document.getDocument().getBody();
        if (!body.isSetSectPr()) {
            body.addNewSectPr();
        }
        CTSectPr sectPr = body.getSectPr();
        CTPageSz pageSz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        CTPageMar pageMar = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();

        long widthTwips = convertPointsToTwips(pageBox != null ? pageBox.getWidth() : 595f);
        long heightTwips = convertPointsToTwips(pageBox != null ? pageBox.getHeight() : 842f);
        if (widthTwips <= 0) {
            widthTwips = 11907;
        }
        if (heightTwips <= 0) {
            heightTwips = 16840;
        }

        pageSz.setW(BigInteger.valueOf(widthTwips));
        pageSz.setH(BigInteger.valueOf(heightTwips));
        pageSz.setOrient(widthTwips > heightTwips ? STPageOrientation.LANDSCAPE : STPageOrientation.PORTRAIT);
        pageMar.setLeft(BigInteger.ZERO);
        pageMar.setRight(BigInteger.ZERO);
        pageMar.setTop(BigInteger.ZERO);
        pageMar.setBottom(BigInteger.ZERO);
        if (pageMar.getHeader() == null) {
            pageMar.setHeader(BigInteger.ZERO);
        }
        if (pageMar.getFooter() == null) {
            pageMar.setFooter(BigInteger.ZERO);
        }

        long availableTwips = Math.max(widthTwips, 1L);
        return Math.round(availableTwips * EMU_PER_TWIP);
    }

    private long convertPointsToTwips(float points) {
        if (!Float.isFinite(points) || points <= 0) {
            return 1L;
        }
        double twips = points * 20d;
        return Math.max(1L, Math.round(twips));
    }

    private PDRectangle resolvePageBox(PDPage page) {
        if (page == null) {
            return null;
        }
        PDRectangle box = page.getCropBox();
        if (box == null || box.getWidth() <= 0 || box.getHeight() <= 0) {
            box = page.getMediaBox();
        }
        return box;
    }

    private List<Path> listPdfFiles(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        }
    }

    private String buildDocxFileName(Path pdfFile) {
        String filename = pdfFile.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String baseName = dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
        if (baseName.isEmpty()) {
            baseName = "document";
        }
        return baseName + "_word.docx";
    }

    private byte[] toPng(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", outputStream)) {
                throw new IOException("Formato immagine non supportato.");
            }
            return outputStream.toByteArray();
        }
    }

    public static final class BatchResult {
        private final Path outputDirectory;
        private final int convertedCount;

        public BatchResult(Path outputDirectory, int convertedCount) {
            this.outputDirectory = outputDirectory;
            this.convertedCount = convertedCount;
        }

        public Path getOutputDirectory() {
            return outputDirectory;
        }

        public int getConvertedCount() {
            return convertedCount;
        }
    }
}
