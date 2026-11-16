package com.ruanvascolabs.camnode.ring;

import org.springframework.data.domain.Page;

import com.ruanvascolabs.camnode.ring.dto.TrashPageResponse;
import com.ruanvascolabs.camnode.ring.dto.TrashedRingResponse;

final class TrashMapper {

    private TrashMapper() {
    }

    static TrashedRingResponse toResponse(Ring ring) {
        return new TrashedRingResponse(ring.getId(), ring.getRingedAt(), ring.getRemovedAt(),
                TrashController.BASE_PATH + "/" + ring.getId() + "/photo");
    }

    static TrashPageResponse toPageResponse(Page<Ring> page) {
        return new TrashPageResponse(
                page.map(TrashMapper::toResponse).getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
