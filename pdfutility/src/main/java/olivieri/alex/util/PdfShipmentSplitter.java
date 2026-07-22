package olivieri.alex.util;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import olivieri.alex.App;
import olivieri.alex.quality.AuditLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Splits a PDF into smaller homogeneous files by grouping a fixed number of
 * shipments. Each shipment starts on a page containing the provided marker.
 */
public class PdfShipmentSplitter {

    public static class Result {
        private final Path outputDirectory;
        private final int outputFileCount;
        private final int shipmentCount;

        public Result(Path outputDirectory, int outputFileCount, int shipmentCount) {
            this.outputDirectory = outputDirectory;
            this.outputFileCount = outputFileCount;
            this.shipmentCount = shipmentCount;
        }

        public Path getOutputDirectory() {
            return outputDirectory;
        }

        public int getOutputFileCount() {
            return outputFileCount;
        }

        public int getShipmentCount() {
            return shipmentCount;
        }
    }

    public Result splitByShipmentCount(Path inputFile, Path outputBaseDir, String folderName, String outputPrefix,
            String marker, boolean caseSensitive, int shipmentsPerFile) throws IOException {
        String details = "input=" + inputFile + ",marker=" + marker + ",folder=" + folderName + ",prefix="
                + outputPrefix + ",shipmentsPerFile=" + shipmentsPerFile;
        Path outputDirectory = null;
        try {
            validateInputs(inputFile, outputBaseDir, folderName, outputPrefix, marker, shipmentsPerFile);

            outputDirectory = outputBaseDir.resolve(folderName);
            Files.createDirectories(outputDirectory);

            try (PdfDocument sourceDocument = new PdfDocument(App.newPdfReader(inputFile));
                    PDDocument pdfBoxDocument = PDDocument.load(inputFile.toFile())) {
                List<Integer> shipmentStartPages = detectShipmentStartPages(sourceDocument, pdfBoxDocument, marker,
                        caseSensitive);
                int totalPages = sourceDocument.getNumberOfPages();
                int shipmentCount = shipmentStartPages.size();
                int outputCount = 0;

                for (int shipmentIndex = 0; shipmentIndex < shipmentCount; shipmentIndex += shipmentsPerFile) {
                    int startPage = shipmentStartPages.get(shipmentIndex);
                    int endShipmentIndexExclusive = Math.min(shipmentIndex + shipmentsPerFile, shipmentCount);
                    int endPage = endShipmentIndexExclusive < shipmentCount
                            ? shipmentStartPages.get(endShipmentIndexExclusive) - 1
                            : totalPages;
                    Path outputFile = outputDirectory.resolve(buildOutputFileName(outputPrefix, outputCount + 1,
                            shipmentIndex + 1, endShipmentIndexExclusive));
                    copyPageRange(sourceDocument, startPage, endPage, outputFile);
                    outputCount++;
                }

                Result result = new Result(outputDirectory, outputCount, shipmentCount);
                AuditLogger.logSuccess("SERVICE_PDF_SHIPMENT_SPLIT", details, outputDirectory);
                return result;
            }
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_SHIPMENT_SPLIT", details, outputDirectory, ex);
            throw ex;
        }
    }

    private void validateInputs(Path inputFile, Path outputBaseDir, String folderName, String outputPrefix,
            String marker, int shipmentsPerFile) {
        if (inputFile == null || !Files.isRegularFile(inputFile)) {
            throw new IllegalArgumentException("File PDF non valido.");
        }
        if (outputBaseDir == null || !Files.isDirectory(outputBaseDir)) {
            throw new IllegalArgumentException("Cartella di destinazione non valida.");
        }
        if (folderName == null || folderName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome cartella di output non valido.");
        }
        if (outputPrefix == null || outputPrefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Prefisso file output non valido.");
        }
        if (marker == null || marker.trim().isEmpty()) {
            throw new IllegalArgumentException("Stringa di ricerca non valida.");
        }
        if (shipmentsPerFile < 1) {
            throw new IllegalArgumentException("Il numero di invii per file deve essere almeno 1.");
        }
    }

    private List<Integer> detectShipmentStartPages(PdfDocument sourceDocument, PDDocument pdfBoxDocument, String marker,
            boolean caseSensitive) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        int totalPages = sourceDocument.getNumberOfPages();
        String normalizedMarker = normalize(marker, caseSensitive);
        List<Integer> startPages = new ArrayList<>();

        for (int pageIndex = 1; pageIndex <= totalPages; pageIndex++) {
            String pageText = extractPageText(pdfBoxDocument, stripper, pageIndex);
            if (normalize(pageText, caseSensitive).contains(normalizedMarker)) {
                startPages.add(pageIndex);
            }
        }

        if (startPages.isEmpty()) {
            throw new IllegalArgumentException("La stringa specificata non e stata trovata nel documento.");
        }
        if (startPages.get(0) != 1) {
            throw new IllegalArgumentException(
                    "La prima pagina non contiene il marker dell'invio. Verificare il PDF sorgente.");
        }
        return startPages;
    }

    private void copyPageRange(PdfDocument sourceDocument, int startPage, int endPage, Path outputFile)
            throws IOException {
        try (PdfWriter writer = new PdfWriter(outputFile.toString(), App.writerProperties);
                PdfDocument destinationDocument = new PdfDocument(writer)) {
            sourceDocument.copyPagesTo(startPage, endPage, destinationDocument);
            for (int destinationPageIndex = 1; destinationPageIndex <= destinationDocument.getNumberOfPages();
                    destinationPageIndex++) {
                int sourcePageIndex = startPage + destinationPageIndex - 1;
                PageSize size = new PageSize(sourceDocument.getPage(sourcePageIndex).getPageSize());
                destinationDocument.getPage(destinationPageIndex).setMediaBox(size);
            }
        }
    }

    private String buildOutputFileName(String prefix, int outputIndex, int startShipment, int endShipment) {
        return sanitizeName(prefix) + "_" + String.format(Locale.ROOT, "%03d", outputIndex) + "_invii_"
                + startShipment + "-" + endShipment + ".pdf";
    }

    private String sanitizeName(String value) {
        if (value == null) {
            return "invii";
        }
        String sanitized = value.trim().replaceAll("[^a-zA-Z0-9_-]+", "_");
        return sanitized.isEmpty() ? "invii" : sanitized;
    }

    private String normalize(String value, boolean caseSensitive) {
        if (value == null) {
            return "";
        }
        return caseSensitive ? value : value.toLowerCase(Locale.ROOT);
    }

    private String extractPageText(PDDocument pdfBoxDocument, PDFTextStripper stripper, int pageIndex)
            throws IOException {
        try {
            stripper.setStartPage(pageIndex);
            stripper.setEndPage(pageIndex);
            return stripper.getText(pdfBoxDocument);
        } catch (IOException ex) {
            throw new IOException("Impossibile leggere il testo dalla pagina " + pageIndex + ".", ex);
        }
    }
}
