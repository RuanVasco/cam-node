package com.ruanvascolabs.camnode.ring;

import java.util.UUID;

public class RingNotFoundException extends RuntimeException {

    RingNotFoundException(UUID id) {
        super("Ring not found: " + id);
    }

    // A bulk update counts rows, it does not say which id was missing or already in the trash.
    RingNotFoundException() {
        super("Ring not found or already removed");
    }
}
