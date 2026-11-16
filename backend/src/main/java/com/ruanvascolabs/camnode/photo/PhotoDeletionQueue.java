package com.ruanvascolabs.camnode.photo;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rule: SPEC-010 #3. A purged ring's photo key is recorded in the same transaction that deletes the ring, then
 * deleted from storage after the commit. Whatever storage refuses stays queued and is retried on a schedule, well
 * inside the 24 h the spec allows. The ring row is already gone, so a queued photo is never served again.
 *
 * <p>A failed key moves to the back of the queue and the round stops there: an outage fails every key alike, and a
 * key that keeps failing on its own must not hold back the ones behind it.
 */
@Component
public class PhotoDeletionQueue {

    private static final Logger log = LoggerFactory.getLogger(PhotoDeletionQueue.class);

    static final int RETRY_BATCH = 500;

    private final PendingPhotoDeletionRepository repository;
    private final PhotoStorage storage;
    private final Clock clock;

    PhotoDeletionQueue(PendingPhotoDeletionRepository repository, PhotoStorage storage, Clock clock) {
        this.repository = repository;
        this.storage = storage;
        this.clock = clock;
    }

    /** Joins the caller's transaction, so the keys are queued only if the rings are really deleted. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(Collection<String> photoKeys) {
        Instant now = clock.instant();
        repository.saveAll(photoKeys.stream().map(key -> new PendingPhotoDeletion(key, now)).toList());
    }

    /**
     * Best effort, outside any transaction: storage is a network call. Never throws, because the caller's removal is
     * already committed and whatever fails here stays queued for the retry.
     */
    public void deleteNow(Collection<String> photoKeys) {
        try {
            deleteAll(photoKeys);
        } catch (RuntimeException e) {
            log.warn("Immediate photo deletion failed, left for the retry", e);
        }
    }

    @Scheduled(
            initialDelayString = "${camnode.photo-storage.deletion-retry-initial-delay}",
            fixedDelayString = "${camnode.photo-storage.deletion-retry-interval}")
    public void retryPending() {
        List<String> keys = repository.findKeysInQueueOrder(PageRequest.ofSize(RETRY_BATCH));
        if (!keys.isEmpty()) {
            deleteAll(keys);
        }
    }

    private void deleteAll(Collection<String> photoKeys) {
        List<String> deleted = new ArrayList<>();
        try {
            for (String key : photoKeys) {
                try {
                    storage.delete(key);
                } catch (RuntimeException e) {
                    repository.moveToBack(key, clock.instant());
                    log.warn("Photo deletion failed, {} left queued: {}", photoKeys.size() - deleted.size(),
                            e.getMessage());
                    return;
                }
                deleted.add(key);
            }
        } finally {
            if (!deleted.isEmpty()) {
                repository.deleteAllByIdInBatch(deleted);
            }
        }
    }
}
