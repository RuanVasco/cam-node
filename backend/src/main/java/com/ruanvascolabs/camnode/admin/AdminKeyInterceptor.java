package com.ruanvascolabs.camnode.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

/** Rule: SPEC-010 #7. Guards every method on the paths it is registered for: the whole trash, reads included. */
class AdminKeyInterceptor implements HandlerInterceptor {

    static final String HEADER = "X-Admin-Key";

    private final AdminKeyVerifier verifier;

    AdminKeyInterceptor(AdminKeyVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        verifier.verify(request.getHeader(HEADER));
        return true;
    }
}
