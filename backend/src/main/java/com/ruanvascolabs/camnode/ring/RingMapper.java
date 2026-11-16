package com.ruanvascolabs.camnode.ring;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.ruanvascolabs.camnode.ring.dto.RingPageResponse;
import com.ruanvascolabs.camnode.ring.dto.RingRemovedResponse;
import com.ruanvascolabs.camnode.ring.dto.RingResponse;

public final class RingMapper {

    private RingMapper() {
    }

    static RingResponse toResponse(Ring ring) {
        return new RingResponse(ring.getId(), ring.getRingedAt(), photoUrl(ring.getId()));
    }

    static RingPageResponse toPageResponse(Page<Ring> page) {
        return new RingPageResponse(
                page.map(RingMapper::toResponse).getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public static RingResponse toResponse(RingRegistered event) {
        return new RingResponse(event.id(), event.ringedAt(), photoUrl(event.id()));
    }

    public static List<RingRemovedResponse> toResponses(RingsRemoved event) {
        return event.ids().stream().map(RingRemovedResponse::new).toList();
    }

    static String photoUrl(UUID id) {
        return RingController.BASE_PATH + "/" + id + "/photo";
    }
}
