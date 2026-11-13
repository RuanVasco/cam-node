package com.ruanvascolabs.camnode.ring;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ruanvascolabs.camnode.ring.dto.RingResponse;

@RestController
@RequestMapping(RingController.BASE_PATH)
class RingController {

    static final String BASE_PATH = "/api/v1/rings";

    private final RingService service;

    RingController(RingService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<RingResponse> register(@RequestPart("photo") MultipartFile photo) throws IOException {
        RingResponse response = RingMapper.toResponse(service.register(photo.getBytes()));
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + response.id())).body(response);
    }
}
