package com.ruanvascolabs.camnode.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Rule: SPEC-010 #7. Guards one method on the paths it is registered for; reads stay public (ADR-003). */
class AdminKeyInterceptor implements HandlerInterceptor {

    static final String HEADER = "X-Admin-Key";

    private final AdminKeyVerifier verifier;
    private final HttpMethod guardedMethod;

    AdminKeyInterceptor(AdminKeyVerifier verifier, HttpMethod guardedMethod) {
        this.verifier = verifier;
        this.guardedMethod = guardedMethod;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (guardedMethod.matches(request.getMethod())) {
            verifier.verify(request.getHeader(HEADER));
        }
        return true;
    }
}
