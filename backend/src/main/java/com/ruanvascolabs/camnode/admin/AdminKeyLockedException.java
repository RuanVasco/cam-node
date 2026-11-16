package com.ruanvascolabs.camnode.admin;

import java.time.Duration;

public class AdminKeyLockedException extends RuntimeException {

    private final Duration remaining;

    AdminKeyLockedException(Duration remaining) {
        super("Too many wrong admin keys, locked for " + remaining);
        this.remaining = remaining;
    }

    public Duration remaining() {
        return remaining;
    }
}
