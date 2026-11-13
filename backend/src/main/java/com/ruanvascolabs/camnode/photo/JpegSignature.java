package com.ruanvascolabs.camnode.photo;

public final class JpegSignature {

    private JpegSignature() {
    }

    /** Rule: SPEC-001 #3. Checks the content itself (SOI marker plus a marker prefix), never the declared type. */
    public static boolean matches(byte[] content) {
        return content.length >= 3
                && content[0] == (byte) 0xFF
                && content[1] == (byte) 0xD8
                && content[2] == (byte) 0xFF;
    }
}
