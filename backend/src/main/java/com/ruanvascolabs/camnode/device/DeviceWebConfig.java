package com.ruanvascolabs.camnode.device;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class DeviceWebConfig implements WebMvcConfigurer {

    private final DeviceProperties properties;

    DeviceWebConfig(DeviceProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DeviceKeyInterceptor(properties)).addPathPatterns("/api/v1/rings");
    }
}
