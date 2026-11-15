package com.ruanvascolabs.camnode.ring;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.ruanvascolabs.camnode.photo.JpegSignature;
import com.ruanvascolabs.camnode.photo.PhotoStorage;

@Service
class RingService {

    /** Rule: SPEC-002 #2. */
    static final int MAX_PAGE_SIZE = 100;

    private static final DateTimeFormatter KEY_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    private final RingRepository repository;
    private final PhotoStorage photoStorage;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    RingService(RingRepository repository, PhotoStorage photoStorage, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.photoStorage = photoStorage;
        this.events = events;
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
}
