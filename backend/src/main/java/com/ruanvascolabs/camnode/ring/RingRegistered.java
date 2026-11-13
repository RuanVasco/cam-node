package com.ruanvascolabs.camnode.ring;

import java.time.Instant;
import java.util.UUID;

/**
 * Published after a ring is persisted. Rule: SPEC-001 #7.
 */
public record RingRegistered(UUID id, Instant ringedAt) {
}
