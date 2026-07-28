package olivieri.alex.util;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FolderProgressiveCreatorTest {

    @Test
    public void createsFoldersWithThreeDigitProgressiveSuffix() throws Exception {
        Path baseDirectory = Files.createTempDirectory("folder-progressive-");
        FolderProgressiveCreator creator = new FolderProgressiveCreator();

        FolderProgressiveCreator.Result result = creator.create(baseDirectory, "Lotto_", 3);

        assertEquals(3, result.getCreatedCount());
        assertEquals(0, result.getSkippedCount());
        assertTrue(Files.isDirectory(baseDirectory.resolve("Lotto_001")));
        assertTrue(Files.isDirectory(baseDirectory.resolve("Lotto_002")));
        assertTrue(Files.isDirectory(baseDirectory.resolve("Lotto_003")));
    }

    @Test
    public void skipsExistingFolders() throws Exception {
        Path baseDirectory = Files.createTempDirectory("folder-progressive-");
        Files.createDirectory(baseDirectory.resolve("Lotto_002"));
        FolderProgressiveCreator creator = new FolderProgressiveCreator();

        FolderProgressiveCreator.Result result = creator.create(baseDirectory, "Lotto_", 2);

        assertEquals(1, result.getCreatedCount());
        assertEquals(1, result.getSkippedCount());
        assertTrue(result.hasWarnings());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCountsAboveThreeDigitRange() throws Exception {
        Path baseDirectory = Files.createTempDirectory("folder-progressive-");
        new FolderProgressiveCreator().create(baseDirectory, "Lotto_", 1000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidFolderNameCharactersInPrefix() throws Exception {
        Path baseDirectory = Files.createTempDirectory("folder-progressive-");
        new FolderProgressiveCreator().create(baseDirectory, "Lotto:", 1);
    }
}
