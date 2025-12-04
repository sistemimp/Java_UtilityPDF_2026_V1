package olivieri.alex.quality;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Centralized audit logger used to produce evidences for ISO 9001.
 */
public final class AuditLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final HexFormat HEX_FORMAT = HexFormat.of();
    private static final String LOG_PROPERTY = "pdfutility.audit.log";
    private static final Path DEFAULT_LOG = Paths.get("logs", "iso_audit.log");

    private AuditLogger() {
    }

    public static void logSuccess(String action, String details, Path output) {
        log(action, details, pathToString(output), "SUCCESS", output, null);
    }

    public static void logFailure(String action, String details, Path output, Throwable error) {
        log(action, details, pathToString(output), "FAILURE", output, error);
    }

    private static void log(String action, String details, String output, String status, Path outputPath,
            Throwable error) {
        Objects.requireNonNull(action, "action");
        String sanitizedAction = sanitize(action);
        String sanitizedDetails = sanitize(details);
        String sanitizedOutput = sanitize(output);
        String sanitizedStatus = sanitize(status);
        String user = sanitize(System.getProperty("user.name", "unknown"));
        String timestamp = FORMATTER.format(OffsetDateTime.now());

        String hash = "";
        if ("SUCCESS".equals(sanitizedStatus) && outputPath != null && Files.isRegularFile(outputPath)) {
            hash = computeSha256(outputPath);
        }

        String errorMessage = error != null ? sanitize(error.getMessage()) : "";
        String line = String.join(";", timestamp, user, sanitizedAction, sanitizedDetails, sanitizedOutput,
                sanitizedStatus, hash, errorMessage);
        writeLine(line);
    }

    private static void writeLine(String line) {
        Path logFile = resolveLogFile();
        try {
            Path parent = logFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            synchronized (AuditLogger.class) {
                Files.writeString(logFile, line + System.lineSeparator(), StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            }
        } catch (IOException ex) {
            System.err.println("Impossibile scrivere il log di audit: " + ex.getMessage());
        }
    }

    public static Path getLogFilePath() {
        return resolveLogFile();
    }

    private static Path resolveLogFile() {
        String custom = System.getProperty(LOG_PROPERTY);
        if (custom != null && !custom.trim().isEmpty()) {
            return Paths.get(custom.trim());
        }
        return DEFAULT_LOG;
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\r\\n]+", " ").replace(';', ',').trim();
    }

    private static String computeSha256(Path file) {
        try (InputStream inputStream = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HEX_FORMAT.formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException ex) {
            return "";
        }
    }

    private static String pathToString(Path path) {
        return path == null ? "" : path.toAbsolutePath().toString();
    }
}
