package com.ruanvascolabs.camnode.ring;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface RingRepository extends JpaRepository<Ring, UUID> {

    Page<Ring> findAllByRemovedAtIsNullOrderByRingedAtDescIdDesc(Pageable pageable);

    Page<Ring> findAllByRemovedAtIsNotNullOrderByRemovedAtDescIdDesc(Pageable pageable);

    Optional<Ring> findByIdAndRemovedAtIsNull(UUID id);

    Optional<Ring> findByIdAndRemovedAtIsNotNull(UUID id);

    @Modifying
    @Query("update Ring r set r.removedAt = :removedAt where r.id in :ids and r.removedAt is null")
    int moveToTrash(Collection<UUID> ids, Instant removedAt);

    @Modifying
    @Query("update Ring r set r.removedAt = null where r.id = :id and r.removedAt is not null")
    int restoreFromTrash(UUID id);

    @Modifying
    @Query("delete from Ring r where r.id = :id and r.removedAt is not null")
    int deleteFromTrash(UUID id);
}
