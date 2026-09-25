package olivieri.alex.util;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CsvTxtMergerTest {

    @Test
    public void skipsEmptyLastLineWhileKeepingOtherLines() throws Exception {
        Path directory = Files.createTempDirectory("csv-txt-merger-");
        Path csv = directory.resolve("first.csv");
        Path txt = directory.resolve("second.txt");
        Path output = directory.resolve("merged.txt");

        Files.writeString(csv, "a\n\ncsv-last\n\n", StandardCharsets.UTF_8);
        Files.writeString(txt, "txt-first\n\n", StandardCharsets.UTF_8);

        new CsvTxtMerger().merge(List.of(csv, txt), output);

        String separator = System.lineSeparator();
        assertEquals("a" + separator + separator + "csv-last" + separator + "txt-first",
                Files.readString(output, StandardCharsets.UTF_8));
    }

    @Test
    public void readsWindows1252WhenUtf8IsInvalid() throws Exception {
        Path directory = Files.createTempDirectory("csv-txt-merger-encoding-");
        Path csv = directory.resolve("first.csv");
        Path txt = directory.resolve("second.txt");
        Path output = directory.resolve("merged.txt");

        Files.write(csv, new byte[] {'c', 'a', 'f', (byte) 0xE9, '\n', '\n'});
        Files.writeString(txt, "second", StandardCharsets.UTF_8);

        new CsvTxtMerger().merge(List.of(csv, txt), output);

        assertEquals("café" + System.lineSeparator() + "second",
                Files.readString(output, StandardCharsets.UTF_8));
    }
}
