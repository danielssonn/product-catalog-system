package com.bank.product.core.service;

import com.bank.product.core.adapter.CoreBankingAdapter;
import com.bank.product.core.adapter.CoreBankingAdapterRegistry;
import com.bank.product.core.model.CoreSystemType;
import com.bank.product.core.model.TenantCoreSystemMapping;
import com.bank.product.core.repository.TenantCoreSystemMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitors health of core banking systems.
 * Performs periodic health checks and tracks availability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoreSystemHealthMonitor {

    private final CoreBankingAdapterRegistry adapterRegistry;
    private final TenantCoreSystemMappingRepository mappingRepository;

    private final Map<String, HealthStatus> healthStatusMap = new ConcurrentHashMap<>();

    /**
     * Perform health checks every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void performHealthChecks() {
        log.debug("Performing core system health checks");

        List<TenantCoreSystemMapping> allMappings = mappingRepository.findAll();

        for (TenantCoreSystemMapping mapping : allMappings) {
            for (TenantCoreSystemMapping.CoreSystemMapping coreMapping : mapping.getCoreSystems()) {
                if (coreMapping.isActive()) {
                    checkCoreSystemHealth(coreMapping);
                }
            }
        }

        logHealthSummary();
    }

    /**
     * Check health of a specific core system.
     */
    private void checkCoreSystemHealth(TenantCoreSystemMapping.CoreSystemMapping coreMapping) {
        String coreSystemId = coreMapping.getCoreSystemId();
        CoreSystemType coreSystemType = coreMapping.getCoreSystemType();

        try {
            CoreBankingAdapter adapter = adapterRegistry.getAdapter(coreSystemType)
                    .orElseThrow(() -> new IllegalStateException(
                            "No adapter for core system type: " + coreSystemType));

            long startTime = System.currentTimeMillis();
            boolean healthy = adapter.healthCheck(coreMapping.getConfig());
            long duration = System.currentTimeMillis() - startTime;

            HealthStatus status = HealthStatus.builder()
                    .coreSystemId(coreSystemId)
                    .coreSystemType(coreSystemType)
                    .healthy(healthy)
                    .lastCheckTime(Instant.now())
                    .responseTimeMs(duration)
                    .build();

            HealthStatus previousStatus = healthStatusMap.put(coreSystemId, status);

            // Log if status changed
            if (previousStatus != null && previousStatus.isHealthy() != healthy) {
                if (healthy) {
                    log.info("Core system {} is now HEALTHY (was down for {}ms)",
                            coreSystemId,
                            status.getLastCheckTime().toEpochMilli() -
                                    previousStatus.getLastCheckTime().toEpochMilli());
                } else {
                    log.error("Core system {} is now UNHEALTHY", coreSystemId);
                }
            }

            if (!healthy) {
                log.warn("Health check failed for core system: {} ({})", coreSystemId, coreSystemType);
            }

        } catch (Exception e) {
            log.error("Error checking health for core system {}: {}", coreSystemId, e.getMessage());

            HealthStatus status = HealthStatus.builder()
                    .coreSystemId(coreSystemId)
                    .coreSystemType(coreSystemType)
                    .healthy(false)
                    .lastCheckTime(Instant.now())
                    .errorMessage(e.getMessage())
                    .build();

            healthStatusMap.put(coreSystemId, status);
        }
    }

    /**
     * Get health status for a specific core system.
     */
    public Optional<HealthStatus> getHealthStatus(String coreSystemId) {
        return Optional.ofNullable(healthStatusMap.get(coreSystemId));
    }

    /**
     * Get all health statuses.
     */
    public Map<String, HealthStatus> getAllHealthStatuses() {
        return new HashMap<>(healthStatusMap);
    }

    /**
     * Check if a core system is healthy.
     */
    public boolean isHealthy(String coreSystemId) {
        HealthStatus status = healthStatusMap.get(coreSystemId);
        return status != null && status.isHealthy();
    }

    /**
     * Get health summary report.
     */
    public HealthSummary getHealthSummary() {
        int totalSystems = healthStatusMap.size();
        long healthySystems = healthStatusMap.values().stream()
                .filter(HealthStatus::isHealthy)
                .count();
        long unhealthySystems = totalSystems - healthySystems;

        Map<CoreSystemType, Integer> healthByType = new HashMap<>();
        for (HealthStatus status : healthStatusMap.values()) {
            if (status.isHealthy()) {
                healthByType.merge(status.getCoreSystemType(), 1, Integer::sum);
            }
        }

        return HealthSummary.builder()
                .totalSystems(totalSystems)
                .healthySystems((int) healthySystems)
                .unhealthySystems((int) unhealthySystems)
                .healthyPercentage(totalSystems > 0
                        ? (double) healthySystems / totalSystems * 100
                        : 0.0)
                .healthByType(healthByType)
                .lastCheckTime(Instant.now())
                .build();
    }

    /**
     * Log health summary.
     */
    private void logHealthSummary() {
        HealthSummary summary = getHealthSummary();
        log.info("Core system health summary: {}/{} healthy ({:.1f}%)",
                summary.getHealthySystems(),
                summary.getTotalSystems(),
                summary.getHealthyPercentage());
    }

    /**
     * Health status for a core system.
     */
    @lombok.Data
    @lombok.Builder
    public static class HealthStatus {
        private String coreSystemId;
        private CoreSystemType coreSystemType;
        private boolean healthy;
        private Instant lastCheckTime;
        private Long responseTimeMs;
        private String errorMessage;
    }

    /**
     * Health summary across all core systems.
     */
    @lombok.Data
    @lombok.Builder
    public static class HealthSummary {
        private int totalSystems;
        private int healthySystems;
        private int unhealthySystems;
        private double healthyPercentage;
        private Map<CoreSystemType, Integer> healthByType;
        private Instant lastCheckTime;
    }
}
