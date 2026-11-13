package com.ruanvascolabs.camnode.photo;

import java.net.URI;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("camnode.photo-storage")
record PhotoStorageProperties(
        @NotNull URI endpoint,
        @NotBlank String bucket,
        @NotBlank String accessKey,
        @NotBlank String secretKey) {
}
