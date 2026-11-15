package com.ruanvascolabs.camnode.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ruanvascolabs.camnode.ring.RingRegistered;

class SseRingNotifierTest {

    private final SseRingNotifier notifier =
            new SseRingNotifier(new NotificationProperties(Duration.ofSeconds(30), Duration.ofMinutes(30)));

    @Test
    @DisplayName("SPEC-002 #5: a subscriber whose connection fails is removed on the next heartbeat")
    void removesFailingSubscriber() {
        notifier.register(failingWith(new IOException("broken pipe")));

        notifier.sendHeartbeat();

        assertThat(notifier.subscriberCount()).isZero();
    }

    @Test
    @DisplayName("SPEC-002 #5: an unexpected failure on one subscriber does not escape the heartbeat")
    void unexpectedFailureStaysContained() {
        notifier.register(failingWith(new UnsupportedOperationException("boom")));
        notifier.register(new SseEmitter());

        notifier.sendHeartbeat();

        assertThat(notifier.subscriberCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("SPEC-001 #7: a ring registered while the notifier is stopped does not fail the upload")
    void ringAfterStopIsIgnored() {
        notifier.start();
        notifier.stop();

        assertThatCode(() -> notifier.onRingRegistered(new RingRegistered(UUID.randomUUID(), Instant.EPOCH)))
                .doesNotThrowAnyException();
    }

    private static SseEmitter failingWith(Exception failure) {
        return new SseEmitter() {
            @Override
            public void send(SseEventBuilder event) throws IOException {
                if (failure instanceof IOException io) {
                    throw io;
                }
                throw (RuntimeException) failure;
            }
        };
    }
}
