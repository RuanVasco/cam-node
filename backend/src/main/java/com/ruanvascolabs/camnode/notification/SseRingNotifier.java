package com.ruanvascolabs.camnode.notification;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ruanvascolabs.camnode.ring.RingMapper;
import com.ruanvascolabs.camnode.ring.RingRegistered;
import com.ruanvascolabs.camnode.ring.RingRestored;
import com.ruanvascolabs.camnode.ring.RingsRemoved;
import com.ruanvascolabs.camnode.ring.dto.RingRemovedResponse;
import com.ruanvascolabs.camnode.ring.dto.RingResponse;

/**
 * Subscribers live in process memory, which only works with a single API replica (ADR-001).
 *
 * <p>Every send runs on one scheduler thread: the upload request never waits on a slow subscriber, and an emitter is
 * never written by two threads at once.
 *
 * <p>Stops before the web server's graceful shutdown (default phase), otherwise open streams would hold it until the
 * shutdown timeout. That phase also means it starts after the web server, so the executor exists from construction
 * and start only schedules the heartbeat. Stop must leave the executor alive: Spring may stop and start the same bean
 * again (the test context cache pauses idle contexts), so the executor is only shut down when the bean is destroyed.
 */
@Component
class SseRingNotifier implements SmartLifecycle, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(SseRingNotifier.class);

    static final String RING_EVENT = "ring";
    static final String RING_REMOVED_EVENT = "ring-removed";
    static final String RING_RESTORED_EVENT = "ring-restored";
    static final String HEARTBEAT_COMMENT = "heartbeat";

    private final NotificationProperties properties;
    private final List<SseEmitter> subscribers = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("sse-notifier").daemon().factory());
    private volatile ScheduledFuture<?> heartbeat;

    SseRingNotifier(NotificationProperties properties) {
        this.properties = properties;
    }

    /**
     * Rule: SPEC-002 #11. Cloudflare holds the response headers until the first body bytes, so without this heartbeat
     * the browser only sees the stream open after a full interval. Sent before the emitter is registered, so the
     * scheduler thread can not write to it at the same time; Spring buffers it until the response starts.
     */
    SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(properties.subscriptionTimeout().toMillis());
        try {
            emitter.send(SseEmitter.event().comment(HEARTBEAT_COMMENT));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }
        register(emitter);
        return emitter;
    }

    void register(SseEmitter emitter) {
        emitter.onCompletion(() -> subscribers.remove(emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> subscribers.remove(emitter));
        subscribers.add(emitter);
    }

    int subscriberCount() {
        return subscribers.size();
    }

    /** Rule: SPEC-002 #4. */
    @EventListener
    void onRingRegistered(RingRegistered event) {
        broadcastRing(RING_EVENT, RingMapper.toResponse(event));
    }

    /** Rule: SPEC-010 #11. */
    @EventListener
    void onRingRestored(RingRestored event) {
        broadcastRing(RING_RESTORED_EVENT, RingMapper.toResponse(event));
    }

    private void broadcastRing(String eventName, RingResponse payload) {
        try {
            scheduler.execute(() -> broadcast(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON)));
        } catch (RejectedExecutionException e) {
            // Bean destroyed: subscribers are already closed and the ring is saved, so the request must not fail.
            log.info("Ring {} not broadcast as {}, notifier destroyed", payload.id(), eventName);
        }
    }

    /** Rule: SPEC-010 #4. One event per ring, so clients handle a bulk removal like single ones. */
    @EventListener
    void onRingsRemoved(RingsRemoved event) {
        Collection<RingRemovedResponse> payloads = RingMapper.toResponses(event);
        List<SseEmitter.SseEventBuilder> events = payloads.stream()
                .map(payload -> SseEmitter.event().name(RING_REMOVED_EVENT).data(payload, MediaType.APPLICATION_JSON))
                .toList();
        try {
            scheduler.execute(() -> broadcast(events));
        } catch (RejectedExecutionException e) {
            // Bean destroyed: subscribers are already closed and the removal is committed, so it must not fail.
            log.info("Removal of {} ring(s) not broadcast, notifier destroyed", payloads.size());
        }
    }

    /** Rule: SPEC-002 #5. */
    void sendHeartbeat() {
        broadcast(SseEmitter.event().comment(HEARTBEAT_COMMENT));
    }

    private void broadcast(SseEmitter.SseEventBuilder event) {
        broadcast(List.of(event));
    }

    // Subscribers on the outside: a failing one is dropped once instead of being retried for every event.
    private void broadcast(List<SseEmitter.SseEventBuilder> events) {
        for (SseEmitter emitter : subscribers) {
            try {
                for (SseEmitter.SseEventBuilder event : events) {
                    emitter.send(event);
                }
            } catch (IOException | IllegalStateException e) {
                // The client went away; dropping it here keeps the list from growing with dead connections.
                subscribers.remove(emitter);
                log.debug("Dropped SSE subscriber: {}", e.getMessage());
            } catch (RuntimeException e) {
                // An exception escaping the heartbeat task would silently cancel every future heartbeat.
                subscribers.remove(emitter);
                log.warn("Dropped SSE subscriber after unexpected failure", e);
            }
        }
    }

    @Override
    public void start() {
        long interval = properties.heartbeatInterval().toMillis();
        heartbeat = scheduler.scheduleAtFixedRate(this::sendHeartbeat, interval, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        ScheduledFuture<?> current = heartbeat;
        if (current != null) {
            current.cancel(false);
            heartbeat = null;
        }
        subscribers.forEach(SseEmitter::complete);
        subscribers.clear();
    }

    @Override
    public boolean isRunning() {
        return heartbeat != null;
    }

    @Override
    public void destroy() {
        scheduler.shutdownNow();
    }
}
