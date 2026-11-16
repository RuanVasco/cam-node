package com.ruanvascolabs.camnode.photo;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

interface PendingPhotoDeletionRepository extends JpaRepository<PendingPhotoDeletion, String> {

    @Query("select d.photoKey from PendingPhotoDeletion d order by d.queuedAt, d.photoKey")
    List<String> findKeysInQueueOrder(Pageable pageable);

    @Transactional
    @Modifying
    @Query("update PendingPhotoDeletion d set d.queuedAt = :queuedAt where d.photoKey = :photoKey")
    void moveToBack(String photoKey, Instant queuedAt);
}
