package com.bank.product.domain.solution.service.impl;

import com.bank.product.domain.solution.dto.ConfigureSolutionRequest;
import com.bank.product.domain.solution.model.Solution;
import com.bank.product.domain.solution.model.SolutionStatus;
import com.bank.product.domain.solution.repository.SolutionRepository;
import com.bank.product.events.SolutionCreatedEvent;
import com.bank.product.events.SolutionStatusChangedEvent;
import com.bank.product.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Solution service with transactional outbox pattern
 * Ensures atomicity between solution creation and event publishing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolutionServiceWithOutbox {

    private final SolutionRepository solutionRepository;
    private final OutboxService outboxService;

    /**
     * Create solution with outbox event (atomic transaction)
     * This method replaces the old createSolutionFromCatalog + async workflow submission
     */
    @Transactional
    public Solution createSolutionWithEvent(String tenantId, String userId, ConfigureSolutionRequest request) {
        log.info("Creating solution with outbox event: catalog={}, tenant={}",
                request.getCatalogProductId(), tenantId);

        // 1. Create solution
        Solution solution = new Solution();
        solution.setId(UUID.randomUUID().toString());
        solution.setSolutionId("sol-" + UUID.randomUUID().toString().substring(0, 8));
        solution.setTenantId(tenantId);
        solution.setCatalogProductId(request.getCatalogProductId());
        solution.setName(request.getSolutionName());
        solution.setDescription(request.getDescription());
        solution.setCategory("CHECKING"); // TODO: Get from catalog
        solution.setStatus(SolutionStatus.DRAFT);
        solution.setVersion("1.0");
        solution.setCreatedAt(LocalDateTime.now());
        solution.setCreatedBy(userId);
        solution.setUpdatedAt(LocalDateTime.now());
        solution.setUpdatedBy(userId);

        Solution savedSolution = solutionRepository.save(solution);

        // 2. Create event (same transaction)
        Map<String, Object> additionalMetadata = new HashMap<>();
        if (request.getCustomFees() != null) {
            additionalMetadata.put("customFees", request.getCustomFees());
        }
        if (request.getCustomInterestRate() != null) {
            additionalMetadata.put("customInterestRate", request.getCustomInterestRate());
        }

        SolutionCreatedEvent event = SolutionCreatedEvent.builder()
                .solutionId(savedSolution.getId())
                .tenantId(tenantId)
                .catalogProductId(request.getCatalogProductId())
                .solutionName(request.getSolutionName())
                .description(request.getDescription())
                .category(savedSolution.getCategory())
                .createdBy(userId)
                .pricingVariance(request.getPricingVariance() != null ? request.getPricingVariance() : 0.0)
                .riskLevel(request.getRiskLevel() != null ? request.getRiskLevel() : "LOW")
                .businessJustification(request.getBusinessJustification())
                .priority(request.getPriority() != null ? request.getPriority() : "MEDIUM")
                .additionalMetadata(additionalMetadata)
                .build();

        // 3. Save to outbox (same transaction - ATOMIC!)
        outboxService.saveEvent(
                event,
                "solution.created",  // Kafka topic
                savedSolution.getId(),
                tenantId
        );

        log.info("Solution created with outbox event: solutionId={}, eventId={}",
                savedSolution.getId(), event.getEventId());

        return savedSolution;
    }

    /**
     * Update solution status with outbox event (atomic transaction)
     */
    @Transactional
    public Solution updateSolutionStatusWithEvent(String solutionId, SolutionStatus newStatus,
                                                    String workflowId, String changedBy, String reason) {
        log.info("Updating solution status: solutionId={}, newStatus={}", solutionId, newStatus);

        // 1. Load solution
        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> new RuntimeException("Solution not found: " + solutionId));

        SolutionStatus oldStatus = solution.getStatus();

        // 2. Validate transition (optional - add state machine validation)
        // SolutionStatusTransition.validate(solutionId, oldStatus, newStatus);

        // 3. Update solution
        solution.setStatus(newStatus);
        solution.setWorkflowId(workflowId);
        solution.setUpdatedAt(LocalDateTime.now());
        solution.setUpdatedBy(changedBy);

        Solution updatedSolution = solutionRepository.save(solution);

        // 4. Create status change event (same transaction)
        SolutionStatusChangedEvent event = SolutionStatusChangedEvent.builder()
                .solutionId(solutionId)
                .tenantId(solution.getTenantId())
                .fromStatus(oldStatus)
                .toStatus(newStatus)
                .changedBy(changedBy)
                .reason(reason)
                .workflowId(workflowId)
                .build();

        // 5. Save to outbox (same transaction - ATOMIC!)
        outboxService.saveEvent(
                event,
                "solution.status-changed",  // Kafka topic
                solutionId,
                solution.getTenantId()
        );

        log.info("Solution status updated with event: solutionId={}, {} -> {}",
                solutionId, oldStatus, newStatus);

        return updatedSolution;
    }
}
