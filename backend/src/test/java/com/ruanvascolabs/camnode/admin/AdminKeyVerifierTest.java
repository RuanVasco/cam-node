package com.ruanvascolabs.camnode.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ruanvascolabs.camnode.support.MutableClock;

class AdminKeyVerifierTest {

    private static final String KEY = "right-key";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-16T12:00:00Z"));
    private final AdminKeyVerifier verifier = new AdminKeyVerifier(new AdminProperties(KEY), clock);

    @Test
    @DisplayName("SPEC-010 #7: the right key passes, a wrong or missing one is refused")
    void checksKey() {
        assertThatCode(() -> verifier.verify(KEY)).doesNotThrowAnyException();
        assertThatThrownBy(() -> verifier.verify("wrong")).isInstanceOf(InvalidAdminKeyException.class);
        assertThatThrownBy(() -> verifier.verify(null)).isInstanceOf(InvalidAdminKeyException.class);
    }

    @Test
    @DisplayName("SPEC-010 #7: without a configured admin key every removal is refused")
    void refusesWhenKeyNotConfigured() {
        var unconfigured = new AdminKeyVerifier(new AdminProperties(""), clock);

        assertThatThrownBy(() -> unconfigured.verify("")).isInstanceOf(InvalidAdminKeyException.class);
    }

    @Test
    @DisplayName("SPEC-010 #8: four wrong keys do not lock, after the fifth even the right key is refused for 15 min")
    void locksAfterFifthFailure() {
        failTimes(4);
        assertThatCode(() -> verifier.verify(KEY)).doesNotThrowAnyException();

        failTimes(1);

        assertThatThrownBy(() -> verifier.verify(KEY))
                .isInstanceOfSatisfying(AdminKeyLockedException.class,
                        e -> assertThat(e.remaining()).isEqualTo(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("SPEC-010 #8: the lock ends when the oldest failure leaves the window, and refused attempts do not extend it")
    void unlocksAtEndOfWindow() {
        failTimes(1);
        clock.advance(Duration.ofMinutes(5));
        failTimes(4);

        clock.advance(Duration.ofMinutes(10).minusSeconds(1));
        assertThatThrownBy(() -> verifier.verify("wrong")).isInstanceOf(AdminKeyLockedException.class);

        clock.advance(Duration.ofSeconds(1));
        assertThatCode(() -> verifier.verify(KEY)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SPEC-010 #8: failures spread over more than 15 min never lock")
    void slowFailuresDoNotLock() {
        for (int i = 0; i < 10; i++) {
            failTimes(1);
            clock.advance(Duration.ofMinutes(4));
        }

        assertThatCode(() -> verifier.verify(KEY)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SPEC-010 #8: a right key in between does not clear earlier failures")
    void successDoesNotClear() {
        failTimes(4);
        verifier.verify(KEY);
        failTimes(1);

        assertThatThrownBy(() -> verifier.verify(KEY)).isInstanceOf(AdminKeyLockedException.class);
    }

    @Test
    @DisplayName("SPEC-010 #8: a missing key does not count as a wrong one")
    void missingKeyDoesNotCount() {
        for (int i = 0; i < 10; i++) {
            assertThatThrownBy(() -> verifier.verify(null)).isInstanceOf(InvalidAdminKeyException.class);
        }

        assertThatCode(() -> verifier.verify(KEY)).doesNotThrowAnyException();
    }

    private void failTimes(int count) {
        for (int i = 0; i < count; i++) {
            assertThatThrownBy(() -> verifier.verify("wrong")).isInstanceOf(InvalidAdminKeyException.class);
        }
    }
}
