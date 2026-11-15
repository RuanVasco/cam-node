package com.ruanvascolabs.camnode.support;

import java.net.URI;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@TestConfiguration(proxyBeanMethods = false)
public class MinioTestConfiguration {

    public static final String BUCKET = "cam-node-photos-test";

    private static final String USER = "camnode";
    private static final String PASSWORD = "camnode-secret";
    private static final int S3_PORT = 9000;

    /**
     * MinIO stopped publishing images to Docker Hub and quay.io; the Bitnami legacy build is the closest release
     * to the homelab one (2025-09-07) that can still be pulled. In production the bucket comes from Ansible.
     *
     * <p>The bucket is created here instead of through MINIO_DEFAULT_BUCKETS: the image reports healthy before it
     * creates default buckets, so the first upload of a test could fail with NoSuchBucket.
     */
    @Bean
    GenericContainer<?> minioContainer() {
        GenericContainer<?> container = new GenericContainer<>("bitnamilegacy/minio:2025.7.23")
                .withEnv("MINIO_ROOT_USER", USER)
                .withEnv("MINIO_ROOT_PASSWORD", PASSWORD)
                .withExposedPorts(S3_PORT)
                .waitingFor(Wait.forHttp("/minio/health/live").forPort(S3_PORT));
        container.start();
        try (S3Client s3 = S3Client.builder()
                .endpointOverride(URI.create(endpoint(container)))
                .region(Region.US_EAST_1)
                .forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(USER, PASSWORD)))
                .build()) {
            s3.createBucket(request -> request.bucket(BUCKET));
        }
        return container;
    }

    @Bean
    DynamicPropertyRegistrar minioProperties(GenericContainer<?> minioContainer) {
        return registry -> {
            registry.add("camnode.photo-storage.endpoint", () -> endpoint(minioContainer));
            registry.add("camnode.photo-storage.bucket", () -> BUCKET);
            registry.add("camnode.photo-storage.access-key", () -> USER);
            registry.add("camnode.photo-storage.secret-key", () -> PASSWORD);
        };
    }

    private static String endpoint(GenericContainer<?> container) {
        return "http://" + container.getHost() + ":" + container.getMappedPort(S3_PORT);
    }
}
