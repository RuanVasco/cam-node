package com.ruanvascolabs.camnode.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import com.ruanvascolabs.camnode.support.MinioTestConfiguration;
import com.ruanvascolabs.camnode.support.PostgresTestConfiguration;
import com.ruanvascolabs.camnode.support.RingUploadClient;
import com.ruanvascolabs.camnode.support.TestPhotos;

/**
 * Own admin key, so the context is not shared with other classes (the lockout is global state), and discarded
 * afterwards, since it stays locked for 15 min.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfiguration.class, MinioTestConfiguration.class})
@TestPropertySource(properties = {
        "camnode.device.api-key=" + RingUploadClient.DEVICE_KEY,
        "camnode.admin.api-key=" + AdminKeyLockoutIT.ADMIN_KEY
})
@DirtiesContext
class AdminKeyLockoutIT {

    static final String ADMIN_KEY = "lockout-admin-key";

    private final RestClient client;
    private final RingUploadClient uploader;
    private final JdbcTemplate jdbc;

    AdminKeyLockoutIT(@LocalServerPort int port, JdbcTemplate jdbc) {
        // JDK client on purpose: the Apache one on the classpath silently waits out Retry-After on a 429.
        this.client = RestClient.builder().requestFactory(new JdkClientHttpRequestFactory())
                .baseUrl("http://localhost:" + port).build();
        this.uploader = new RingUploadClient(port);
        this.jdbc = jdbc;
    }

    @Test
    @DisplayName("SPEC-010 CA-9 (#8): after 5 wrong keys the sixth attempt is 429 even with the right key")
    void lockedAfterFiveFailures() {
        uploader.upload(TestPhotos.jpeg(512), "image/jpeg", RingUploadClient.DEVICE_KEY);
        UUID id = jdbc.queryForObject("SELECT id FROM ring ORDER BY ringed_at DESC, id DESC LIMIT 1", UUID.class);
        jdbc.update("UPDATE ring SET removed_at = now() WHERE id = ?", id);
        for (int i = 0; i < 5; i++) {
            assertThat(purge(id, "wrong").status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        Answer answer = purge(id, ADMIN_KEY);

        assertThat(answer.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(Long.parseLong(answer.retryAfter())).isBetween(1L, 900L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ring WHERE id = ?", Integer.class, id)).isEqualTo(1);
    }

    private Answer purge(UUID id, String key) {
        return client.delete().uri("/api/v1/trash/{id}", id)
                .header(AdminKeyInterceptor.HEADER, key)
                .exchange((request, r) -> new Answer(r.getStatusCode(), r.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)));
    }

    private record Answer(HttpStatusCode status, String retryAfter) {
    }
}
