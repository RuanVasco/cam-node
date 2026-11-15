package com.ruanvascolabs.camnode.ring;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface RingRepository extends JpaRepository<Ring, UUID> {

    Page<Ring> findAllByOrderByRingedAtDescIdDesc(Pageable pageable);
}
