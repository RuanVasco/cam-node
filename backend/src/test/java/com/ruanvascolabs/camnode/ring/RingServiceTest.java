package com.ruanvascolabs.camnode.ring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.ruanvascolabs.camnode.photo.PhotoStorage;
import com.ruanvascolabs.camnode.support.TestPhotos;

class RingServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T19:42:10Z");

    private final RingRepository repository = mock(RingRepository.class);
    private final PhotoStorage photoStorage = mock(PhotoStorage.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final RingService service =
            new RingService(repository, photoStorage, events, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void persistAssignsId() {
        when(repository.save(any(Ring.class))).thenAnswer(invocation -> {
            Ring ring = invocation.getArgument(0);
            ReflectionTestUtils.setField(ring, "id", UUID.fromString("0193f1c2-7a4e-7c3b-9f00-1a2b3c4d5e6f"));
            return ring;
        });
    }

    @Test
    @DisplayName("SPEC-001 #5: ring time is the backend clock at reception")
    void usesBackendClock() {
        Ring ring = service.register(TestPhotos.jpeg(1024));

        assertThat(ring.getRingedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("SPEC-001 #6: photo is stored before the ring is persisted, under a dated key")
    void storesPhotoUnderDatedKey() {
        Ring ring = service.register(TestPhotos.jpeg(1024));

        verify(photoStorage).store(eq(ring.getPhotoKey()), any());
        assertThat(ring.getPhotoKey()).startsWith("2026/11/23/").endsWith(".jpg");
        assertThat(ring.getPhotoSizeBytes()).isEqualTo(1024);
    }

    @Test
    @DisplayName("SPEC-001 #6: no ring is persisted when the photo cannot be stored")
    void noRingWhenStorageFails() {
        RuntimeException failure = new RuntimeException("storage down");
        doThrow(failure).when(photoStorage).store(anyString(), any());

        assertThatThrownBy(() -> service.register(TestPhotos.jpeg(1024))).isSameAs(failure);
        verify(repository, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    @DisplayName("SPEC-001 #3: non JPEG content is rejected before storing anything")
    void rejectsNonJpeg() {
        assertThatThrownBy(() -> service.register(TestPhotos.png())).isInstanceOf(UnsupportedPhotoFormatException.class);
        verify(photoStorage, never()).store(anyString(), any());
    }

    @Test
    @DisplayName("SPEC-001 #2: empty photo is rejected")
    void rejectsEmpty() {
        assertThatThrownBy(() -> service.register(new byte[0])).isInstanceOf(EmptyPhotoException.class);
    }

    @Test
    @DisplayName("SPEC-001 #7: a registered ring is announced")
    void announcesRegisteredRing() {
        Ring ring = service.register(TestPhotos.jpeg(1024));

        verify(events).publishEvent(new RingRegistered(ring.getId(), NOW));
    }
}
