package com.ruanvascolabs.camnode.ring.dto;

import java.util.List;

public record RingPageResponse(List<RingResponse> content, int page, int size, long totalElements) {
}
