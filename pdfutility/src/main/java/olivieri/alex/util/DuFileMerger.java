package olivieri.alex.util;

import olivieri.alex.quality.AuditLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Utility that merges DU text files, keeping the full first file (master) and
 * skipping the first line in subsequent files.
 */
public final class DuFileMerger {

    /**
     * Merges DU files into a single output file.
     *
     * @param inputs DU files to merge
     * @param output output DU file
     * @return output path
     * @throws IOException              if IO operations fail
     * @throws IllegalArgumentException if inputs are invalid
     */
    public Path merge(List<Path> inputs, Path output) throws IOException {
        String details = "inputs=" + inputs + ",output=" + output;
        try {
            if (inputs == null || inputs.isEmpty()) {
                throw new IllegalArgumentException("Nessun file DU da unire.");
            }
            if (output == null) {
                throw new IllegalArgumentException("Percorso di output non valido.");
            }

            List<Path> orderedInputs = validateAndOrder(inputs);
            Path normalizedOutput = output.toAbsolutePath().normalize();
            for (Path input : orderedInputs) {
                if (normalizedOutput.equals(input.toAbsolutePath().normalize())) {
                    throw new IllegalArgumentException("Il file di output non puo essere uno dei file sorgente.");
                }
            }

            Path parent = normalizedOutput.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(normalizedOutput, StandardCharsets.UTF_8)) {
                boolean wroteAnyLine = false;
                for (int index = 0; index < orderedInputs.size(); index++) {
                    Path input = orderedInputs.get(index);
                    boolean skipFirstLine = index > 0;
                    try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
                        String line;
                        boolean firstLine = true;
                        while ((line = reader.readLine()) != null) {
                            if (skipFirstLine && firstLine) {
                                firstLine = false;
                                continue;
                            }
                            if (wroteAnyLine) {
                                writer.newLine();
                            }
                            writer.write(line);
                            wroteAnyLine = true;
                            firstLine = false;
                        }
                    }
                }
            }

            AuditLogger.logSuccess("SERVICE_DU_MERGE", details, normalizedOutput);
            return normalizedOutput;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_DU_MERGE", details, output, ex);
            throw ex;
        }
    }

    private List<Path> validateAndOrder(List<Path> inputs) {
        List<Path> validated = new ArrayList<>();
        for (Path input : inputs) {
            if (input == null || !Files.isRegularFile(input)) {
                throw new IllegalArgumentException("File non valido: " + input);
            }
            if (!hasDuExtension(input)) {
                String name = input.getFileName() != null ? input.getFileName().toString() : input.toString();
                throw new IllegalArgumentException("Formato non supportato per " + name + ". Usa file .DU.");
            }
            validated.add(input);
        }

        validated.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)));

        int masterIndex = -1;
        for (int i = 0; i < validated.size(); i++) {
            String name = validated.get(i).getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.contains("_01_")) {
                masterIndex = i;
                break;
            }
        }
        if (masterIndex > 0) {
            Path master = validated.remove(masterIndex);
            validated.add(0, master);
        }

        return validated;
    }

    private boolean hasDuExtension(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return filename.endsWith(".du");
    }
}
