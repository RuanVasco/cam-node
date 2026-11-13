package com.ruanvascolabs.camnode.ring;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface RingRepository extends JpaRepository<Ring, UUID> {
}
