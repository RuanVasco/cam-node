package com.ruanvascolabs.camnode.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.ruanvascolabs.camnode.support.MinioTestConfiguration;
import com.ruanvascolabs.camnode.support.PostgresTestConfiguration;
import com.ruanvascolabs.camnode.support.RingUploadClient;
import com.ruanvascolabs.camnode.support.SseTestClient;
import com.ruanvascolabs.camnode.support.TestPhotos;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfiguration.class, MinioTestConfiguration.class})
@TestPropertySource(properties = {
        "camnode.device.api-key=" + RingUploadClient.DEVICE_KEY,
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
