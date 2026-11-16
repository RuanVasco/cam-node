package com.ruanvascolabs.camnode.ring;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import com.ruanvascolabs.camnode.support.PostgresTestConfiguration;
import com.ruanvascolabs.camnode.support.RingUploadClient;
import com.ruanvascolabs.camnode.support.TestPhotos;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestConfiguration.class)
@TestPropertySource(properties = {
        "camnode.device.api-key=" + RingUploadClient.DEVICE_KEY,
        // Nothing listens on port 1, so every storage call fails like an unreachable MinIO.
        "camnode.photo-storage.endpoint=http://localhost:1"
})
class RingStorageUnavailableIT {

    private final RingUploadClient client;
    private final RestClient reader;
    private final RingRepository repository;

    RingStorageUnavailableIT(@LocalServerPort int port, RingRepository repository) {
        this.client = new RingUploadClient(port);
        this.reader = RestClient.create("http://localhost:" + port);
        this.repository = repository;
    }

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("SPEC-001 CA-6 (#6): unavailable photo storage answers 503 and records nothing")
    void storageUnavailable() {
        var response = client.upload(TestPhotos.jpeg(1024), "image/jpeg", RingUploadClient.DEVICE_KEY);

        assertThat(response.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("SPEC-002 #3: unavailable photo storage answers 503 when reading a photo")
    void storageUnavailableOnRead() {
        UUID id = repository.save(new Ring(Instant.parse("2026-11-20T12:00:00Z"), "any/photo.jpg", 1)).getId();

        var status = reader.get().uri("/api/v1/rings/{id}/photo", id)
                .exchange((request, response) -> response.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
