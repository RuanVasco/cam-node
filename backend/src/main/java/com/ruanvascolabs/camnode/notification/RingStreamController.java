package com.ruanvascolabs.camnode.notification;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
class RingStreamController {

    private final SseRingNotifier notifier;

    RingStreamController(SseRingNotifier notifier) {
        this.notifier = notifier;
    }

    // X-Accel-Buffering tells nginx not to buffer this response even if a location forgets proxy_buffering off.
    @GetMapping(path = "/api/v1/rings/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<SseEmitter> stream() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("X-Accel-Buffering", "no")
                .body(notifier.subscribe());
    }
}
