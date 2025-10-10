package com.bank.product.core.service;

import com.bank.product.core.adapter.CoreBankingAdapter;
import com.bank.product.core.adapter.CoreBankingAdapterRegistry;
import com.bank.product.core.model.*;
import com.bank.product.core.repository.TenantCoreSystemMappingRepository;
import com.bank.product.domain.solution.model.Solution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates provisioning operations across multiple core banking systems.
 * Handles routing, adapter selection, and event publishing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoreProvisioningOrchestrator {

    private final CoreBankingAdapterRegistry adapterRegistry;
    private final TenantCoreSystemMappingRepository mappingRepository;
    private final CoreSystemRouter coreSystemRouter;
    private final KafkaTemplate<String, ProvisioningEvent> kafkaTemplate;

    private static final String PROVISIONING_TOPIC = "core-provisioning-events";

    /**
     * Provision a solution to appropriate core banking system(s).
     *
     * @param solution the solution to provision
     * @return list of provisioning results (one per core system)
     */
    public List<CoreProvisioningResult> provisionSolution(Solution solution) {
        log.info("Orchestrating provisioning for solution: {} (tenant: {})",
                solution.getId(), solution.getTenantId());

        String correlationId = UUID.randomUUID().toString();

        // Get target core systems for this solution
        List<TenantCoreSystemMapping.CoreSystemMapping> targetCores =
                coreSystemRouter.routeSolutionToCores(solution);

        if (targetCores.isEmpty()) {
            log.warn("No core systems configured for tenant: {}", solution.getTenantId());
            publishEvent(buildEvent(solution, null, ProvisioningEventType.PROVISIONING_FAILED,
                    "No core systems configured", correlationId));
            return List.of();
        }

        List<CoreProvisioningResult> results = new ArrayList<>();

        // Provision to each target core system
        for (TenantCoreSystemMapping.CoreSystemMapping coreMapping : targetCores) {
            CoreProvisioningResult result = provisionToCore(solution, coreMapping, correlationId);
            results.add(result);

            // Update solution's provisioning records
            updateProvisioningRecord(solution, coreMapping, result);
        }

        return results;
    }

    /**
     * Update an existing product in core banking system(s).
     *
     * @param solution the updated solution
     * @return list of update results
     */
    public List<CoreProvisioningResult> updateSolution(Solution solution) {
        log.info("Orchestrating update for solution: {} (tenant: {})",
                solution.getId(), solution.getTenantId());

        String correlationId = UUID.randomUUID().toString();
        List<CoreProvisioningResult> results = new ArrayList<>();

        // Get existing provisioning records
        if (solution.getCoreProvisioningRecords() == null ||
            solution.getCoreProvisioningRecords().isEmpty()) {
            log.warn("Solution {} has no provisioning records to update", solution.getId());
            return results;
        }

        // Update in each core system where it's provisioned
        for (CoreProvisioningRecord record : solution.getCoreProvisioningRecords()) {
            if (record.getStatus() != CoreProvisioningStatus.PROVISIONED) {
                log.debug("Skipping update for non-provisioned core: {} (status: {})",
                        record.getCoreSystemId(), record.getStatus());
                continue;
            }

            CoreProvisioningResult result = updateInCore(solution, record, correlationId);
            results.add(result);

            // Update provisioning record status
            updateRecordFromResult(record, result);
        }

        return results;
    }

    /**
     * Deactivate a product in core banking system(s).
     *
     * @param solution the solution to deactivate
     * @return list of deactivation results
     */
    public List<CoreProvisioningResult> deactivateSolution(Solution solution) {
        log.info("Orchestrating deactivation for solution: {} (tenant: {})",
                solution.getId(), solution.getTenantId());

        String correlationId = UUID.randomUUID().toString();
        return performLifecycleOperation(
                solution,
                correlationId,
                ProvisioningEventType.DEACTIVATION_STARTED,
                ProvisioningEventType.DEACTIVATION_SUCCEEDED,
                ProvisioningEventType.DEACTIVATION_FAILED,
                CoreProvisioningStatus.DEACTIVATING,
                CoreProvisioningStatus.DEACTIVATED,
                (adapter, coreProductId, config) -> adapter.deactivateProduct(coreProductId, config)
        );
    }

    /**
     * Sunset a product in core banking system(s).
     *
     * @param solution the solution to sunset
     * @return list of sunset results
     */
    public List<CoreProvisioningResult> sunsetSolution(Solution solution) {
        log.info("Orchestrating sunset for solution: {} (tenant: {})",
                solution.getId(), solution.getTenantId());

        String correlationId = UUID.randomUUID().toString();
        return performLifecycleOperation(
                solution,
                correlationId,
                ProvisioningEventType.SUNSET_STARTED,
                ProvisioningEventType.SUNSET_SUCCEEDED,
                ProvisioningEventType.SUNSET_FAILED,
                CoreProvisioningStatus.SUNSET,
                CoreProvisioningStatus.SUNSET,
                (adapter, coreProductId, config) -> adapter.sunsetProduct(coreProductId, config)
        );
    }

    /**
     * Provision solution to a specific core system.
     */
    private CoreProvisioningResult provisionToCore(
            Solution solution,
            TenantCoreSystemMapping.CoreSystemMapping coreMapping,
            String correlationId) {

        String coreSystemId = coreMapping.getCoreSystemId();
        CoreSystemType coreSystemType = coreMapping.getCoreSystemType();

        log.info("Provisioning solution {} to core system: {} (type: {})",
                solution.getId(), coreSystemId, coreSystemType);

        // Publish start event
        publishEvent(buildEvent(solution, coreMapping, ProvisioningEventType.PROVISIONING_STARTED,
                null, correlationId));

        // Get adapter
        CoreBankingAdapter adapter = adapterRegistry.getAdapter(coreSystemType)
                .orElseThrow(() -> new IllegalStateException(
                        "No adapter found for core system type: " + coreSystemType));

        // Provision
        CoreProvisioningResult result = adapter.provisionProduct(solution, coreMapping.getConfig());

        // Publish result event
        ProvisioningEventType eventType = result.isSuccess()
                ? ProvisioningEventType.PROVISIONING_SUCCEEDED
                : ProvisioningEventType.PROVISIONING_FAILED;

        publishEvent(buildEvent(solution, coreMapping, eventType,
                result.getErrorMessage(), correlationId));

        return result;
    }

    /**
     * Update solution in a specific core system.
     */
    private CoreProvisioningResult updateInCore(
            Solution solution,
            CoreProvisioningRecord record,
            String correlationId) {

        log.info("Updating solution {} in core system: {} (type: {})",
                solution.getId(), record.getCoreSystemId(), record.getCoreSystemType());

        // Get core system config
        TenantCoreSystemMapping mapping = mappingRepository
                .findByTenantId(solution.getTenantId())
                .orElseThrow(() -> new IllegalStateException(
                        "No core system mapping found for tenant: " + solution.getTenantId()));

        TenantCoreSystemMapping.CoreSystemMapping coreMapping = mapping.getCoreSystems().stream()
                .filter(cs -> cs.getCoreSystemId().equals(record.getCoreSystemId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Core system not found in mapping: " + record.getCoreSystemId()));

        // Publish start event
        publishEvent(buildEvent(solution, coreMapping, ProvisioningEventType.UPDATE_STARTED,
                null, correlationId));

        // Get adapter
        CoreBankingAdapter adapter = adapterRegistry.getAdapter(record.getCoreSystemType())
                .orElseThrow(() -> new IllegalStateException(
                        "No adapter found for core system type: " + record.getCoreSystemType()));

        // Update
        CoreProvisioningResult result = adapter.updateProduct(
                solution, record.getCoreProductId(), coreMapping.getConfig());

        // Publish result event
        ProvisioningEventType eventType = result.isSuccess()
                ? ProvisioningEventType.UPDATE_SUCCEEDED
                : ProvisioningEventType.UPDATE_FAILED;

        publishEvent(buildEvent(solution, coreMapping, eventType,
                result.getErrorMessage(), correlationId));

        return result;
    }

    /**
     * Generic lifecycle operation (deactivate/sunset).
     */
    private List<CoreProvisioningResult> performLifecycleOperation(
            Solution solution,
            String correlationId,
            ProvisioningEventType startEvent,
            ProvisioningEventType successEvent,
            ProvisioningEventType failureEvent,
            CoreProvisioningStatus inProgressStatus,
            CoreProvisioningStatus finalStatus,
            LifecycleOperation operation) {

        List<CoreProvisioningResult> results = new ArrayList<>();

        if (solution.getCoreProvisioningRecords() == null ||
            solution.getCoreProvisioningRecords().isEmpty()) {
            log.warn("Solution {} has no provisioning records", solution.getId());
            return results;
        }

        for (CoreProvisioningRecord record : solution.getCoreProvisioningRecords()) {
            if (record.getStatus() == CoreProvisioningStatus.SUNSET) {
                log.debug("Skipping already sunset core: {}", record.getCoreSystemId());
                continue;
            }

            // Get core system config
            TenantCoreSystemMapping mapping = mappingRepository
                    .findByTenantId(solution.getTenantId())
                    .orElseThrow(() -> new IllegalStateException(
                            "No core system mapping found for tenant: " + solution.getTenantId()));

            TenantCoreSystemMapping.CoreSystemMapping coreMapping = mapping.getCoreSystems().stream()
                    .filter(cs -> cs.getCoreSystemId().equals(record.getCoreSystemId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Core system not found in mapping: " + record.getCoreSystemId()));

            // Publish start event
            publishEvent(buildEvent(solution, coreMapping, startEvent, null, correlationId));

            // Get adapter
            CoreBankingAdapter adapter = adapterRegistry.getAdapter(record.getCoreSystemType())
                    .orElseThrow(() -> new IllegalStateException(
                            "No adapter found for core system type: " + record.getCoreSystemType()));

            // Execute operation
            record.setStatus(inProgressStatus);
            CoreProvisioningResult result = operation.execute(
                    adapter, record.getCoreProductId(), coreMapping.getConfig());

            // Update record
            if (result.isSuccess()) {
                record.setStatus(finalStatus);
                record.setLastSyncedAt(Instant.now());
            } else {
                record.setStatus(CoreProvisioningStatus.PROVISION_FAILED);
                record.getErrorMessages().add(result.getErrorMessage());
                record.setLastErrorAt(Instant.now());
                record.setRetryCount(record.getRetryCount() + 1);
            }

            // Publish result event
            ProvisioningEventType eventType = result.isSuccess() ? successEvent : failureEvent;
            publishEvent(buildEvent(solution, coreMapping, eventType,
                    result.getErrorMessage(), correlationId));

            results.add(result);
        }

        return results;
    }

    /**
     * Update solution's provisioning record after operation.
     */
    private void updateProvisioningRecord(
            Solution solution,
            TenantCoreSystemMapping.CoreSystemMapping coreMapping,
            CoreProvisioningResult result) {

        if (solution.getCoreProvisioningRecords() == null) {
            solution.setCoreProvisioningRecords(new ArrayList<>());
        }

        CoreProvisioningRecord record = CoreProvisioningRecord.builder()
                .coreSystemId(coreMapping.getCoreSystemId())
                .coreSystemType(coreMapping.getCoreSystemType())
                .coreProductId(result.getCoreProductId())
                .status(result.isSuccess()
                        ? CoreProvisioningStatus.PROVISIONED
                        : CoreProvisioningStatus.PROVISION_FAILED)
                .provisionedAt(result.isSuccess() ? Instant.now() : null)
                .lastSyncedAt(result.isSuccess() ? Instant.now() : null)
                .build();

        if (!result.isSuccess()) {
            record.getErrorMessages().add(result.getErrorMessage());
            record.setLastErrorAt(Instant.now());
            record.setRetryCount(1);
        }

        solution.getCoreProvisioningRecords().add(record);
    }

    /**
     * Update provisioning record from result.
     */
    private void updateRecordFromResult(CoreProvisioningRecord record, CoreProvisioningResult result) {
        if (result.isSuccess()) {
            record.setLastSyncedAt(Instant.now());
        } else {
            record.getErrorMessages().add(result.getErrorMessage());
            record.setLastErrorAt(Instant.now());
            record.setRetryCount(record.getRetryCount() + 1);
            record.setStatus(result.isRetryable()
                    ? CoreProvisioningStatus.PROVISION_FAILED
                    : CoreProvisioningStatus.PROVISION_FAILED_PERMANENT);
        }
    }

    /**
     * Build provisioning event.
     */
    private ProvisioningEvent buildEvent(
            Solution solution,
            TenantCoreSystemMapping.CoreSystemMapping coreMapping,
            ProvisioningEventType eventType,
            String errorMessage,
            String correlationId) {

        return ProvisioningEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .tenantId(solution.getTenantId())
                .solutionId(solution.getId())
                .coreSystemId(coreMapping != null ? coreMapping.getCoreSystemId() : null)
                .coreSystemType(coreMapping != null ? coreMapping.getCoreSystemType() : null)
                .errorMessage(errorMessage)
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .build();
    }

    /**
     * Publish provisioning event to Kafka.
     */
    private void publishEvent(ProvisioningEvent event) {
        try {
            kafkaTemplate.send(PROVISIONING_TOPIC, event.getSolutionId(), event);
            log.debug("Published provisioning event: {} for solution: {}",
                    event.getEventType(), event.getSolutionId());
        } catch (Exception e) {
            log.error("Failed to publish provisioning event", e);
        }
    }

    /**
     * Functional interface for lifecycle operations.
     */
    @FunctionalInterface
    private interface LifecycleOperation {
        CoreProvisioningResult execute(
                CoreBankingAdapter adapter,
                String coreProductId,
                CoreSystemConfig config);
    }
}
