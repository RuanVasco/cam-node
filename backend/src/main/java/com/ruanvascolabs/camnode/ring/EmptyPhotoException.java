package com.ruanvascolabs.camnode.ring;

public class EmptyPhotoException extends RuntimeException {

    EmptyPhotoException() {
        super("Photo is empty");
    }
}
