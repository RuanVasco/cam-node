package com.ruanvascolabs.camnode.ring;

public class UnsupportedPhotoFormatException extends RuntimeException {

    UnsupportedPhotoFormatException() {
        super("Photo is not a JPEG");
    }
}
