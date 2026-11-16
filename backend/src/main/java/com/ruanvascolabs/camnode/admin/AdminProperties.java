package com.ruanvascolabs.camnode.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param apiKey Rule: SPEC-010 #7. Optional: blank turns the trash off instead of failing startup, so a release
 *               does not depend on the secret being created first.
 */
@ConfigurationProperties("camnode.admin")
record AdminProperties(String apiKey) {

    boolean trashEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
