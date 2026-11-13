package com.ruanvascolabs.camnode.device;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("camnode.device")
record DeviceProperties(@NotBlank String apiKey) {
}
