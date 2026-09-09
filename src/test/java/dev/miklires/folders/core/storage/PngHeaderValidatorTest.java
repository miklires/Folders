package dev.miklires.folders.core.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PngHeaderValidatorTest {
    @TempDir
    Path directory;

    @Test
    void acceptsSaneDimensionsBeforeFullDecode() throws Exception {
        Path png = writeHeader("safe.png", 64, 64);
        assertDoesNotThrow(() -> PngHeaderValidator.validate(png, 1024, 512));
    }

    @Test
    void rejectsHugeDimensionsBeforeAnImageDecoderCanAllocateMemory() throws Exception {
        Path png = writeHeader("huge.png", 100_000, 100_000);
        assertThrows(IOException.class, () -> PngHeaderValidator.validate(png, 1024, 512));
    }

    @Test
    void rejectsFilesThatOnlyHaveAPngExtension() throws Exception {
        Path fake = Files.writeString(directory.resolve("fake.png"), "not a png file at all");
        assertThrows(IOException.class, () -> PngHeaderValidator.validate(fake, 1024, 512));
    }

    private Path writeHeader(String name, int width, int height) throws Exception {
        ByteBuffer bytes = ByteBuffer.allocate(24);
        bytes.put(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        bytes.putInt(13);
        bytes.put(new byte[]{0x49, 0x48, 0x44, 0x52});
        bytes.putInt(width);
        bytes.putInt(height);
        return Files.write(directory.resolve(name), bytes.array());
    }
}
