package com.ruanvascolabs.camnode.support;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Reads the stream line by line into a queue, so tests wait on a line with a timeout instead of sleeping.
 */
public final class SseTestClient implements AutoCloseable {

    private final HttpClient http = HttpClient.newHttpClient();
    private final BlockingQueue<String> lines = new LinkedBlockingQueue<>();
    private final CompletableFuture<HttpResponse<Void>> response;

    public SseTestClient(int port) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/rings/stream"))
                .header("Accept", "text/event-stream")
                .build();
        this.response = http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArrayConsumer(chunk -> chunk.ifPresent(
                bytes -> new String(bytes, StandardCharsets.UTF_8).lines().forEach(lines::add))));
    }

    /** Returns the next line starting with the prefix, or null when none arrives within the timeout. */
    public String awaitLine(String prefix, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            String line = lines.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
            if (line == null || line.startsWith(prefix)) {
                return line;
            }
        }
    }

    public boolean isOpen() {
        return !response.isDone();
    }

    @Override
    public void close() {
        response.cancel(true);
        http.shutdownNow();
    }
}
