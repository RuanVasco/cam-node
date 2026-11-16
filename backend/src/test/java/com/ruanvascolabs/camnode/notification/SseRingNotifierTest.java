package com.ruanvascolabs.camnode.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ruanvascolabs.camnode.ring.RingRegistered;
import com.ruanvascolabs.camnode.ring.RingsRemoved;

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
    @DisplayName("SPEC-001 #7: a ring registered after the notifier is destroyed does not fail the upload")
    void ringAfterDestroyIsIgnored() {
        notifier.start();
        notifier.stop();
        notifier.destroy();

        assertThatCode(() -> notifier.onRingRegistered(new RingRegistered(UUID.randomUUID(), Instant.EPOCH)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SPEC-010 #4: a removal after the notifier is destroyed does not fail the removal")
    void removalAfterDestroyIsIgnored() {
        notifier.destroy();

        assertThatCode(() -> notifier.onRingsRemoved(new RingsRemoved(List.of(UUID.randomUUID()))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SPEC-002 #5: the notifier starts again after a stop, as when Spring pauses and resumes a context")
    void restartsAfterStop() {
        notifier.start();
        notifier.stop();

        assertThatCode(notifier::start).doesNotThrowAnyException();
        assertThat(notifier.isRunning()).isTrue();
        notifier.stop();
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
