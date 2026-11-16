package com.ruanvascolabs.camnode.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

import com.ruanvascolabs.camnode.support.MinioTestConfiguration;
import com.ruanvascolabs.camnode.support.PostgresTestConfiguration;
import com.ruanvascolabs.camnode.support.RingUploadClient;
import com.ruanvascolabs.camnode.support.SseTestClient;
import com.ruanvascolabs.camnode.support.TestPhotos;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfiguration.class, MinioTestConfiguration.class})
@TestPropertySource(properties = {
        "camnode.device.api-key=" + RingUploadClient.DEVICE_KEY,
        "camnode.admin.api-key=" + RingUploadClient.ADMIN_KEY,
        // Short interval so CA-6 does not wait 65 s in real time.
        "camnode.notification.heartbeat-interval=200ms"
})
class RingStreamIT {

    private static final Duration WAIT = Duration.ofSeconds(2);

    private final int port;
    private final SseRingNotifier notifier;

    RingStreamIT(@LocalServerPort int port, SseRingNotifier notifier) {
        this.port = port;
        this.notifier = notifier;
    }

    @Test
    @DisplayName("SPEC-002 CA-5 (#4): a subscriber receives the ring event within 2 s")
    void receivesRingEvent() throws InterruptedException {
        try (SseTestClient stream = new SseTestClient(port)) {
            assertThat(stream.awaitLine(":", WAIT)).as("subscribed").isNotNull();

            var upload = new RingUploadClient(port).upload(TestPhotos.jpeg(1024), "image/jpeg", RingUploadClient.DEVICE_KEY);

            assertThat(stream.awaitLine("event:", WAIT)).isEqualTo("event:ring");
            String data = stream.awaitLine("data:", WAIT);
            assertThat(data).isEqualTo("data:" + upload.body());
        }
    }

    @Test
    @DisplayName("SPEC-010 CA-4 (#4): a subscriber receives ring-removed within 2 s of a removal")
    void receivesRingRemovedEvent() throws InterruptedException {
        try (SseTestClient stream = new SseTestClient(port)) {
            assertThat(stream.awaitLine(":", WAIT)).as("subscribed").isNotNull();
            UUID first = uploadAndAwait(stream);
            UUID second = uploadAndAwait(stream);

            HttpStatusCode status = RestClient.create("http://localhost:" + port).post().uri("/api/v1/rings/removal")
                    .header("X-Admin-Key", RingUploadClient.ADMIN_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"ids\":[\"" + first + "\",\"" + second + "\"]}")
                    .exchange((request, response) -> response.getStatusCode());

            assertThat(status).isEqualTo(HttpStatus.NO_CONTENT);
            for (UUID id : List.of(first, second)) {
                assertThat(stream.awaitLine("event:", WAIT)).isEqualTo("event:ring-removed");
                assertThat(stream.awaitLine("data:", WAIT)).isEqualTo("data:{\"id\":\"" + id + "\"}");
            }
        }
    }

    // Waits for the ring event too, so it can not arrive after the removal events.
    private UUID uploadAndAwait(SseTestClient stream) throws InterruptedException {
        var upload = new RingUploadClient(port).upload(TestPhotos.jpeg(1024), "image/jpeg", RingUploadClient.DEVICE_KEY);
        assertThat(upload.status()).isEqualTo(HttpStatus.CREATED);
        assertThat(stream.awaitLine("event:", WAIT)).isEqualTo("event:ring");
        stream.awaitLine("data:", WAIT);
        String path = upload.headers().getLocation().getPath();
        return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
    }

    @Test
    @DisplayName("SPEC-002 CA-6 (#5): an idle subscriber gets periodic heartbeats and stays connected")
    void receivesHeartbeats() throws InterruptedException {
        try (SseTestClient stream = new SseTestClient(port)) {
            assertThat(stream.awaitLine(":heartbeat", WAIT)).isNotNull();
            assertThat(stream.awaitLine(":heartbeat", WAIT)).isNotNull();
            assertThat(stream.isOpen()).isTrue();
        }
    }

    @Test
    @DisplayName("SPEC-002 #4: a subscriber that disconnects is dropped")
    void dropsDisconnectedSubscriber() throws InterruptedException {
        try (SseTestClient stream = new SseTestClient(port)) {
            stream.awaitLine(":heartbeat", WAIT);
        }

        try (SseTestClient other = new SseTestClient(port)) {
            // Writing heartbeats to the closed connection fails, which removes it; wait on heartbeats, not on a sleep.
            for (int i = 0; i < 10 && notifier.subscriberCount() > 1; i++) {
                other.awaitLine(":heartbeat", WAIT);
            }
            assertThat(notifier.subscriberCount()).isEqualTo(1);
        }
    }
}
