package dev.miklires.folders.core.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;

public final class PngHeaderValidator {
    private static final byte[] SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] IHDR = {0x49, 0x48, 0x44, 0x52};
    private static final int HEADER_BYTES = 24;

    private PngHeaderValidator() {
    }

    public static void validate(Path path, long maximumBytes, int maximumDimension) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)) {
            throw new IOException("not a regular file");
        }
        long size = Files.size(path);
        if (size < HEADER_BYTES || size > maximumBytes) {
            throw new IOException("PNG size is outside the allowed range");
        }

        byte[] header;
        try (InputStream input = Files.newInputStream(path)) {
            header = input.readNBytes(HEADER_BYTES);
        }
        if (header.length != HEADER_BYTES
                || !Arrays.equals(SIGNATURE, Arrays.copyOfRange(header, 0, 8))
                || readInt(header, 8) != 13
                || !Arrays.equals(IHDR, Arrays.copyOfRange(header, 12, 16))) {
            throw new IOException("invalid PNG header");
        }

        long width = Integer.toUnsignedLong(readInt(header, 16));
        long height = Integer.toUnsignedLong(readInt(header, 20));
        if (width < 1 || height < 1 || width > maximumDimension || height > maximumDimension) {
            throw new IOException("PNG dimensions exceed " + maximumDimension + "px");
        }
    }

    private static int readInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) << 24
                | (bytes[offset + 1] & 0xFF) << 16
                | (bytes[offset + 2] & 0xFF) << 8
                | bytes[offset + 3] & 0xFF;
    }
}
