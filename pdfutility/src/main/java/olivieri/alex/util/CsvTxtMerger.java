package olivieri.alex.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import olivieri.alex.quality.AuditLogger;

/**
 * Utility that concatenates multiple CSV or TXT files into a single text file.
 */
public class CsvTxtMerger {

    /**
     * Merges the provided input files into {@code output}.
     *
     * @param inputs files to merge, in order
     * @param output destination file
     * @return the generated output path
     * @throws IOException              if IO errors occur
     * @throws IllegalArgumentException if the arguments are invalid
     */
    public Path merge(List<Path> inputs, Path output) throws IOException {
        String details = "inputs=" + inputs + ",output=" + output;
        try {
            if (inputs == null || inputs.isEmpty()) {
                throw new IllegalArgumentException("Nessun file da unire.");
            }
            if (output == null) {
                throw new IllegalArgumentException("Percorso di output non valido.");
            }

            for (Path input : inputs) {
                if (input == null || !Files.isRegularFile(input)) {
                    throw new IllegalArgumentException("File non valido: " + input);
                }
                String filename = input.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!filename.endsWith(".csv") && !filename.endsWith(".txt")) {
                    throw new IllegalArgumentException("Formato non supportato per: " + input.getFileName());
                }
            }

            Path parent = output.toAbsolutePath().getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                boolean wroteAnyLine = false;
                for (Path input : inputs) {
                    try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (wroteAnyLine) {
                                writer.newLine();
                            }
                            writer.write(line);
                            wroteAnyLine = true;
                        }
                    }
                }
            }

            AuditLogger.logSuccess("SERVICE_CSV_TXT_MERGE", details, output);
            return output;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_CSV_TXT_MERGE", details, output, ex);
            throw ex;
        }
    }
}
