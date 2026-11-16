package com.ruanvascolabs.camnode.ring;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruanvascolabs.camnode.ring.dto.TrashPageResponse;

/** Every route needs the admin key (SPEC-010 #7), checked by the admin interceptor before reaching here. */
@RestController
@RequestMapping(TrashController.BASE_PATH)
class TrashController {

    static final String BASE_PATH = "/api/v1/trash";

    private final TrashService service;

    TrashController(TrashService service) {
        this.service = service;
    }

    @GetMapping
    TrashPageResponse list(
            @RequestParam(defaultValue = "0") @PositiveOrZero @Max(RingController.MAX_PAGE) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return TrashMapper.toPageResponse(service.list(page, size));
    }

    // Not cached anywhere: the photo is only for the admin and may be purged at any moment.
    @GetMapping("/{id}/photo")
    ResponseEntity<InputStreamResource> photo(@PathVariable UUID id) {
        RingPhoto photo = service.photo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .contentLength(photo.sizeBytes())
                .cacheControl(CacheControl.noStore())
                .body(new InputStreamResource(photo.content()));
    }

    @PostMapping("/{id}/restore")
    ResponseEntity<Void> restore(@PathVariable UUID id) {
        service.restore(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> purge(@PathVariable UUID id) {
        service.purge(id);
        return ResponseEntity.noContent().build();
    }
}
