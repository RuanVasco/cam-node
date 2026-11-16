package com.ruanvascolabs.camnode.ring.dto;

import java.time.Instant;
import java.util.UUID;

public record TrashedRingResponse(UUID id, Instant ringedAt, Instant removedAt, String photoUrl) {
}
