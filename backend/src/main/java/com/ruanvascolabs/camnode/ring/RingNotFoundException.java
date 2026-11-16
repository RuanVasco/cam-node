package com.ruanvascolabs.camnode.ring;

import java.util.UUID;

public class RingNotFoundException extends RuntimeException {

    RingNotFoundException(UUID id) {
        super("Ring not found: " + id);
    }

    // A ring removed concurrently: the delete count does not say which one.
    RingNotFoundException() {
        super("Ring removed concurrently");
    }
}
