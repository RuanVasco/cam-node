package com.ruanvascolabs.camnode.ring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import com.ruanvascolabs.camnode.photo.PhotoDeletionQueue;
import com.ruanvascolabs.camnode.photo.PhotoStorage;
import com.ruanvascolabs.camnode.ring.dto.RingPageResponse;
import com.ruanvascolabs.camnode.ring.dto.RingResponse;
import com.ruanvascolabs.camnode.ring.dto.TrashPageResponse;
import com.ruanvascolabs.camnode.ring.dto.TrashedRingResponse;
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
class TrashIT {

    private static final String ADMIN_HEADER = "X-Admin-Key";

    private final RestClient client;
    private final RingUploadClient uploader;
    private final RingRepository repository;
    private final JdbcTemplate jdbc;
    private final S3Client s3;
    private final PhotoStorage storage;
    private final PhotoDeletionQueue photoDeletions;

    TrashIT(@LocalServerPort int port, RingRepository repository, JdbcTemplate jdbc, S3Client photoS3Client,
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
    @DisplayName("SPEC-010 CA-11 (#10): the trash lists the most recently removed first, with both times and the photo")
    void listsMostRecentlyRemovedFirst() {
        List<UUID> ids = uploadRings(3);
        trash(ids.get(0), "2026-11-23T20:05:00Z");
        trash(ids.get(1), "2026-11-23T20:01:00Z");

        TrashPageResponse page = client.get().uri("/api/v1/trash").header(ADMIN_HEADER, RingUploadClient.ADMIN_KEY)
                .retrieve().body(TrashPageResponse.class);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(TrashedRingResponse::id).containsExactly(ids.get(0), ids.get(1));
        TrashedRingResponse first = page.content().getFirst();
        assertThat(first.removedAt()).isEqualTo(Instant.parse("2026-11-23T20:05:00Z"));
        assertThat(first.ringedAt()).isEqualTo(repository.findById(ids.get(0)).orElseThrow().getRingedAt());
        assertThat(first.photoUrl()).isEqualTo("/api/v1/trash/" + ids.get(0) + "/photo");
    }

    @Test
    @DisplayName("SPEC-010 #10: the trash photo is served with the key and never cached; a ring in the history has none")
    void servesTrashPhoto() {
        List<UUID> ids = uploadRings(2);
        trash(ids.get(0), "2026-11-23T20:05:00Z");

        var photo = client.get().uri("/api/v1/trash/{id}/photo", ids.get(0)).header(ADMIN_HEADER, RingUploadClient.ADMIN_KEY)
                .retrieve().toEntity(byte[].class);

        assertThat(photo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(photo.getBody()).hasSize(256);
        assertThat(photo.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(trashStatus(client.get().uri("/api/v1/trash/{id}/photo", ids.get(1)))).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("SPEC-010 CA-12 (#11): a restored ring is back in the history with the same id, time and photo")
    void restores() {
        UUID id = uploadRings(1).getFirst();
        RingResponse before = history().content().getFirst();
        trash(id, "2026-11-23T20:05:00Z");

        assertThat(trashStatus(client.post().uri("/api/v1/trash/{id}/restore", id))).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(history().content()).containsExactly(before);
        assertThat(client.get().uri(before.photoUrl()).retrieve().toEntity(byte[].class).getBody()).hasSize(256);
        assertThat(trashList().totalElements()).isZero();
    }

    @Test
    @DisplayName("SPEC-010 #11: restoring a ring that is not in the trash is 404")
    void restoreOutsideTrash() {
        UUID id = uploadRings(1).getFirst();

        assertThat(trashStatus(client.post().uri("/api/v1/trash/{id}/restore", id))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(trashStatus(client.post().uri("/api/v1/trash/{id}/restore", UUID.randomUUID())))
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("SPEC-010 CA-7 (#3, #12): purging deletes the ring and its photo; purging or restoring again is 404")
    void purges() {
        UUID id = uploadRings(1).getFirst();
        String photoKey = repository.findById(id).orElseThrow().getPhotoKey();
        trash(id, "2026-11-23T20:05:00Z");

        assertThat(trashStatus(client.delete().uri("/api/v1/trash/{id}", id))).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(repository.existsById(id)).isFalse();
        assertThatThrownBy(() -> s3.headObject(r -> r.bucket(MinioTestConfiguration.BUCKET).key(photoKey)))
                .isInstanceOf(NoSuchKeyException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM photo_deletion", Integer.class)).isZero();
        assertThat(trashStatus(client.delete().uri("/api/v1/trash/{id}", id))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(trashStatus(client.post().uri("/api/v1/trash/{id}/restore", id))).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("SPEC-010 #12: a ring still in the history can not be purged")
    void purgeOutsideTrash() {
        UUID id = uploadRings(1).getFirst();

        assertThat(trashStatus(client.delete().uri("/api/v1/trash/{id}", id))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(history().content()).extracting(RingResponse::id).containsExactly(id);
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
        assertThat(jdbc.queryForObject("SELECT count(*) FROM photo_deletion", Integer.class)).isZero();
    }

    private List<UUID> uploadRings(int count) {
        for (int i = 0; i < count; i++) {
            uploader.upload(TestPhotos.jpeg(256), "image/jpeg", RingUploadClient.DEVICE_KEY);
        }
        return jdbc.queryForList("SELECT id FROM ring ORDER BY ringed_at, id", UUID.class);
    }

    // Removal time set directly: two removals through the API can land on the same microsecond.
    private void trash(UUID id, String removedAt) {
        jdbc.update("UPDATE ring SET removed_at = ? WHERE id = ?", Timestamp.from(Instant.parse(removedAt)), id);
    }

    private RingPageResponse history() {
        return client.get().uri("/api/v1/rings").retrieve().body(RingPageResponse.class);
    }

    private TrashPageResponse trashList() {
        return client.get().uri("/api/v1/trash").header(ADMIN_HEADER, RingUploadClient.ADMIN_KEY)
                .retrieve().body(TrashPageResponse.class);
    }

    private static HttpStatusCode trashStatus(RestClient.RequestHeadersSpec<?> request) {
        return request.header(ADMIN_HEADER, RingUploadClient.ADMIN_KEY)
                .exchange((req, response) -> response.getStatusCode());
    }
}
