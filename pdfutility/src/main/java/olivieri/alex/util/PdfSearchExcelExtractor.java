package olivieri.alex.util;

import olivieri.alex.quality.AuditLogger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Extracts line data found after a search key inside a PDF and exports the
 * parsed values to an Excel workbook.
 */
public final class PdfSearchExcelExtractor {

    public ExtractionResult extract(Path pdfFile, String searchKey, boolean caseSensitive, Path excelFile)
            throws IOException {
        String details = "pdf=" + pdfFile + ",excel=" + excelFile + ",searchKey=" + searchKey + ",caseSensitive="
                + caseSensitive;
        try {
            if (pdfFile == null || !Files.isRegularFile(pdfFile)) {
                throw new IllegalArgumentException("File PDF non valido.");
            }
            if (excelFile == null) {
                throw new IllegalArgumentException("Percorso Excel non valido.");
            }
            String normalizedKey = normalizeKey(searchKey);
            if (normalizedKey.isEmpty()) {
                throw new IllegalArgumentException("Inserisci la chiave di ricerca.");
            }

            Path parent = excelFile.toAbsolutePath().getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            List<ExtractedRow> rows = readRows(pdfFile, normalizedKey, caseSensitive);
            writeWorkbook(pdfFile, excelFile, normalizedKey, rows);

            ExtractionResult result = new ExtractionResult(excelFile, rows.size());
            AuditLogger.logSuccess("SERVICE_PDF_SEARCH_TO_EXCEL", details + ",matches=" + rows.size(), excelFile);
            return result;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_SEARCH_TO_EXCEL", details, excelFile, ex);
            throw ex;
        }
    }

    private List<ExtractedRow> readRows(Path pdfFile, String searchKey, boolean caseSensitive) throws IOException {
        List<ExtractedRow> rows = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfFile.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document);
                String[] lines = text.split("\\R");
                for (String line : lines) {
                    ExtractedRow row = extractLine(pdfFile, page, line, searchKey, caseSensitive);
                    if (row != null) {
                        rows.add(row);
                    }
                }
            }
        }
        return rows;
    }

    private ExtractedRow extractLine(Path pdfFile, int pageNumber, String line, String searchKey, boolean caseSensitive) {
        if (line == null) {
            return null;
        }
        String effectiveLine = line.trim();
        if (effectiveLine.isEmpty()) {
            return null;
        }

        int matchIndex = indexOf(effectiveLine, searchKey, caseSensitive);
        if (matchIndex < 0) {
            return null;
        }

        String rawValue = effectiveLine.substring(matchIndex + searchKey.length()).trim();
        rawValue = rawValue.replaceFirst("^[\\s:;=\\-]+", "");
        rawValue = rawValue.replaceFirst("^\\|+", "").trim();

        List<String> values = new ArrayList<>();
        if (!rawValue.isEmpty()) {
            String[] tokens = rawValue.split("\\|", -1);
            for (String token : tokens) {
                values.add(token.trim());
            }
        }

        return new ExtractedRow(pdfFile.getFileName().toString(), pageNumber, effectiveLine, rawValue, values);
    }

    private void writeWorkbook(Path pdfFile, Path excelFile, String searchKey, List<ExtractedRow> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Estrazioni");
            DataFormat dataFormat = workbook.createDataFormat();
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(dataFormat.getFormat("@"));

            int maxValues = 0;
            for (ExtractedRow row : rows) {
                maxValues = Math.max(maxValues, row.values().size());
            }

            Row header = sheet.createRow(0);
            writeCell(header, 0, "PDF", textStyle);
            writeCell(header, 1, "Pagina", textStyle);
            writeCell(header, 2, "Chiave", textStyle);
            writeCell(header, 3, "Riga completa", textStyle);
            writeCell(header, 4, "Valore grezzo", textStyle);
            for (int i = 0; i < maxValues; i++) {
                writeCell(header, 5 + i, "Campo " + (i + 1), textStyle);
            }

            int rowIndex = 1;
            for (ExtractedRow rowData : rows) {
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, rowData.pdfName(), textStyle);
                writeCell(row, 1, Integer.toString(rowData.pageNumber()), textStyle);
                writeCell(row, 2, searchKey, textStyle);
                writeCell(row, 3, rowData.fullLine(), textStyle);
                writeCell(row, 4, rowData.rawValue(), textStyle);
                for (int i = 0; i < rowData.values().size(); i++) {
                    writeCell(row, 5 + i, rowData.values().get(i), textStyle);
                }
            }

            for (int i = 0; i < 5 + maxValues; i++) {
                sheet.autoSizeColumn(i);
            }

            try (OutputStream outputStream = Files.newOutputStream(excelFile)) {
                workbook.write(outputStream);
            }
        }
    }

    private void writeCell(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index, CellType.STRING);
        cell.setCellStyle(style);
        cell.setCellValue(value == null ? "" : value);
    }

    private int indexOf(String source, String target, boolean caseSensitive) {
        if (caseSensitive) {
            return source.indexOf(target);
        }
        return source.toLowerCase(Locale.ROOT).indexOf(target.toLowerCase(Locale.ROOT));
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim();
    }

    public record ExtractionResult(Path outputFile, int extractedRows) {
    }

    private record ExtractedRow(String pdfName, int pageNumber, String fullLine, String rawValue, List<String> values) {
    }
}
