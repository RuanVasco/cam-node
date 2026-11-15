package com.ruanvascolabs.camnode.ring;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ruanvascolabs.camnode.ring.dto.RingPageResponse;
import com.ruanvascolabs.camnode.ring.dto.RingResponse;

@RestController
@RequestMapping(RingController.BASE_PATH)
class RingController {

    static final String BASE_PATH = "/api/v1/rings";

    /** Rule: SPEC-002 #3. */
    private static final CacheControl PHOTO_CACHE = CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    private final RingService service;

    RingController(RingService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<RingResponse> register(@RequestPart("photo") MultipartFile photo) throws IOException {
        RingResponse response = RingMapper.toResponse(service.register(photo.getBytes()));
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + response.id())).body(response);
    }

    // Explicit parameters instead of Pageable: its resolver silently turns a negative page into 0,
    // and SPEC-002 requires a 400.
    @GetMapping
    RingPageResponse history(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return RingMapper.toPageResponse(service.history(page, size));
    }

    @GetMapping("/{id}/photo")
    ResponseEntity<InputStreamResource> photo(@PathVariable UUID id) {
        RingPhoto photo = service.photo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .contentLength(photo.sizeBytes())
                .cacheControl(PHOTO_CACHE)
                .body(new InputStreamResource(photo.content()));
    }
}
