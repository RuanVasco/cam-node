package com.ruanvascolabs.camnode.admin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.stereotype.Component;

/**
 * Rule: SPEC-010 #7, #8. Temporary until login exists (SPEC-004).
 *
 * <p>The lockout is global, not per client: behind Cloudflare the client address would mean trusting a forwarded
 * header, and anyone can still lock removal out for a window (accepted in the plan). Checking the lock and recording
 * a failure happen under one lock, so parallel guesses can not all slip past the check before any is counted.
 */
@Component
class AdminKeyVerifier {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final byte[] expectedKey;
    private final Clock clock;
    private final Deque<Instant> failures = new ArrayDeque<>();

    AdminKeyVerifier(AdminProperties properties, Clock clock) {
        this.expectedKey = properties.removalEnabled() ? properties.apiKey().getBytes(StandardCharsets.UTF_8) : null;
        this.clock = clock;
    }

    /**
     * Refused while locked, even with the right key. A missing key, or none configured, is refused without counting
     * as a wrong one. A right key does not clear earlier failures: #8 counts wrong keys in the window, whatever
     * succeeded in between.
     */
    synchronized void verify(String providedKey) {
        Instant now = clock.instant();
        while (!failures.isEmpty() && !failures.peekFirst().plus(WINDOW).isAfter(now)) {
            failures.removeFirst();
        }
        if (failures.size() >= MAX_FAILURES) {
            throw new AdminKeyLockedException(Duration.between(now, failures.peekFirst().plus(WINDOW)));
        }
        if (providedKey == null || expectedKey == null) {
            throw new InvalidAdminKeyException();
        }
        // Constant-time comparison so response timing does not leak how much of the key matched.
        if (!MessageDigest.isEqual(expectedKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
            failures.addLast(now);
            throw new InvalidAdminKeyException();
        }
    }
}
