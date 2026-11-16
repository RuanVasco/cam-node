package com.ruanvascolabs.camnode.ring;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionOperations;

import com.ruanvascolabs.camnode.photo.PhotoDeletionQueue;
import com.ruanvascolabs.camnode.photo.PhotoStorage;

class TrashServiceTest {

    private static final Instant RINGED_AT = Instant.parse("2026-11-23T19:42:10Z");
    private static final String PHOTO_KEY = "2026/11/23/a.jpg";

    private final RingRepository repository = mock(RingRepository.class);
    private final PhotoDeletionQueue photoDeletions = mock(PhotoDeletionQueue.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final TrashService service = new TrashService(repository, mock(PhotoStorage.class), photoDeletions, events,
            TransactionOperations.withoutTransaction());

    @Test
    @DisplayName("SPEC-010 #10: the trash lists the most recently removed first and caps the page size at 100")
    void listsByRemovalTime() {
        when(repository.findAllByRemovedAtIsNotNullOrderByRemovedAtDescIdDesc(any())).thenReturn(Page.empty());

        service.list(0, 500);

        verify(repository).findAllByRemovedAtIsNotNullOrderByRemovedAtDescIdDesc(PageRequest.of(0, 100));
    }

    @Test
    @DisplayName("SPEC-010 #11: a restored ring is announced with its original ring time")
    void restoreAnnounces() {
        Ring ring = trashedRing();
        when(repository.restoreFromTrash(ring.getId())).thenReturn(1);
        when(repository.findById(ring.getId())).thenReturn(Optional.of(ring));

        service.restore(ring.getId());

        verify(events).publishEvent(new RingRestored(ring.getId(), RINGED_AT));
    }

    @Test
    @DisplayName("SPEC-010 #11: restoring a ring that is not in the trash is not found and announces nothing")
    void restoreOutsideTrash() {
        UUID id = UUID.randomUUID();
        when(repository.restoreFromTrash(id)).thenReturn(0);

        assertThatThrownBy(() -> service.restore(id)).isInstanceOf(RingNotFoundException.class);
        verify(events, never()).publishEvent(any());
    }

    @Test
    @DisplayName("SPEC-010 #3, #12: purging queues the photo, announces the ring and then deletes the photo")
    void purgeQueuesAnnouncesAndDeletes() {
        Ring ring = trashedRing();
        when(repository.findByIdAndRemovedAtIsNotNull(ring.getId())).thenReturn(Optional.of(ring));
        when(repository.deleteFromTrash(ring.getId())).thenReturn(1);

        service.purge(ring.getId());

        InOrder order = inOrder(photoDeletions, events);
        order.verify(photoDeletions).enqueue(List.of(PHOTO_KEY));
        order.verify(events).publishEvent(new RingsRemoved(List.of(ring.getId())));
        order.verify(photoDeletions).deleteNow(List.of(PHOTO_KEY));
    }

    @Test
    @DisplayName("SPEC-010 #12: a ring purged or restored concurrently since the read fails without queueing its photo")
    void concurrentPurgeFails() {
        Ring ring = trashedRing();
        when(repository.findByIdAndRemovedAtIsNotNull(ring.getId())).thenReturn(Optional.of(ring));
        when(repository.deleteFromTrash(ring.getId())).thenReturn(0);

        assertThatThrownBy(() -> service.purge(ring.getId())).isInstanceOf(RingNotFoundException.class);
        verify(photoDeletions, never()).enqueue(any());
        verify(photoDeletions, never()).deleteNow(any());
        verify(events, never()).publishEvent(any());
    }

    private static Ring trashedRing() {
        Ring ring = new Ring(RINGED_AT, PHOTO_KEY, 1);
        ReflectionTestUtils.setField(ring, "id", UUID.randomUUID());
        return ring;
    }
}
