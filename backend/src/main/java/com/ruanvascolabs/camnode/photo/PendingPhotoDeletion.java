package com.ruanvascolabs.camnode.photo;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

/**
 * Persistable because the id is assigned: without it Spring Data would merge, costing a SELECT per enqueued key.
 */
@Entity
@Table(name = "photo_deletion")
class PendingPhotoDeletion implements Persistable<String> {

    @Id
    @Column(name = "photo_key")
    private String photoKey;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt;

    @Transient
    private boolean isNew = true;

    protected PendingPhotoDeletion() {
    }

    PendingPhotoDeletion(String photoKey, Instant queuedAt) {
        this.photoKey = photoKey;
        this.queuedAt = queuedAt;
    }

    @PostLoad
    void markLoaded() {
        isNew = false;
    }

    @Override
    public String getId() {
        return photoKey;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
