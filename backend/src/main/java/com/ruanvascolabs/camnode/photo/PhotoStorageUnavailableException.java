package com.ruanvascolabs.camnode.photo;

public class PhotoStorageUnavailableException extends RuntimeException {

    PhotoStorageUnavailableException(Throwable cause) {
        super("Photo storage unavailable", cause);
    }
}
