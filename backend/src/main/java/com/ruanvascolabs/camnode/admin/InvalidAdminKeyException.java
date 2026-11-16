package com.ruanvascolabs.camnode.admin;

public class InvalidAdminKeyException extends RuntimeException {

    InvalidAdminKeyException() {
        super("Missing or invalid admin key");
    }
}
