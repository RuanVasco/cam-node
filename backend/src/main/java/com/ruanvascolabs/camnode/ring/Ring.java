package com.ruanvascolabs.camnode.ring;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "ring")
class Ring {

    // Time-ordered ids keep inserts append-only on the primary key index.
    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "ringed_at", nullable = false)
    private Instant ringedAt;

    @Column(name = "photo_key", nullable = false)
    private String photoKey;

    @Column(name = "photo_size_bytes", nullable = false)
    private int photoSizeBytes;

    protected Ring() {
    }

    Ring(Instant ringedAt, String photoKey, int photoSizeBytes) {
        this.ringedAt = ringedAt;
        this.photoKey = photoKey;
        this.photoSizeBytes = photoSizeBytes;
    }

    UUID getId() {
        return id;
    }

    Instant getRingedAt() {
        return ringedAt;
    }

    String getPhotoKey() {
        return photoKey;
    }

    int getPhotoSizeBytes() {
        return photoSizeBytes;
    }
}
