package olivieri.alex.util;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.utils.PdfMerger;

import olivieri.alex.App;
import olivieri.alex.quality.AuditLogger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renames PDF files in a directory based on a CSV mapping. CSV expected format:
 * original_filename;new_filename (without path, extension optional).
 */
public class PdfCsvRenamer {

    public static class Result {
        private final int renamedCount;
        private final List<String> warnings;

        public Result(int renamedCount, List<String> warnings) {
            this.renamedCount = renamedCount;
            this.warnings = warnings;
        }

        public int getRenamedCount() {
            return renamedCount;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }

    /**
     * Applies the CSV mapping to rename PDF files in the target directory.
     *
     * @param directory directory that contains the PDFs
     * @param csvFile   CSV file with two columns: original name, new name
     * @return renaming result
     * @throws IOException              if IO operations fail
     * @throws IllegalArgumentException if inputs are invalid
     */
    public Result renameFromCsv(Path directory, Path csvFile) throws IOException {
        String details = "directory=" + directory + ",csv=" + csvFile;
        try {
            if (directory == null || !Files.isDirectory(directory)) {
                throw new IllegalArgumentException("Cartella PDF non valida.");
            }
            if (csvFile == null || !Files.isRegularFile(csvFile)) {
                throw new IllegalArgumentException("File CSV non valido.");
            }

            List<CsvEntry> entries = readCsv(csvFile);
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("Il file CSV non contiene dati validi.");
            }

            Map<String, List<String>> targetsToSources = new LinkedHashMap<>();
            for (CsvEntry entry : entries) {
                String sourceName = ensurePdfExtension(entry.original());
                String targetName = ensurePdfExtension(entry.renamed());
                targetsToSources.computeIfAbsent(targetName, key -> new ArrayList<>()).add(sourceName);
            }

            List<String> warnings = new ArrayList<>();
            int renamed = 0;

            for (Map.Entry<String, List<String>> entry : targetsToSources.entrySet()) {
                String targetName = entry.getKey();
                Path targetPath = directory.resolve(targetName);

                List<Path> pendingSources = new ArrayList<>();
                for (String sourceName : entry.getValue()) {
                    Path sourcePath = directory.resolve(sourceName);
                    if (Files.isRegularFile(sourcePath)) {
                        pendingSources.add(sourcePath);
                    } else {
                        warnings.add("File non trovato: " + sourceName);
                    }
                }

                boolean baseExists = Files.isRegularFile(targetPath);
                if (!baseExists) {
                    Path baseSource = findAndRemoveFirstExisting(pendingSources);
                    if (baseSource != null && !baseSource.equals(targetPath)) {
                        Files.move(baseSource, targetPath);
                        baseExists = true;
                    } else if (baseSource != null) {
                        baseExists = true;
                    }
                }

                if (!baseExists) {
                    continue;
                }

                for (Path additional : pendingSources) {
                    if (!Files.isRegularFile(additional) || targetPath.equals(additional)) {
                        continue;
                    }
                    appendPdf(additional, targetPath);
                    if (!targetPath.equals(additional)) {
                        Files.deleteIfExists(additional);
                    }
                }

                addAnchorageStamp(targetPath);
                renamed++;
            }

            Result result = new Result(renamed, warnings);
            AuditLogger.logSuccess("SERVICE_PDF_CSV_RENAME", details, directory);
            return result;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_CSV_RENAME", details, directory, ex);
            throw ex;
        }
    }

    private List<CsvEntry> readCsv(Path csvFile) throws IOException {
        List<CsvEntry> entries = new ArrayList<>();
        try (Reader reader = new InputStreamReader(Files.newInputStream(csvFile), StandardCharsets.UTF_8)) {
            StringBuilder buffer = new StringBuilder();
            int read;
            while ((read = reader.read()) != -1) {
                buffer.append((char) read);
            }
            String[] lines = buffer.toString().split("\\R");
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = null;
                if (line.contains(";")) {
                    parts = line.split(";", 2);
                }
                if (line.contains(",")) {
                    parts = line.split(",", 2);
                }

                if (parts.length != 2) {
                    continue;
                }
                String original = parts[0].trim();
                String renamed = parts[1].trim();
                if (original.isEmpty() || renamed.isEmpty()) {
                    continue;
                }
                entries.add(new CsvEntry(original, renamed));
            }
        }
        return entries;
    }

    private void appendPdf(Path source, Path target) throws IOException {
        Path tempFile = createTempFileNear(target, "merge_", ".pdf");
        try (PdfWriter writer = new PdfWriter(tempFile.toString(), App.writerProperties);
                PdfDocument outputDocument = new PdfDocument(writer);
                PdfDocument targetDocument = new PdfDocument(new PdfReader(target.toString()));
                PdfDocument sourceDocument = new PdfDocument(new PdfReader(source.toString()))) {
            PdfMerger merger = new PdfMerger(outputDocument);
            merger.merge(targetDocument, 1, targetDocument.getNumberOfPages());
            merger.merge(sourceDocument, 1, sourceDocument.getNumberOfPages());
        }
        Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void addAnchorageStamp(Path pdfPath) throws IOException {
        Path tempFile = createTempFileNear(pdfPath, "anchorage_", ".pdf");
        String text = "anchorage->" + pdfPath.getFileName();
        try (PdfDocument pdfDocument = new PdfDocument(new PdfReader(pdfPath.toString()),
                new PdfWriter(tempFile.toString(), App.writerProperties))) {
            PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
                PdfPage page = pdfDocument.getPage(i);
                PdfCanvas canvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pdfDocument);
                canvas.beginText();
                canvas.setFontAndSize(font, 1f);
                canvas.moveText(0, 0);
                canvas.showText(text);
                canvas.endText();
                canvas.release();
            }
        }
        Files.move(tempFile, pdfPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private Path createTempFileNear(Path reference, String prefix, String suffix) throws IOException {
        Path parent = reference.getParent();
        if (parent != null) {
            return Files.createTempFile(parent, prefix, suffix);
        }
        return Files.createTempFile(prefix, suffix);
    }

    private Path findAndRemoveFirstExisting(List<Path> sources) {
        for (int i = 0; i < sources.size(); i++) {
            Path candidate = sources.get(i);
            if (Files.isRegularFile(candidate)) {
                sources.remove(i);
                return candidate;
            }
        }
        return null;
    }

    private String ensurePdfExtension(String name) {
        if (name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return name;
        }
        return name + ".pdf";
    }

    private record CsvEntry(String original, String renamed) {
    }
}
