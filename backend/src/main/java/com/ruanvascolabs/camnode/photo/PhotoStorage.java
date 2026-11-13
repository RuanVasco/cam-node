package com.ruanvascolabs.camnode.photo;

public interface PhotoStorage {

    void store(String key, byte[] jpeg);
}
