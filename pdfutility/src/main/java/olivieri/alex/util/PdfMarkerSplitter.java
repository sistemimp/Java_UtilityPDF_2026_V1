package olivieri.alex.util;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.SimpleTextExtractionStrategy;

import olivieri.alex.App;
import olivieri.alex.quality.AuditLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Splits a PDF into multiple documents whenever a page contains a marker
 * string.
 */
public class PdfMarkerSplitter {

    public static class Result {
        private final Path outputDirectory;
        private final int documentCount;

        public Result(Path outputDirectory, int documentCount) {
            this.outputDirectory = outputDirectory;
            this.documentCount = documentCount;
        }

        public Path getOutputDirectory() {
            return outputDirectory;
        }

        public int getDocumentCount() {
            return documentCount;
        }
    }

    public Result splitByMarker(Path inputFile, Path outputBaseDir, String folderName, String marker,
            boolean caseSensitive, boolean appendIfDuplicate) throws IOException {
        String details = "input=" + inputFile + ",marker=" + marker + ",folder=" + folderName + ",append="
                + appendIfDuplicate;
        Path outputDirectory = null;
        try {
            if (inputFile == null || !Files.isRegularFile(inputFile)) {
                throw new IllegalArgumentException("File PDF non valido.");
            }
            if (outputBaseDir == null || !Files.isDirectory(outputBaseDir)) {
                throw new IllegalArgumentException("Cartella di destinazione non valida.");
            }
            if (folderName == null || folderName.trim().isEmpty()) {
                throw new IllegalArgumentException("Nome cartella di output non valido.");
            }
            if (marker == null || marker.trim().isEmpty()) {
                throw new IllegalArgumentException("Stringa di ricerca non valida.");
            }

            outputDirectory = outputBaseDir.resolve(folderName);
            Files.createDirectories(outputDirectory);

            Map<String, OutputContext> openContexts = new LinkedHashMap<>();
            Map<String, Integer> duplicateCounters = new HashMap<>();
            OutputContext currentContext = null;
            int documentsCreated = 0;

            try (PdfDocument sourceDocument = new PdfDocument(new PdfReader(inputFile.toString()))) {
                int totalPages = sourceDocument.getNumberOfPages();
                for (int pageIndex = 1; pageIndex <= totalPages; pageIndex++) {
                    String pageText = PdfTextExtractor.getTextFromPage(sourceDocument.getPage(pageIndex),
                            new SimpleTextExtractionStrategy());
                    String normalizedText = caseSensitive ? pageText : pageText.toLowerCase(Locale.ROOT);
                    String normalizedMarker = caseSensitive ? marker : marker.toLowerCase(Locale.ROOT);
                    boolean isMarkerPage = normalizedText.contains(normalizedMarker);

                    if (isMarkerPage) {
                        String baseName = deriveBaseName(pageText, marker, caseSensitive);
                        if (appendIfDuplicate) {
                            currentContext = openContexts.get(baseName);
                            if (currentContext == null) {
                                currentContext = createContext(outputDirectory, baseName, App.writerProperties);
                                openContexts.put(baseName, currentContext);
                                documentsCreated++;
                            }
                        } else {
                            int occurrence = duplicateCounters.merge(baseName, 1, Integer::sum);
                            String uniqueName = occurrence == 1 ? baseName : baseName + "_" + occurrence;
                            if (currentContext != null) {
                                currentContext.close();
                            }
                            currentContext = createContext(outputDirectory, uniqueName, App.writerProperties);
                            documentsCreated++;
                        }
                    }

                    if (currentContext == null) {
                        throw new IllegalArgumentException(
                                "La prima pagina del gruppo non contiene la stringa indicata. Verificare il file.");
                    }

                    PdfDocument destination = currentContext.getDocument();
                    sourceDocument.copyPagesTo(pageIndex, pageIndex, destination);
                    PageSize size = new PageSize(sourceDocument.getPage(pageIndex).getPageSize());
                    destination.getPage(destination.getNumberOfPages()).setMediaBox(size);
                }
            } finally {
                if (appendIfDuplicate) {
                    for (OutputContext context : openContexts.values()) {
                        context.close();
                    }
                } else if (currentContext != null) {
                    currentContext.close();
                }
            }

            if (documentsCreated == 0) {
                throw new IllegalArgumentException("La stringa specificata non e stata trovata nel documento.");
            }

            Result result = new Result(outputDirectory, documentsCreated);
            AuditLogger.logSuccess("SERVICE_PDF_MARKER_SPLIT", details, outputDirectory);
            return result;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_MARKER_SPLIT", details, outputDirectory, ex);
            throw ex;
        }
    }

    private String deriveBaseName(String pageText, String marker, boolean caseSensitive) {
        String searchText = caseSensitive ? pageText : pageText.toLowerCase(Locale.ROOT);
        String markerSearch = caseSensitive ? marker : marker.toLowerCase(Locale.ROOT);
        int index = searchText.indexOf(markerSearch);
        if (index < 0) {
            return sanitizeName(marker);
        }
        int start = index + marker.length();
        String remainder = pageText.substring(start).trim();
        int newline = remainder.indexOf('\n');
        if (newline >= 0) {
            remainder = remainder.substring(0, newline).trim();
        }
        return sanitizeName(remainder.isEmpty() ? marker : remainder);
    }

    private OutputContext createContext(Path outputDirectory, String baseName, WriterProperties writerProperties)
            throws IOException {
        String sanitized = sanitizeName(baseName);
        Path outputFile = outputDirectory.resolve(sanitized + ".pdf");
        PdfWriter writer = new PdfWriter(outputFile.toString(), writerProperties);
        PdfDocument document = new PdfDocument(writer);
        return new OutputContext(outputFile, writer, document);
    }

    private String sanitizeName(String value) {
        if (value == null) {
            return "segment";
        }
        String sanitized = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]+", "_");
        return sanitized.isEmpty() ? "segment" : sanitized;
    }

    private static class OutputContext implements AutoCloseable {
        private final PdfDocument document;
        private boolean closed;

        OutputContext(Path outputFile, PdfWriter writer, PdfDocument document) {
            this.document = document;
        }

        PdfDocument getDocument() {
            return document;
        }

        @Override
        public void close() {
            if (!closed) {
                document.close();
                closed = true;
            }
        }
    }
}
