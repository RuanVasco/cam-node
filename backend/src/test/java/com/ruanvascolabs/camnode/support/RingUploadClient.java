package com.ruanvascolabs.camnode.support;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.RestClient;

/**
 * Talks to the real embedded server: multipart size limits are enforced by Tomcat and are not exercised by MockMvc.
 */
public final class RingUploadClient {

    public static final String DEVICE_KEY = "test-device-key";

    private final RestClient client;

    public RingUploadClient(int port) {
        this.client = RestClient.create("http://localhost:" + port);
    }

    public Response upload(byte[] photo, String contentType, String deviceKey) {
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        if (photo != null) {
            body.part("photo", new ByteArrayResource(photo) {
                @Override
                public String getFilename() {
                    return "photo.jpg";
                }
            }).contentType(MediaType.parseMediaType(contentType));
        } else {
            body.part("other", "no photo here");
        }

        return client.post()
                .uri("/api/v1/rings")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .headers(headers -> {
                    if (deviceKey != null) {
                        headers.set("X-Device-Key", deviceKey);
                    }
                })
                .body(body.build())
                .exchange((request, response) -> new Response(
                        response.getStatusCode(),
                        response.getHeaders(),
                        new String(response.getBody().readAllBytes())));
    }

    public record Response(HttpStatusCode status, HttpHeaders headers, String body) {
    }
}
