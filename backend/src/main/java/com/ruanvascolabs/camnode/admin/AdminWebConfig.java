package com.ruanvascolabs.camnode.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class AdminWebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AdminWebConfig.class);

    private final AdminProperties properties;
    private final AdminKeyVerifier verifier;

    AdminWebConfig(AdminProperties properties, AdminKeyVerifier verifier) {
        this.properties = properties;
        this.verifier = verifier;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (!properties.trashEnabled()) {
            log.warn("Admin key not configured (ADMIN_API_KEY): the trash is disabled");
        }
        registry.addInterceptor(new AdminKeyInterceptor(verifier)).addPathPatterns("/api/v1/trash", "/api/v1/trash/**");
    }
}
