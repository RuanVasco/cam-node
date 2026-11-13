package com.ruanvascolabs.camnode.ring;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import com.ruanvascolabs.camnode.support.MinioTestConfiguration;
import com.ruanvascolabs.camnode.support.PostgresTestConfiguration;
import com.ruanvascolabs.camnode.support.RingUploadClient;
import com.ruanvascolabs.camnode.support.TestPhotos;

import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfiguration.class, MinioTestConfiguration.class})
@TestPropertySource(properties = "camnode.device.api-key=" + RingUploadClient.DEVICE_KEY)
class RingRegistrationIT {

    private final RingRepository repository;
    private final S3Client s3;
    private final RingUploadClient client;

    RingRegistrationIT(@LocalServerPort int port, RingRepository repository, S3Client s3) {
        this.repository = repository;
        this.s3 = s3;
        this.client = new RingUploadClient(port);
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("SPEC-001 CA-1 (#1, #2, #5): valid JPEG with the right key creates a ring")
    void registersRing() {
        Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);

        var response = client.upload(TestPhotos.jpeg(300 * 1024), "image/jpeg", RingUploadClient.DEVICE_KEY);

        assertThat(response.status()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.body()).contains("\"id\"", "\"ringedAt\"", "/api/v1/rings/");
        assertThat(response.headers().getLocation()).isNotNull();

        Ring ring = repository.findAll().getFirst();
        assertThat(ring.getRingedAt()).isBetween(before, Instant.now());
        assertThat(s3.headObject(r -> r.bucket(MinioTestConfiguration.BUCKET).key(ring.getPhotoKey())).contentLength())
                .isEqualTo(300 * 1024);
    }

    @Test
    @DisplayName("SPEC-001 CA-2 (#1): missing device key is rejected")
    void rejectsMissingKey() {
        var response = client.upload(TestPhotos.jpeg(1024), "image/jpeg", null);

        assertThat(response.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("SPEC-001 CA-2 (#1): wrong device key is rejected")
    void rejectsWrongKey() {
        var response = client.upload(TestPhotos.jpeg(1024), "image/jpeg", "wrong-key");

        assertThat(response.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.headers().getContentType()).hasToString("application/problem+json");
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("SPEC-001 CA-3 (#2): request without the photo part is rejected")
    void rejectsMissingPhoto() {
        var response = client.upload(null, "image/jpeg", RingUploadClient.DEVICE_KEY);

        assertThat(response.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("SPEC-001 CA-4 (#3): PNG declared as JPEG is rejected")
    void rejectsPngDeclaredAsJpeg() {
        var response = client.upload(TestPhotos.png(), "image/jpeg", RingUploadClient.DEVICE_KEY);

        assertThat(response.status()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("SPEC-001 CA-5 (#4): photo above 2 MB is rejected")
    void rejectsOversizedPhoto() {
        var response = client.upload(TestPhotos.jpeg(2560 * 1024), "image/jpeg", RingUploadClient.DEVICE_KEY);

        assertThat(response.status()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(repository.count()).isZero();
    }
}
