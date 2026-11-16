package com.ruanvascolabs.camnode.photo;

import java.io.InputStream;

public interface PhotoStorage {

    void store(String key, byte[] jpeg);

    /** The caller owns the returned stream and must close it. */
    InputStream open(String key);

    /** Idempotent: deleting a key that does not exist succeeds. */
    void delete(String key);
}
