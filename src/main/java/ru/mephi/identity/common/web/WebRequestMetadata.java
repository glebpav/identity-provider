package ru.mephi.identity.common.web;

import jakarta.servlet.http.HttpServletRequest;

public record WebRequestMetadata(String ipAddress, String userAgent) {

    public static WebRequestMetadata from(HttpServletRequest request) {
        var forwardedFor = request.getHeader("X-Forwarded-For");
        var ip = forwardedFor == null || forwardedFor.isBlank()
            ? request.getRemoteAddr()
            : forwardedFor.split(",")[0].trim();
        return new WebRequestMetadata(ip, request.getHeader("User-Agent"));
    }
}
