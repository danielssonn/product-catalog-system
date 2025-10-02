package com.bank.product.version.controller;

import com.bank.product.version.domain.model.ApiVersion;
import com.bank.product.version.domain.model.VersionStatus;
import com.bank.product.version.domain.service.ApiVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * API Version Management Controller
 * Manages API version registry and lifecycle
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/versions")
@RequiredArgsConstructor
public class ApiVersionController {

    private final ApiVersionService apiVersionService;

    /**
     * Register a new API version
     */
    @PostMapping
    public ResponseEntity<ApiVersion> registerVersion(
            @RequestHeader("X-User-ID") String userId,
            @Valid @RequestBody ApiVersion apiVersion) {

        log.info("Registering new API version {} for service {} by user {}",
                apiVersion.getVersion(), apiVersion.getServiceId(), userId);

        apiVersion.setCreatedBy(userId);
        ApiVersion created = apiVersionService.registerVersion(apiVersion);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get version by service and version identifier
     */
    @GetMapping("/{serviceId}/{version}")
    public ResponseEntity<ApiVersion> getVersion(
            @PathVariable String serviceId,
            @PathVariable String version) {

        log.info("Fetching version {} for service {}", version, serviceId);

        return apiVersionService.getVersion(serviceId, version)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all versions for a service
     */
    @GetMapping("/{serviceId}")
    public ResponseEntity<List<ApiVersion>> getAllVersions(@PathVariable String serviceId) {
        log.info("Fetching all versions for service {}", serviceId);
        List<ApiVersion> versions = apiVersionService.getAllVersions(serviceId);
        return ResponseEntity.ok(versions);
    }

    /**
     * Get current stable version
     */
    @GetMapping("/{serviceId}/stable")
    public ResponseEntity<ApiVersion> getStableVersion(@PathVariable String serviceId) {
        log.info("Fetching stable version for service {}", serviceId);

        return apiVersionService.getStableVersion(serviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get default version
     */
    @GetMapping("/{serviceId}/default")
    public ResponseEntity<ApiVersion> getDefaultVersion(@PathVariable String serviceId) {
        log.info("Fetching default version for service {}", serviceId);

        return apiVersionService.getDefaultVersion(serviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update version status
     */
    @PatchMapping("/{serviceId}/{version}/status")
    public ResponseEntity<ApiVersion> updateVersionStatus(
            @PathVariable String serviceId,
            @PathVariable String version,
            @RequestParam VersionStatus status,
            @RequestHeader("X-User-ID") String userId) {

        log.info("Updating version {} status to {} for service {} by user {}",
                version, status, serviceId, userId);

        ApiVersion updated = apiVersionService.updateVersionStatus(serviceId, version, status);
        updated.setUpdatedBy(userId);

        return ResponseEntity.ok(updated);
    }

    /**
     * Deprecate version
     */
    @PostMapping("/{serviceId}/{version}/deprecate")
    public ResponseEntity<ApiVersion> deprecateVersion(
            @PathVariable String serviceId,
            @PathVariable String version,
            @RequestParam String reason,
            @RequestHeader("X-User-ID") String userId) {

        log.info("Deprecating version {} for service {}: {} by user {}",
                version, serviceId, reason, userId);

        ApiVersion deprecated = apiVersionService.deprecateVersion(serviceId, version, reason);
        deprecated.setUpdatedBy(userId);

        return ResponseEntity.ok(deprecated);
    }

    /**
     * Check if version is supported
     */
    @GetMapping("/{serviceId}/{version}/supported")
    public ResponseEntity<Boolean> isVersionSupported(
            @PathVariable String serviceId,
            @PathVariable String version) {

        log.info("Checking if version {} is supported for service {}", version, serviceId);
        boolean supported = apiVersionService.isVersionSupported(serviceId, version);

        return ResponseEntity.ok(supported);
    }
}
