package com.bank.product.workflow.kafka;

import com.bank.product.workflow.domain.service.WorkflowExecutionService;
import com.bank.product.workflow.dto.WorkflowSubmitRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumes party change events from source systems (Commercial Banking, Capital Markets)
 * and triggers Change in Circumstance (CIC) workflows
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartyChangeEventConsumer {

    private final WorkflowExecutionService workflowExecutionService;
    private final ObjectMapper objectMapper;

    /**
     * Listen for party change events from Commercial Banking system
     */
    @KafkaListener(
            topics = "commercial-banking-party-changes",
            groupId = "workflow-service-party-cic",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCommercialBankingChanges(String message) {
        try {
            log.info("Received Commercial Banking party change event: {}", message);
            processPartyChangeEvent(message, "COMMERCIAL_BANKING");
        } catch (Exception e) {
            log.error("Error processing Commercial Banking party change event", e);
        }
    }

    /**
     * Listen for party change events from Capital Markets system
     */
    @KafkaListener(
            topics = "capital-markets-party-changes",
            groupId = "workflow-service-party-cic",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeCapitalMarketsChanges(String message) {
        try {
            log.info("Received Capital Markets party change event: {}", message);
            processPartyChangeEvent(message, "CAPITAL_MARKETS");
        } catch (Exception e) {
            log.error("Error processing Capital Markets party change event", e);
        }
    }

    /**
     * Process party change event and trigger CIC workflow if needed
     */
    private void processPartyChangeEvent(String message, String sourceSystem) throws Exception {
        JsonNode event = objectMapper.readTree(message);

        String eventType = event.get("eventType").asText();
        String partyId = event.get("partyId").asText();
        JsonNode changes = event.get("changes");

        log.info("Processing {} event for party {} from {}", eventType, partyId, sourceSystem);

        // Determine if changes require workflow approval
        if (requiresApproval(eventType, changes)) {
            log.info("Change requires approval - triggering CIC workflow");
            triggerCICWorkflow(partyId, sourceSystem, eventType, changes);
        } else {
            log.info("Change does not require approval - auto-syncing to federated party system");
            // Could trigger automatic sync here
        }
    }

    /**
     * Determine if changes require workflow approval
     */
    private boolean requiresApproval(String eventType, JsonNode changes) {
        // Material changes that require approval
        if ("PARTY_RISK_RATING_CHANGED".equals(eventType)) {
            return true;
        }

        if ("PARTY_STATUS_CHANGED".equals(eventType)) {
            String newStatus = changes.get("newStatus").asText();
            // Status changes to SUSPENDED or TERMINATED require approval
            return "SUSPENDED".equals(newStatus) || "TERMINATED".equals(newStatus);
        }

        if ("PARTY_LEI_CHANGED".equals(eventType)) {
            // LEI changes are material
            return true;
        }

        if ("PARTY_JURISDICTION_CHANGED".equals(eventType)) {
            // Jurisdiction changes require approval
            return true;
        }

        if ("PARTY_CONTROL_CHANGE".equals(eventType)) {
            // Changes in control (ownership > 25%) require approval
            return true;
        }

        // Default: non-material changes don't require approval
        return false;
    }

    /**
     * Trigger CIC workflow for party changes
     */
    private void triggerCICWorkflow(String partyId, String sourceSystem, String eventType, JsonNode changes) {
        try {
            // Build workflow request
            Map<String, Object> entityData = new HashMap<>();
            entityData.put("partyId", partyId);
            entityData.put("sourceSystem", sourceSystem);
            entityData.put("eventType", eventType);
            entityData.put("changes", objectMapper.convertValue(changes, Map.class));

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("changeType", eventType);
            metadata.put("sourceSystem", sourceSystem);

            // Determine priority based on event type
            String priority = determinePriority(eventType);

            WorkflowSubmitRequest request = WorkflowSubmitRequest.builder()
                    .entityType("PARTY_CHANGE")
                    .entityId(partyId)
                    .entityData(entityData)
                    .entityMetadata(metadata)
                    .templateId("party-cic-approval")
                    .businessJustification("Change in Circumstance detected in source system: " + eventType)
                    .tenantId("system")
                    .initiatedBy("system")
                    .priority(priority)
                    .build();

            // Submit workflow
            workflowExecutionService.submitWorkflow(request);

            log.info("Successfully triggered CIC workflow for party {} - Event: {}", partyId, eventType);

        } catch (Exception e) {
            log.error("Failed to trigger CIC workflow for party {}", partyId, e);
        }
    }

    /**
     * Determine workflow priority based on event type
     */
    private String determinePriority(String eventType) {
        return switch (eventType) {
            case "PARTY_STATUS_CHANGED", "PARTY_CONTROL_CHANGE" -> "HIGH";
            case "PARTY_RISK_RATING_CHANGED", "PARTY_LEI_CHANGED" -> "MEDIUM";
            default -> "LOW";
        };
    }
}
