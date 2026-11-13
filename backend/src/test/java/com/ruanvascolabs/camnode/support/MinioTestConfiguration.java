package com.ruanvascolabs.camnode.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@TestConfiguration(proxyBeanMethods = false)
public class MinioTestConfiguration {

    public static final String BUCKET = "cam-node-photos-test";

    private static final String USER = "camnode";
    private static final String PASSWORD = "camnode-secret";
    private static final int S3_PORT = 9000;

    /**
     * MinIO stopped publishing images to Docker Hub and quay.io; the Bitnami legacy build is the closest release
     * to the homelab one (2025-09-07) that can still be pulled. In production the bucket comes from Ansible.
     */
    @Bean
    GenericContainer<?> minioContainer() {
        return new GenericContainer<>("bitnamilegacy/minio:2025.7.23")
                .withEnv("MINIO_ROOT_USER", USER)
                .withEnv("MINIO_ROOT_PASSWORD", PASSWORD)
                .withEnv("MINIO_DEFAULT_BUCKETS", BUCKET)
                .withExposedPorts(S3_PORT)
                .waitingFor(Wait.forHttp("/minio/health/live").forPort(S3_PORT));
    }

    @Bean
    DynamicPropertyRegistrar minioProperties(GenericContainer<?> minioContainer) {
        return registry -> {
            registry.add("camnode.photo-storage.endpoint",
                    () -> "http://" + minioContainer.getHost() + ":" + minioContainer.getMappedPort(S3_PORT));
            registry.add("camnode.photo-storage.bucket", () -> BUCKET);
            registry.add("camnode.photo-storage.access-key", () -> USER);
            registry.add("camnode.photo-storage.secret-key", () -> PASSWORD);
        };
    }
}
