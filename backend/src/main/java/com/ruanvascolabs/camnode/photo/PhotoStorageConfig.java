package com.ruanvascolabs.camnode.photo;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration(proxyBeanMethods = false)
class PhotoStorageConfig {

    @Bean(destroyMethod = "close")
    S3Client photoS3Client(PhotoStorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(properties.endpoint())
                // MinIO ignores the region but the SDK requires one. Path style because there is no wildcard DNS per bucket.
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                .overrideConfiguration(config -> config.apiCallTimeout(Duration.ofSeconds(10)))
                .build();
    }

    @Bean
    PhotoStorage photoStorage(S3Client photoS3Client, PhotoStorageProperties properties) {
        return new S3PhotoStorage(photoS3Client, properties.bucket());
    }
}
