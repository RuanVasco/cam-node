package com.ruanvascolabs.camnode.photo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ruanvascolabs.camnode.support.TestPhotos;

class JpegSignatureTest {

    @Test
    @DisplayName("SPEC-001 #3: accepts content starting with the JPEG signature")
    void acceptsJpeg() {
        assertThat(JpegSignature.matches(TestPhotos.jpeg(16))).isTrue();
    }

    @Test
    @DisplayName("SPEC-001 #3: rejects PNG content")
    void rejectsPng() {
        assertThat(JpegSignature.matches(TestPhotos.png())).isFalse();
    }

    @Test
    @DisplayName("SPEC-001 #3: rejects content shorter than the signature")
    void rejectsTooShort() {
        assertThat(JpegSignature.matches(new byte[] {(byte) 0xFF, (byte) 0xD8})).isFalse();
    }
}
