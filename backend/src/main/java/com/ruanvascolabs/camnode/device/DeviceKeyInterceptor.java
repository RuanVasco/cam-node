package com.ruanvascolabs.camnode.device;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Rule: SPEC-001 #1. Only device uploads are protected; reads stay public (ADR-003).
 */
class DeviceKeyInterceptor implements HandlerInterceptor {

    static final String HEADER = "X-Device-Key";

    private final byte[] expectedKey;

    DeviceKeyInterceptor(DeviceProperties properties) {
        this.expectedKey = properties.apiKey().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        String provided = request.getHeader(HEADER);
        // Constant-time comparison so response timing does not leak how much of the key matched.
        if (provided == null || !MessageDigest.isEqual(expectedKey, provided.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidDeviceKeyException();
        }
        return true;
    }
}
