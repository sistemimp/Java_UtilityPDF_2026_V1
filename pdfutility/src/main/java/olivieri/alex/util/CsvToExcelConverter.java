package olivieri.alex.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import olivieri.alex.quality.AuditLogger;

/**
 * Converts CSV files to Excel workbooks, forcing every cell to be stored as
 * text.
 */
public class CsvToExcelConverter {

    /**
     * Converts the provided CSV file to an Excel workbook placed at
     * {@code excelFile}.
     *
     * @param csvFile   input CSV file
     * @param excelFile destination .xlsx file
     * @return the generated Excel path
     * @throws IOException              if IO errors occur
     * @throws IllegalArgumentException if the input paths are not valid
     */
    public Path convert(Path csvFile, Path excelFile) throws IOException {
        String details = "csv=" + csvFile + ",excel=" + excelFile;
        try {
            if (csvFile == null || !Files.isRegularFile(csvFile)) {
                throw new IllegalArgumentException("File CSV non valido.");
            }
            if (excelFile == null) {
                throw new IllegalArgumentException("Percorso Excel non valido.");
            }

            Path parent = excelFile.toAbsolutePath().getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8);
                    Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("CSV");
                DataFormat dataFormat = workbook.createDataFormat();
                CellStyle textStyle = workbook.createCellStyle();
                textStyle.setDataFormat(dataFormat.getFormat("@"));

                String firstLine = reader.readLine();
                if (firstLine != null && firstLine.startsWith("\uFEFF")) {
                    firstLine = firstLine.substring(1);
                }
                char delimiter = detectDelimiter(firstLine);

                int rowIndex = 0;
                String currentLine = firstLine;
                while (currentLine != null) {
                    Row row = sheet.createRow(rowIndex++);
                    List<String> cells = parseCsvLine(currentLine, delimiter);
                    for (int i = 0; i < cells.size(); i++) {
                        Cell cell = row.createCell(i, CellType.STRING);
                        cell.setCellStyle(textStyle);
                        cell.setCellValue(cells.get(i));
                    }
                    currentLine = reader.readLine();
                }

                try (OutputStream outputStream = Files.newOutputStream(excelFile)) {
                    workbook.write(outputStream);
                }
            }

            AuditLogger.logSuccess("SERVICE_CSV_TO_EXCEL", details, excelFile);
            return excelFile;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_CSV_TO_EXCEL", details, excelFile, ex);
            throw ex;
        }
    }

    private char detectDelimiter(String line) {
        if (line == null || line.isEmpty()) {
            return ';';
        }
        int commas = 0;
        int semicolons = 0;
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    i++; // escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (!inQuotes) {
                if (ch == ',') {
                    commas++;
                } else if (ch == ';') {
                    semicolons++;
                }
            }
        }
        if (semicolons >= commas) {
            return ';';
        }
        return ',';
    }

    private List<String> parseCsvLine(String line, char delimiter) {
        List<String> result = new ArrayList<>();
        if (line == null) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == delimiter && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString());
        return result;
    }
}
