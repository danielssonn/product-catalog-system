package com.bank.product.core.service;

import com.bank.product.core.model.TenantCoreSystemMapping;
import com.bank.product.core.repository.TenantCoreSystemMappingRepository;
import com.bank.product.domain.solution.model.Solution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Routes solutions to appropriate core banking systems.
 * Supports multiple routing strategies: geographic, product-type, priority, etc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoreSystemRouter {

    private final TenantCoreSystemMappingRepository mappingRepository;

    /**
     * Determine which core system(s) a solution should be provisioned to.
     *
     * @param solution the solution to route
     * @return list of target core systems (ordered by priority)
     */
    public List<TenantCoreSystemMapping.CoreSystemMapping> routeSolutionToCores(Solution solution) {
        log.debug("Routing solution {} for tenant: {}", solution.getId(), solution.getTenantId());

        // Get tenant's core system mapping
        TenantCoreSystemMapping mapping = mappingRepository
                .findByTenantId(solution.getTenantId())
                .orElse(null);

        if (mapping == null || mapping.getCoreSystems().isEmpty()) {
            log.warn("No core systems configured for tenant: {}", solution.getTenantId());
            return List.of();
        }

        // Filter active cores
        List<TenantCoreSystemMapping.CoreSystemMapping> activeCores = mapping.getCoreSystems()
                .stream()
                .filter(TenantCoreSystemMapping.CoreSystemMapping::isActive)
                .collect(Collectors.toList());

        if (activeCores.isEmpty()) {
            log.warn("No active core systems for tenant: {}", solution.getTenantId());
            return List.of();
        }

        // Apply routing strategies
        List<TenantCoreSystemMapping.CoreSystemMapping> targetCores = activeCores.stream()
                // Filter by product type support (if specified)
                .filter(core -> supportsProductType(core, solution.getCategory()))
                // Filter by geographic region (if specified in metadata)
                .filter(core -> matchesGeography(core, solution))
                // Sort by priority (higher first)
                .sorted(Comparator.comparingInt(TenantCoreSystemMapping.CoreSystemMapping::getPriority).reversed())
                .collect(Collectors.toList());

        if (targetCores.isEmpty()) {
            // Fallback: use default core system
            String defaultCoreId = mapping.getDefaultCoreSystemId();
            if (defaultCoreId != null) {
                log.info("Using default core system {} for solution {}", defaultCoreId, solution.getId());
                targetCores = activeCores.stream()
                        .filter(core -> core.getCoreSystemId().equals(defaultCoreId))
                        .collect(Collectors.toList());
            }
        }

        if (targetCores.isEmpty()) {
            log.warn("No suitable core systems found for solution {}. Routing strategy failed.", solution.getId());
        } else {
            log.info("Routed solution {} to {} core system(s)", solution.getId(), targetCores.size());
        }

        return targetCores;
    }

    /**
     * Check if core system supports a product type.
     */
    private boolean supportsProductType(
            TenantCoreSystemMapping.CoreSystemMapping core,
            String productType) {

        // Empty list means all types supported
        if (core.getSupportedProductTypes() == null || core.getSupportedProductTypes().isEmpty()) {
            return true;
        }

        return core.getSupportedProductTypes().contains(productType);
    }

    /**
     * Check if core system matches geography requirements.
     */
    private boolean matchesGeography(
            TenantCoreSystemMapping.CoreSystemMapping core,
            Solution solution) {

        // If no region specified on core, it serves all regions
        if (core.getRegion() == null || core.getRegion().isEmpty()) {
            return true;
        }

        // Check if solution metadata specifies a region
        if (solution.getMetadata() == null) {
            return true;
        }

        String requiredRegion = (String) solution.getMetadata().get("region");
        if (requiredRegion == null) {
            return true;
        }

        return core.getRegion().equalsIgnoreCase(requiredRegion);
    }
}
