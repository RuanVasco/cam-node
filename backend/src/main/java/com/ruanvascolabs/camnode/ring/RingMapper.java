package com.ruanvascolabs.camnode.ring;

import java.util.UUID;

import org.springframework.data.domain.Page;

import com.ruanvascolabs.camnode.ring.dto.RingPageResponse;
import com.ruanvascolabs.camnode.ring.dto.RingResponse;

final class RingMapper {

    private RingMapper() {
    }

    static RingResponse toResponse(Ring ring) {
        return new RingResponse(ring.getId(), ring.getRingedAt(), photoUrl(ring.getId()));
    }

    static RingPageResponse toPageResponse(Page<Ring> page) {
        return new RingPageResponse(
                page.map(RingMapper::toResponse).getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    static String photoUrl(UUID id) {
        return RingController.BASE_PATH + "/" + id + "/photo";
    }
}
