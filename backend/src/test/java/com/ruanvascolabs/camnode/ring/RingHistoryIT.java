package com.ruanvascolabs.camnode.ring;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import com.ruanvascolabs.camnode.ring.dto.RingPageResponse;
import com.ruanvascolabs.camnode.ring.dto.RingResponse;
import com.ruanvascolabs.camnode.support.MinioTestConfiguration;
import com.ruanvascolabs.camnode.support.PostgresTestConfiguration;
import com.ruanvascolabs.camnode.support.RingUploadClient;
import com.ruanvascolabs.camnode.support.TestPhotos;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfiguration.class, MinioTestConfiguration.class})
@TestPropertySource(properties = "camnode.device.api-key=" + RingUploadClient.DEVICE_KEY)
class RingHistoryIT {

    private static final Instant FIRST_RING = Instant.parse("2026-11-20T12:00:00Z");

    private final RingRepository repository;
    private final RingUploadClient uploader;
    private final RestClient client;

    RingHistoryIT(@LocalServerPort int port, RingRepository repository) {
        this.repository = repository;
        this.uploader = new RingUploadClient(port);
        this.client = RestClient.create("http://localhost:" + port);
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("SPEC-002 CA-1 (#1, #2): without parameters returns the 20 most recent, newest first")
    void defaultPage() {
        saveRingsOneMinuteApart(25);

        RingPageResponse page = client.get().uri("/api/v1/rings").retrieve().body(RingPageResponse.class);

        assertThat(page.totalElements()).isEqualTo(25);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.content()).hasSize(20);
        assertThat(page.content().getFirst().ringedAt()).isEqualTo(FIRST_RING.plusSeconds(24 * 60));
        assertThat(page.content()).extracting(RingResponse::ringedAt).isSortedAccordingTo((a, b) -> b.compareTo(a));
        assertThat(page.content().getFirst().photoUrl())
                .isEqualTo("/api/v1/rings/" + page.content().getFirst().id() + "/photo");
    }

    @Test
    @DisplayName("SPEC-002 CA-2 (#2): size above 100 is capped at 100")
    void sizeIsCapped() {
        saveRingsOneMinuteApart(101);

        RingPageResponse page = client.get().uri("/api/v1/rings?size=500").retrieve().body(RingPageResponse.class);

        assertThat(page.content()).hasSize(100);
        assertThat(page.size()).isEqualTo(100);
    }

    @Test
    @DisplayName("SPEC-002 #2: negative page is rejected")
    void negativePage() {
        assertThat(statusOf("/api/v1/rings?page=-1")).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bodyOf("/api/v1/rings?page=-1")).contains("Parâmetros inválidos.");
    }

    @Test
    @DisplayName("SPEC-002 #2: page above the maximum is rejected instead of failing")
    void pageAboveMaximum() {
        assertThat(statusOf("/api/v1/rings?page=100000000&size=100")).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bodyOf("/api/v1/rings?page=100000000&size=100")).contains("Parâmetros inválidos.");
    }

    @Test
    @DisplayName("SPEC-002 #2: size below 1 is rejected")
    void zeroSize() {
        assertThat(statusOf("/api/v1/rings?size=0")).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("SPEC-002 CA-3 (#3): photo is served as JPEG with an immutable cache header")
    void servesPhoto() {
        byte[] jpeg = TestPhotos.jpeg(4096);
        uploader.upload(jpeg, "image/jpeg", RingUploadClient.DEVICE_KEY);
        UUID id = repository.findAll().getFirst().getId();

        ResponseEntity<byte[]> response = client.get().uri("/api/v1/rings/{id}/photo", id).retrieve().toEntity(byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).hasToString("image/jpeg");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("max-age=31536000, public, immutable");
        assertThat(response.getBody()).isEqualTo(jpeg);
    }

    @Test
    @DisplayName("SPEC-002 CA-4 (#3): photo of an unknown ring is 404")
    void unknownPhoto() {
        assertThat(statusOf("/api/v1/rings/" + UUID.randomUUID() + "/photo")).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("SPEC-002 #3: ring whose photo object is missing from storage is 404, not a retryable 503")
    void missingPhotoObject() {
        UUID id = repository.save(new Ring(FIRST_RING, "missing/photo.jpg", 1)).getId();

        assertThat(statusOf("/api/v1/rings/" + id + "/photo")).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(bodyOf("/api/v1/rings/" + id + "/photo")).contains("Foto não encontrada.");
    }

    @Test
    @DisplayName("SPEC-002 #3: malformed ring id is rejected with a PT-BR message")
    void malformedId() {
        assertThat(statusOf("/api/v1/rings/not-a-uuid/photo")).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(bodyOf("/api/v1/rings/not-a-uuid/photo")).contains("Parâmetro em formato inválido.");
    }

    private void saveRingsOneMinuteApart(int count) {
        repository.saveAll(IntStream.range(0, count)
                .mapToObj(i -> new Ring(FIRST_RING.plusSeconds(i * 60L), "test/" + i + ".jpg", 1))
                .toList());
    }

    private String bodyOf(String uri) {
        return client.get().uri(uri).exchange((request, response) -> new String(response.getBody().readAllBytes(),
                StandardCharsets.UTF_8));
    }

    private HttpStatusCode statusOf(String uri) {
        return client.get().uri(uri).exchange((request, response) -> response.getStatusCode());
    }
}
