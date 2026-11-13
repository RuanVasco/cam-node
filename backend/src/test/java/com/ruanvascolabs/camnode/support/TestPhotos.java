package com.ruanvascolabs.camnode.support;

import java.util.Arrays;

public final class TestPhotos {

    private TestPhotos() {
    }

    public static byte[] jpeg(int sizeBytes) {
        byte[] content = new byte[sizeBytes];
        Arrays.fill(content, (byte) 0x42);
        content[0] = (byte) 0xFF;
        content[1] = (byte) 0xD8;
        content[2] = (byte) 0xFF;
        content[3] = (byte) 0xE0;
        return content;
    }

    public static byte[] png() {
        return new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};
    }
}
