package com.bank.product.version.interceptor;

import com.bank.product.version.domain.model.ApiVersion;
import com.bank.product.version.domain.model.VersionStatus;
import com.bank.product.version.domain.model.VersioningStrategy;
import com.bank.product.version.domain.service.ApiVersionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * API Version Interceptor
 * Validates API version and adds deprecation warnings
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiVersionInterceptor implements HandlerInterceptor {

    private final ApiVersionService apiVersionService;
    private final ApiVersionDetector versionDetector;

    // Default strategy (can be configured)
    private final VersioningStrategy defaultStrategy = VersioningStrategy.URL_PATH;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Detect API version
        String version = versionDetector.detectVersion(request, defaultStrategy);
        String serviceId = versionDetector.extractServiceId(request);

        log.debug("Processing request for service {} version {}", serviceId, version);

        // Store version in request attributes for later use
        request.setAttribute("api.version", version);
        request.setAttribute("api.service", serviceId);

        // Validate version
        Optional<ApiVersion> apiVersion = apiVersionService.getVersion(serviceId, version);

        if (apiVersion.isEmpty()) {
            log.warn("Unknown API version {} requested for service {}", version, serviceId);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setHeader("X-Error-Message", "Unknown API version: " + version);
            return false;
        }

        ApiVersion versionInfo = apiVersion.get();

        // Check if version is EOL
        if (versionInfo.getStatus() == VersionStatus.EOL) {
            log.warn("EOL version {} requested for service {}", version, serviceId);
            response.setStatus(HttpServletResponse.SC_GONE);
            response.setHeader("X-Error-Message",
                    String.format("API version %s has reached end-of-life", version));
            return false;
        }

        // Add deprecation warnings
        if (versionInfo.getStatus() == VersionStatus.DEPRECATED ||
            versionInfo.getStatus() == VersionStatus.SUNSET) {

            String warningMessage = buildDeprecationWarning(versionInfo);
            response.setHeader("Warning", warningMessage);
            response.setHeader("Sunset", versionInfo.getSunsetAt() != null ?
                    versionInfo.getSunsetAt().toString() : "TBD");

            log.info("Deprecated version {} requested for service {}: {}",
                    version, serviceId, warningMessage);
        }

        // Add version info headers
        response.setHeader("X-API-Version", version);
        response.setHeader("X-API-Status", versionInfo.getStatus().name());

        return true;
    }

    /**
     * Build RFC 7234 compliant deprecation warning
     */
    private String buildDeprecationWarning(ApiVersion versionInfo) {
        StringBuilder warning = new StringBuilder();
        warning.append("299 - \"Deprecated API\"");

        if (versionInfo.getDeprecatedAt() != null) {
            warning.append(" Deprecated on: ")
                    .append(versionInfo.getDeprecatedAt());
        }

        if (versionInfo.getSunsetAt() != null) {
            warning.append(" Sunset date: ")
                    .append(versionInfo.getSunsetAt());
        }

        if (versionInfo.getMigrationGuideUrl() != null) {
            warning.append(" See migration guide: ")
                    .append(versionInfo.getMigrationGuideUrl());
        }

        return warning.toString();
    }
}
