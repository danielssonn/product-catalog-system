package com.bank.product.version.interceptor;

import com.bank.product.version.domain.model.VersioningStrategy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API Version Detection
 * Extracts API version from request based on configured strategy
 */
@Slf4j
@Component
public class ApiVersionDetector {

    private static final String DEFAULT_VERSION = "v1";
    private static final Pattern URL_VERSION_PATTERN = Pattern.compile("/api/(v\\d+)/");
    private static final String VERSION_HEADER = "X-API-Version";
    private static final String VERSION_QUERY_PARAM = "version";

    /**
     * Detect API version from request
     */
    public String detectVersion(HttpServletRequest request, VersioningStrategy strategy) {
        String version = switch (strategy) {
            case URL_PATH -> detectFromUrlPath(request);
            case HEADER -> detectFromHeader(request);
            case QUERY_PARAM -> detectFromQueryParam(request);
            case CONTENT_NEGOTIATION -> detectFromContentNegotiation(request);
            default -> null;
        };

        return version != null ? version : DEFAULT_VERSION;
    }

    /**
     * Detect version from URL path (e.g., /api/v2/solutions)
     */
    private String detectFromUrlPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        Matcher matcher = URL_VERSION_PATTERN.matcher(uri);

        if (matcher.find()) {
            String version = matcher.group(1);
            log.debug("Detected version {} from URL path: {}", version, uri);
            return version;
        }

        return null;
    }

    /**
     * Detect version from header (e.g., X-API-Version: v2)
     */
    private String detectFromHeader(HttpServletRequest request) {
        String version = request.getHeader(VERSION_HEADER);
        if (version != null) {
            log.debug("Detected version {} from header", version);
        }
        return version;
    }

    /**
     * Detect version from query parameter (e.g., ?version=v2)
     */
    private String detectFromQueryParam(HttpServletRequest request) {
        String version = request.getParameter(VERSION_QUERY_PARAM);
        if (version != null) {
            log.debug("Detected version {} from query parameter", version);
        }
        return version;
    }

    /**
     * Detect version from Accept header content negotiation
     * (e.g., Accept: application/vnd.bank.v2+json)
     */
    private String detectFromContentNegotiation(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept == null) {
            return null;
        }

        // Pattern: application/vnd.bank.v2+json
        Pattern pattern = Pattern.compile("application/vnd\\.bank\\.(v\\d+)\\+json");
        Matcher matcher = pattern.matcher(accept);

        if (matcher.find()) {
            String version = matcher.group(1);
            log.debug("Detected version {} from content negotiation", version);
            return version;
        }

        return null;
    }

    /**
     * Extract service ID from request
     */
    public String extractServiceId(HttpServletRequest request) {
        // Extract from URI path or header
        String uri = request.getRequestURI();

        // Check for service header
        String serviceHeader = request.getHeader("X-Service-ID");
        if (serviceHeader != null) {
            return serviceHeader;
        }

        // Assume service ID is in the path after /api/vX/
        // Example: /api/v1/solutions -> service = product-service
        // This can be customized based on your routing conventions
        if (uri.contains("/solutions") || uri.contains("/catalog")) {
            return "product-service";
        } else if (uri.contains("/workflows")) {
            return "workflow-service";
        }

        return "unknown-service";
    }
}
