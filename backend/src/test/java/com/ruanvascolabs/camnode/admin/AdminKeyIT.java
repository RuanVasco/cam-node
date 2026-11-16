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
import org.springframework.http.MediaType;
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
    @DisplayName("SPEC-010 CA-8 (#7): without the admin key the removal is 401 and the ring stays")
    void missingKey() {
        UUID id = upload();

        assertThat(removeStatus(id, null)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exists(id)).isTrue();
    }

    @Test
    @DisplayName("SPEC-010 CA-8 (#7): with a wrong admin key both removal routes are 401 and the ring stays")
    void wrongKey() {
        UUID id = upload();

        assertThat(removeStatus(id, "wrong")).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(bulkRemoveStatus(id, "wrong")).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exists(id)).isTrue();
    }

    @Test
    @DisplayName("SPEC-010 #7: reads stay public")
    void readsNeedNoKey() {
        HttpStatusCode status = client.get().uri("/api/v1/rings").exchange((request, r) -> r.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.OK);
    }

    private UUID upload() {
        uploader.upload(TestPhotos.jpeg(512), "image/jpeg", RingUploadClient.DEVICE_KEY);
        return jdbc.queryForObject("SELECT id FROM ring ORDER BY ringed_at DESC, id DESC LIMIT 1", UUID.class);
    }

    private boolean exists(UUID id) {
        return jdbc.queryForObject("SELECT count(*) FROM ring WHERE id = ?", Integer.class, id) == 1;
    }

    private HttpStatusCode removeStatus(UUID id, String key) {
        return client.delete().uri("/api/v1/rings/{id}", id)
                .headers(headers -> {
                    if (key != null) {
                        headers.set(AdminKeyInterceptor.HEADER, key);
                    }
                })
                .exchange((request, response) -> response.getStatusCode());
    }

    private HttpStatusCode bulkRemoveStatus(UUID id, String key) {
        return client.post().uri("/api/v1/rings/removal")
                .header(AdminKeyInterceptor.HEADER, key)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"ids\":[\"" + id + "\"]}")
                .exchange((request, response) -> response.getStatusCode());
    }
}
