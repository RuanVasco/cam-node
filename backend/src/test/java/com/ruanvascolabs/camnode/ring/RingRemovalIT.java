package com.ruanvascolabs.camnode.ring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import com.ruanvascolabs.camnode.photo.PhotoDeletionQueue;
import com.ruanvascolabs.camnode.photo.PhotoStorage;
import com.ruanvascolabs.camnode.ring.dto.RingPageResponse;
import com.ruanvascolabs.camnode.support.MinioTestConfiguration;
import com.ruanvascolabs.camnode.support.PostgresTestConfiguration;
import com.ruanvascolabs.camnode.support.RingUploadClient;
import com.ruanvascolabs.camnode.support.TestPhotos;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfiguration.class, MinioTestConfiguration.class})
@TestPropertySource(properties = {
        "camnode.device.api-key=" + RingUploadClient.DEVICE_KEY,
        "camnode.admin.api-key=" + RingUploadClient.ADMIN_KEY
})
class RingRemovalIT {

    private static final String ADMIN_HEADER = "X-Admin-Key";

    private final RestClient client;
    private final RingUploadClient uploader;
    private final RingRepository repository;
    private final JdbcTemplate jdbc;
    private final S3Client s3;
    private final PhotoStorage storage;
    private final PhotoDeletionQueue photoDeletions;

    RingRemovalIT(@LocalServerPort int port, RingRepository repository, JdbcTemplate jdbc, S3Client photoS3Client,
            PhotoStorage storage, PhotoDeletionQueue photoDeletions) {
        this.client = RestClient.create("http://localhost:" + port);
        this.uploader = new RingUploadClient(port);
        this.repository = repository;
        this.jdbc = jdbc;
        this.s3 = photoS3Client;
        this.storage = storage;
        this.photoDeletions = photoDeletions;
    }

    @BeforeEach
    void clean() {
        repository.deleteAll();
        jdbc.update("DELETE FROM photo_deletion");
    }

    @Test
    @DisplayName("SPEC-010 CA-1 (#2, #3): removal is 204, the ring leaves the history, the photo is 404 and deleted")
    void removesRing() {
        List<UUID> ids = uploadRings(2);
        UUID removed = ids.getFirst();
        String photoKey = repository.findById(removed).orElseThrow().getPhotoKey();

        assertThat(remove(removed)).isEqualTo(HttpStatus.NO_CONTENT);

        RingPageResponse page = client.get().uri("/api/v1/rings").retrieve().body(RingPageResponse.class);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).extracting(response -> response.id()).doesNotContain(removed);
        assertThat(photoStatus(removed)).isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> s3.headObject(r -> r.bucket(MinioTestConfiguration.BUCKET).key(photoKey)))
                .isInstanceOf(NoSuchKeyException.class);
        assertThat(pendingDeletions()).isZero();
    }

    @Test
    @DisplayName("SPEC-010 CA-7 (#2): removing the same ring twice answers 404 the second time")
    void removeTwice() {
        UUID id = uploadRings(1).getFirst();

        assertThat(remove(id)).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(remove(id)).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("SPEC-010 CA-6 (#6): three selected rings are removed at once")
    void removesSeveral() {
        List<UUID> ids = uploadRings(4);

        assertThat(removeAll(ids.subList(0, 3))).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(repository.findAll()).extracting(Ring::getId).containsExactly(ids.get(3));
    }

    @Test
    @DisplayName("SPEC-010 CA-6 (#6): 101 ids is 400 and nothing is removed")
    void moreThanHundred() {
        UUID id = uploadRings(1).getFirst();
        List<UUID> ids = IntStream.range(0, 100).mapToObj(i -> UUID.randomUUID()).collect(Collectors.toList());
        ids.add(id);

        assertThat(removeAll(ids)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(repository.existsById(id)).isTrue();
    }

    @Test
    @DisplayName("SPEC-010 #6: an empty list is 400")
    void emptyList() {
        assertThat(removeAll(List.of())).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("SPEC-010 #6: one unknown id is 404 and none of the others is removed")
    void unknownIdRemovesNothing() {
        List<UUID> ids = uploadRings(2);

        assertThat(removeAll(List.of(ids.get(0), UUID.randomUUID(), ids.get(1)))).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(repository.count()).isEqualTo(2);
        assertThat(pendingDeletions()).isZero();
    }

    @Test
    @DisplayName("SPEC-010 #6: a repeated id counts once")
    void repeatedId() {
        UUID id = uploadRings(1).getFirst();

        assertThat(removeAll(List.of(id, id))).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(repository.count()).isZero();
    }

    @Test
    @DisplayName("SPEC-010 CA-3 (#3): a queued photo is deleted from storage by the retry")
    void retryDeletesQueuedPhoto() {
        String key = "2026/11/16/queued.jpg";
        storage.store(key, TestPhotos.jpeg(64));
        jdbc.update("INSERT INTO photo_deletion (photo_key, queued_at) VALUES (?, ?)", key,
                Timestamp.from(Instant.parse("2026-11-16T12:00:00Z")));

        photoDeletions.retryPending();

        assertThatThrownBy(() -> s3.headObject(r -> r.bucket(MinioTestConfiguration.BUCKET).key(key)))
                .isInstanceOf(NoSuchKeyException.class);
        assertThat(pendingDeletions()).isZero();
    }

    @Test
    @DisplayName("SPEC-010 #6: a body that is not JSON is 400 with a PT-BR message")
    void malformedBody() {
        String body = client.post().uri("/api/v1/rings/removal")
                .header(ADMIN_HEADER, RingUploadClient.ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{not json")
                .exchange((request, response) -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    return new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                });

        assertThat(body).contains("Corpo da requisição em formato inválido.");
    }

    private List<UUID> uploadRings(int count) {
        for (int i = 0; i < count; i++) {
            uploader.upload(TestPhotos.jpeg(256), "image/jpeg", RingUploadClient.DEVICE_KEY);
        }
        return jdbc.queryForList("SELECT id FROM ring ORDER BY ringed_at, id", UUID.class);
    }

    private int pendingDeletions() {
        return jdbc.queryForObject("SELECT count(*) FROM photo_deletion", Integer.class);
    }

    private HttpStatusCode remove(UUID id) {
        return client.delete().uri("/api/v1/rings/{id}", id)
                .header(ADMIN_HEADER, RingUploadClient.ADMIN_KEY)
                .exchange((request, response) -> response.getStatusCode());
    }

    private HttpStatusCode removeAll(List<UUID> ids) {
        return client.post().uri("/api/v1/rings/removal")
                .header(ADMIN_HEADER, RingUploadClient.ADMIN_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new IdsBody(ids))
                .exchange((request, response) -> response.getStatusCode());
    }

    private HttpStatusCode photoStatus(UUID id) {
        return client.get().uri("/api/v1/rings/{id}/photo", id).exchange((request, response) -> response.getStatusCode());
    }

    private record IdsBody(List<UUID> ids) {
    }
}
