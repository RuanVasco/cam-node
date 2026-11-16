package com.ruanvascolabs.camnode.ring;

import java.util.List;
import java.util.UUID;

/**
 * Published after removed rings are committed. Rule: SPEC-010 #4.
 */
public record RingsRemoved(List<UUID> ids) {
}
