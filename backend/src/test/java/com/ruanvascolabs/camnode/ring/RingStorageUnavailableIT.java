package com.ruanvascolabs.camnode.ring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

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

    @LocalServerPort
    private int port;

    @Autowired
    private RingRepository repository;

    @Test
    @DisplayName("SPEC-001 CA-6 (#6): unavailable photo storage answers 503 and records nothing")
    void storageUnavailable() {
        var response = new RingUploadClient(port).upload(TestPhotos.jpeg(1024), "image/jpeg", RingUploadClient.DEVICE_KEY);

        assertThat(response.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(repository.count()).isZero();
    }
}
