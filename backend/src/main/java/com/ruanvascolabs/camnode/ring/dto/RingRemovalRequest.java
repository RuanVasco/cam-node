package com.ruanvascolabs.camnode.ring.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Rule: SPEC-010 #6. */
public record RingRemovalRequest(@NotNull @Size(min = 1, max = 100) List<@NotNull UUID> ids) {
}
