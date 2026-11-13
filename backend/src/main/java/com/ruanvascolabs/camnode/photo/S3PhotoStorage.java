package com.ruanvascolabs.camnode.photo;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

class S3PhotoStorage implements PhotoStorage {

    private static final String JPEG_CONTENT_TYPE = "image/jpeg";

    private final S3Client s3;
    private final String bucket;

    S3PhotoStorage(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public void store(String key, byte[] jpeg) {
        try {
            s3.putObject(request -> request.bucket(bucket).key(key).contentType(JPEG_CONTENT_TYPE),
                    RequestBody.fromBytes(jpeg));
        } catch (SdkException e) {
            throw new PhotoStorageUnavailableException(e);
        }
    }
}
