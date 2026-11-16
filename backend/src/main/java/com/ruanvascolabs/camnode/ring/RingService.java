package com.ruanvascolabs.camnode.ring;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import com.ruanvascolabs.camnode.photo.JpegSignature;
import com.ruanvascolabs.camnode.photo.PhotoDeletionQueue;
import com.ruanvascolabs.camnode.photo.PhotoStorage;

@Service
class RingService {

    /** Rule: SPEC-002 #2. */
    static final int MAX_PAGE_SIZE = 100;

    private static final DateTimeFormatter KEY_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    private final RingRepository repository;
    private final PhotoStorage photoStorage;
    private final PhotoDeletionQueue photoDeletions;
    private final ApplicationEventPublisher events;
    private final TransactionOperations transaction;
    private final Clock clock;

    RingService(RingRepository repository, PhotoStorage photoStorage, PhotoDeletionQueue photoDeletions,
            ApplicationEventPublisher events, TransactionOperations transaction, Clock clock) {
        this.repository = repository;
        this.photoStorage = photoStorage;
        this.photoDeletions = photoDeletions;
        this.events = events;
        this.transaction = transaction;
        this.clock = clock;
    }

    /**
     * Not transactional on purpose: the photo upload is a network call and must not hold a database
     * transaction open. The photo is stored first so a failed upload never leaves a ring without a
     * photo (Rule: SPEC-001 #6); a failed insert leaves an orphan object instead (ADR-002).
     */
    Ring register(byte[] photo) {
        // Postgres keeps microseconds; truncating here makes the upload response match what the history returns.
        Instant ringedAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
        if (photo.length == 0) {
            throw new EmptyPhotoException();
        }
        if (!JpegSignature.matches(photo)) {
            throw new UnsupportedPhotoFormatException();
        }

        String photoKey = KEY_DATE.format(ringedAt) + "/" + UUID.randomUUID() + ".jpg";
        photoStorage.store(photoKey, photo);

        Ring ring = repository.save(new Ring(ringedAt, photoKey, photo.length));
        events.publishEvent(new RingRegistered(ring.getId(), ring.getRingedAt()));
        return ring;
    }

    /** Rule: SPEC-002 #2. Sizes above the maximum are capped, not rejected. */
    Page<Ring> history(int page, int size) {
        return repository.findAllByOrderByRingedAtDescIdDesc(PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE)));
    }

    RingPhoto photo(UUID id) {
        Ring ring = repository.findById(id).orElseThrow(() -> new RingNotFoundException(id));
        return new RingPhoto(photoStorage.open(ring.getPhotoKey()), ring.getPhotoSizeBytes());
    }

    /**
     * Rule: SPEC-010 #2, #3, #6. All or nothing: a missing id, including one removed concurrently, rolls the whole
     * removal back. Only database work runs in the transaction; the photos leave storage after the commit.
     */
    void remove(Collection<UUID> requestedIds) {
        Set<UUID> ids = new LinkedHashSet<>(requestedIds);
        List<String> photoKeys = transaction.execute(status -> {
            List<Ring> rings = repository.findAllById(ids);
            if (rings.size() != ids.size()) {
                throw new RingNotFoundException(firstMissing(ids, rings));
            }
            // A concurrent removal deletes some rows first; the count tells, and the exception rolls back.
            if (repository.deleteByIds(ids) != ids.size()) {
                throw new RingNotFoundException();
            }
            List<String> keys = rings.stream().map(Ring::getPhotoKey).toList();
            photoDeletions.enqueue(keys);
            return keys;
        });
        events.publishEvent(new RingsRemoved(List.copyOf(ids)));
        photoDeletions.deleteNow(photoKeys);
    }

    private static UUID firstMissing(Set<UUID> ids, List<Ring> found) {
        Set<UUID> foundIds = found.stream().map(Ring::getId).collect(Collectors.toSet());
        return ids.stream().filter(id -> !foundIds.contains(id)).findFirst().orElseThrow();
    }
}
