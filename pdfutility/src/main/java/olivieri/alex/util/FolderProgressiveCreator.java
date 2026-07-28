package olivieri.alex.util;

import olivieri.alex.quality.AuditLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FolderProgressiveCreator {
    private static final int WIDTH = 3;
    private static final int MAX_FOLDERS = 999;

    public static final class Result {
        private final int createdCount;
        private final int skippedCount;
        private final Path baseDirectory;
        private final List<String> warnings;

        public Result(int createdCount, int skippedCount, Path baseDirectory, List<String> warnings) {
            this.createdCount = createdCount;
            this.skippedCount = skippedCount;
            this.baseDirectory = baseDirectory;
            this.warnings = List.copyOf(warnings);
        }

        public int getCreatedCount() {
            return createdCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public Path getBaseDirectory() {
            return baseDirectory;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }

    public Result create(Path baseDirectory, String prefix, int folderCount) throws IOException {
        String details = "baseDirectory=" + baseDirectory + ",prefix=" + prefix + ",folderCount=" + folderCount;
        try {
            validate(baseDirectory, prefix, folderCount);

            int created = 0;
            int skipped = 0;
            List<String> warnings = new ArrayList<>();
            for (int index = 1; index <= folderCount; index++) {
                String folderName = prefix + String.format("%0" + WIDTH + "d", index);
                Path target = baseDirectory.resolve(folderName);
                if (Files.exists(target)) {
                    skipped++;
                    warnings.add("Cartella gia esistente, saltata: " + folderName);
                    continue;
                }
                Files.createDirectory(target);
                created++;
            }

            Result result = new Result(created, skipped, baseDirectory, warnings);
            AuditLogger.logSuccess("SERVICE_FOLDER_PROGRESSIVE_CREATE", details, baseDirectory);
            return result;
        } catch (IOException | RuntimeException ex) {
            AuditLogger.logFailure("SERVICE_FOLDER_PROGRESSIVE_CREATE", details, baseDirectory, ex);
            throw ex;
        }
    }

    private void validate(Path baseDirectory, String prefix, int folderCount) {
        if (baseDirectory == null || !Files.isDirectory(baseDirectory)) {
            throw new IllegalArgumentException("Seleziona una cartella di destinazione valida.");
        }
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Inserisci il prefisso delle cartelle.");
        }
        if (!prefix.equals(prefix.trim())) {
            throw new IllegalArgumentException("Il prefisso non deve iniziare o finire con spazi.");
        }
        if (prefix.matches(".*[\\\\/:*?\"<>|].*")) {
            throw new IllegalArgumentException("Il prefisso contiene caratteri non validi per il nome cartella.");
        }
        if (folderCount < 1) {
            throw new IllegalArgumentException("Il numero di cartelle deve essere almeno 1.");
        }
        if (folderCount > MAX_FOLDERS) {
            throw new IllegalArgumentException("Il numero massimo di cartelle e 999.");
        }
    }
}
