package com.ruanvascolabs.camnode.photo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PhotoDeletionQueueTest {

    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

    private final PendingPhotoDeletionRepository repository = mock(PendingPhotoDeletionRepository.class);
    private final PhotoStorage storage = mock(PhotoStorage.class);
    private final PhotoDeletionQueue queue = new PhotoDeletionQueue(repository, storage, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("SPEC-010 #3: a failed key goes to the back of the queue and the round stops there")
    void failedKeyMovesToBack() {
        when(repository.findKeysInQueueOrder(any())).thenReturn(List.of("a.jpg", "b.jpg", "c.jpg"));
        doThrow(new PhotoStorageUnavailableException(new RuntimeException("down"))).when(storage).delete("b.jpg");

        queue.retryPending();

        verify(repository).deleteAllByIdInBatch(List.of("a.jpg"));
        verify(repository).moveToBack("b.jpg", NOW);
        verify(storage, never()).delete("c.jpg");
    }

    @Test
    @DisplayName("SPEC-010 #3: the immediate deletion never fails the removal that called it")
    void deleteNowSwallowsFailures() {
        doThrow(new IllegalStateException("database down")).when(repository).deleteAllByIdInBatch(any());

        queue.deleteNow(List.of("a.jpg"));

        verify(storage).delete("a.jpg");
    }
}
