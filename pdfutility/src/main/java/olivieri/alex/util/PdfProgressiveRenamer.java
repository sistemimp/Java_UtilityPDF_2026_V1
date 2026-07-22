package olivieri.alex.util;

import olivieri.alex.quality.AuditLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class PdfProgressiveRenamer {

    public static final class Result {
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

    public Result rename(Path directory, int width) throws IOException {
        String details = "directory=" + directory + ",width=" + width;
        try {
            if (directory == null || !Files.isDirectory(directory)) {
                throw new IllegalArgumentException("Cartella non valida.");
            }
            if (width < 1) {
                throw new IllegalArgumentException("La lunghezza X deve essere almeno 1.");
            }

            List<Path> files = Files.list(directory)
                    .filter(Files::isRegularFile)
                    .filter(this::isPdf)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();

            if (files.isEmpty()) {
                throw new IllegalArgumentException("Nessun file PDF trovato nella cartella selezionata.");
            }

            List<String> warnings = new ArrayList<>();
            if (files.size() == 1) {
                Path source = files.get(0);
                String originalName = source.getFileName().toString();
                String baseName = extractBaseName(originalName);
                String finalBaseName = leftPadWithZeros(baseName, width);
                Path target = directory.resolve(finalBaseName + ".pdf");
                if (!target.equals(source)) {
                    if (Files.exists(target)) {
                        warnings.add("Destinazione già esistente, file saltato: " + target.getFileName());
                        return new Result(0, warnings);
                    }
                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
                Result result = new Result(1, warnings);
                AuditLogger.logSuccess("SERVICE_PDF_PROGRESSIVE_RENAME", details, directory);
                return result;
            }

            int renamed = 0;
            for (Path source : files) {
                String originalName = source.getFileName().toString();
                String baseName = extractBaseName(originalName);
                String finalBaseName = leftPadWithZeros(baseName, width);
                String finalName = finalBaseName + ".pdf";
                Path target = directory.resolve(finalName);

                if (target.equals(source)) {
                    renamed++;
                    continue;
                }

                if (Files.exists(target)) {
                    warnings.add("Destinazione già esistente, file saltato: " + finalName);
                    continue;
                }

                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                renamed++;
            }

            Result result = new Result(renamed, warnings);
            AuditLogger.logSuccess("SERVICE_PDF_PROGRESSIVE_RENAME", details, directory);
            return result;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_PDF_PROGRESSIVE_RENAME", details, directory, ex);
            throw ex;
        }
    }

    private boolean isPdf(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private String extractBaseName(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }

    private String leftPadWithZeros(String value, int width) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() >= width) {
            return normalized;
        }
        return "0".repeat(width - normalized.length()) + normalized;
    }
}
