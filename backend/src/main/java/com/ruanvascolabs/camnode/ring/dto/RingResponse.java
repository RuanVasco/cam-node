package com.ruanvascolabs.camnode.ring.dto;

import java.time.Instant;
import java.util.UUID;

public record RingResponse(UUID id, Instant ringedAt, String photoUrl) {
}
