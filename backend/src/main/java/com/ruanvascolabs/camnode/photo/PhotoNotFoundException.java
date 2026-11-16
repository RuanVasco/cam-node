package com.ruanvascolabs.camnode.photo;

public class PhotoNotFoundException extends RuntimeException {

    PhotoNotFoundException(String key) {
        super("Photo not found in storage: " + key);
    }
}
