package com.ruanvascolabs.camnode.ring;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface RingRepository extends JpaRepository<Ring, UUID> {

    Page<Ring> findAllByOrderByRingedAtDescIdDesc(Pageable pageable);

    @Modifying
    @Query("delete from Ring r where r.id in :ids")
    int deleteByIds(Collection<UUID> ids);
}
