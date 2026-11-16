package com.ruanvascolabs.camnode.ring;

import java.time.Instant;
import java.util.UUID;

/**
 * Published after a ring leaves the trash. Rule: SPEC-010 #11.
 */
public record RingRestored(UUID id, Instant ringedAt) {
}
