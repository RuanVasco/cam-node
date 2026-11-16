package com.ruanvascolabs.camnode.ring.dto;

import java.util.List;

public record TrashPageResponse(List<TrashedRingResponse> content, int page, int size, long totalElements) {
}
