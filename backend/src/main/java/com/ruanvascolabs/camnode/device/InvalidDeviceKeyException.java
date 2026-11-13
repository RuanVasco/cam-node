package com.ruanvascolabs.camnode.device;

public class InvalidDeviceKeyException extends RuntimeException {

    InvalidDeviceKeyException() {
        super("Missing or invalid device key");
    }
}
