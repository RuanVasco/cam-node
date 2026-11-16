package com.ruanvascolabs.camnode.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.ruanvascolabs.camnode.support.MinioTestConfiguration;
import com.ruanvascolabs.camnode.support.PostgresTestConfiguration;
import com.ruanvascolabs.camnode.support.SseTestClient;

/** Runs with the default 30 s heartbeat interval, so only the heartbeat sent on open can arrive in time. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresTestConfiguration.class, MinioTestConfiguration.class})
class RingStreamOpenIT {

    private final int port;

    RingStreamOpenIT(@LocalServerPort int port) {
        this.port = port;
    }

    @Test
    @DisplayName("SPEC-002 CA-10 (#11): a new subscriber gets a heartbeat right away, before the first interval")
    void heartbeatOnOpen() throws InterruptedException {
        try (SseTestClient stream = new SseTestClient(port)) {
            assertThat(stream.awaitLine(":heartbeat", Duration.ofSeconds(2))).isNotNull();
        }
    }
}
