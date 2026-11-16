package com.ruanvascolabs.camnode.ring;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.ruanvascolabs.camnode.photo.PhotoDeletionQueue;
import com.ruanvascolabs.camnode.photo.PhotoStorage;

/** Rule: SPEC-010 #10, #11, #12. Only database work runs in a transaction; storage calls stay outside. */
@Service
class TrashService {

    private final RingRepository repository;
    private final PhotoStorage photoStorage;
    private final PhotoDeletionQueue photoDeletions;
    private final ApplicationEventPublisher events;
    private final TransactionOperations transaction;

    TrashService(RingRepository repository, PhotoStorage photoStorage, PhotoDeletionQueue photoDeletions,
            ApplicationEventPublisher events, TransactionOperations transaction) {
        this.repository = repository;
        this.photoStorage = photoStorage;
        this.photoDeletions = photoDeletions;
        this.events = events;
        this.transaction = transaction;
    }

    Page<Ring> list(int page, int size) {
        return repository.findAllByRemovedAtIsNotNullOrderByRemovedAtDescIdDesc(
                PageRequest.of(page, Math.min(size, RingService.MAX_PAGE_SIZE)));
    }

    RingPhoto photo(UUID id) {
        Ring ring = repository.findByIdAndRemovedAtIsNotNull(id).orElseThrow(() -> new RingNotFoundException(id));
        return new RingPhoto(photoStorage.open(ring.getPhotoKey()), ring.getPhotoSizeBytes());
    }

    void restore(UUID id) {
        Ring ring = transaction.execute(status -> {
            if (repository.restoreFromTrash(id) != 1) {
                throw new RingNotFoundException(id);
            }
            return repository.findById(id).orElseThrow(() -> new RingNotFoundException(id));
        });
        events.publishEvent(new RingRestored(ring.getId(), ring.getRingedAt()));
    }

    /** Rule: SPEC-010 #3. The photo key is queued with the delete, so a storage outage never loses it. */
    void purge(UUID id) {
        String photoKey = transaction.execute(status -> {
            Ring ring = repository.findByIdAndRemovedAtIsNotNull(id).orElseThrow(() -> new RingNotFoundException(id));
            // Purged or restored concurrently since the read: the count tells, and the exception rolls back.
            if (repository.deleteFromTrash(id) != 1) {
                throw new RingNotFoundException(id);
            }
            photoDeletions.enqueue(List.of(ring.getPhotoKey()));
            return ring.getPhotoKey();
        });
        events.publishEvent(new RingsRemoved(List.of(id)));
        photoDeletions.deleteNow(List.of(photoKey));
    }
}
