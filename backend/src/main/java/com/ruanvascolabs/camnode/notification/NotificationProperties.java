package com.ruanvascolabs.camnode.notification;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param heartbeatInterval Rule: SPEC-002 #5
 * @param subscriptionTimeout how long one stream connection lives before the browser's EventSource reconnects
 */
@Validated
@ConfigurationProperties("camnode.notification")
record NotificationProperties(@NotNull Duration heartbeatInterval, @NotNull Duration subscriptionTimeout) {
}
