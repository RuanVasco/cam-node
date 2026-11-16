package com.ruanvascolabs.camnode.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import com.ruanvascolabs.camnode.support.MinioTestConfiguration;
import com.ruanvascolabs.camnode.support.PostgresTestConfiguration;
import com.ruanvascolabs.camnode.support.RingUploadClient;
import com.ruanvascolabs.camnode.support.TestPhotos;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfiguration.class, MinioTestConfiguration.class})
@TestPropertySource(properties = {
        "camnode.device.api-key=" + RingUploadClient.DEVICE_KEY,
        "camnode.admin.api-key=" + RingUploadClient.ADMIN_KEY
})
class AdminKeyIT {

    private final RestClient client;
    private final RingUploadClient uploader;
    private final JdbcTemplate jdbc;

    AdminKeyIT(@LocalServerPort int port, JdbcTemplate jdbc) {
        this.client = RestClient.create("http://localhost:" + port);
        this.uploader = new RingUploadClient(port);
        this.jdbc = jdbc;
    }

    @Test
    @DisplayName("SPEC-010 CA-8 (#7): without the admin key every trash route is 401 and the ring stays in the trash")
    void missingKey() {
        UUID id = uploadToTrash();

        assertThat(status(client.get().uri("/api/v1/trash"), null)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(status(client.get().uri("/api/v1/trash/{id}/photo", id), null)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(status(client.post().uri("/api/v1/trash/{id}/restore", id), null)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(status(client.delete().uri("/api/v1/trash/{id}", id), null)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(inTrash(id)).isTrue();
    }

    @Test
    @DisplayName("SPEC-010 CA-8 (#7): with a wrong admin key restore and purge are 401 and the ring stays in the trash")
    void wrongKey() {
        UUID id = uploadToTrash();

        assertThat(status(client.post().uri("/api/v1/trash/{id}/restore", id), "wrong")).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(status(client.delete().uri("/api/v1/trash/{id}", id), "wrong")).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(inTrash(id)).isTrue();
    }

    @Test
    @DisplayName("SPEC-010 #7: the history stays public")
    void historyNeedsNoKey() {
        assertThat(status(client.get().uri("/api/v1/rings"), null)).isEqualTo(HttpStatus.OK);
    }

    private UUID uploadToTrash() {
        uploader.upload(TestPhotos.jpeg(512), "image/jpeg", RingUploadClient.DEVICE_KEY);
        UUID id = jdbc.queryForObject("SELECT id FROM ring ORDER BY ringed_at DESC, id DESC LIMIT 1", UUID.class);
        assertThat(status(client.delete().uri("/api/v1/rings/{id}", id), null)).isEqualTo(HttpStatus.NO_CONTENT);
        return id;
    }

    private boolean inTrash(UUID id) {
        return jdbc.queryForObject("SELECT count(*) FROM ring WHERE id = ? AND removed_at IS NOT NULL", Integer.class, id) == 1;
    }

    private static HttpStatusCode status(RestClient.RequestHeadersSpec<?> request, String key) {
        return request
                .headers(headers -> {
                    if (key != null) {
                        headers.set(AdminKeyInterceptor.HEADER, key);
                    }
                })
                .exchange((req, response) -> response.getStatusCode());
    }
}
