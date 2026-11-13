package com.ruanvascolabs.camnode.ring;

import java.util.UUID;

import com.ruanvascolabs.camnode.ring.dto.RingResponse;

final class RingMapper {

    private RingMapper() {
    }

    static RingResponse toResponse(Ring ring) {
        return new RingResponse(ring.getId(), ring.getRingedAt(), photoUrl(ring.getId()));
    }

    static String photoUrl(UUID id) {
        return RingController.BASE_PATH + "/" + id + "/photo";
    }
}
