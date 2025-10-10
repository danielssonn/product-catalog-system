package com.bank.product.workflow.handler.party;

import com.bank.product.workflow.domain.model.WorkflowSubject;
import com.bank.product.workflow.domain.service.WorkflowCallbackHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Handles rejection of party relationship creation requests
 * Marks the relationship as rejected and records the reason
 */
@Component("relationshipRejectionHandler")
@RequiredArgsConstructor
@Slf4j
public class RelationshipRejectionHandler implements WorkflowCallbackHandler {

    private final RestTemplate restTemplate;

    @Override
    public void handle(WorkflowSubject subject) throws Exception {
        log.info("Handling relationship rejection for workflow: {}", subject.getWorkflowId());

        Map<String, Object> entityData = subject.getEntityData();
        String relationshipId = (String) entityData.get("relationshipId");
        String relationshipType = (String) entityData.get("relationshipType");
        String rejectionReason = subject.getErrorMessage(); // Contains rejection reason

        if (relationshipId == null) {
            throw new IllegalArgumentException("Relationship ID is required");
        }

        log.info("Rejecting {} relationship: {} - Reason: {}", relationshipType, relationshipId, rejectionReason);

        // Call party-service to mark relationship as rejected
        String partyServiceUrl = "http://party-service:8083/api/v1/relationships/" + relationshipType + "/" + relationshipId + "/reject";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                    Map.of(
                            "workflowId", subject.getWorkflowId(),
                            "rejectionReason", rejectionReason != null ? rejectionReason : "Rejected by approver",
                            "rejectionTimestamp", System.currentTimeMillis()
                    ),
                    headers
            );

            restTemplate.postForEntity(partyServiceUrl, request, Void.class);

            log.info("Successfully rejected relationship: {}", relationshipId);

        } catch (Exception e) {
            log.error("Failed to reject relationship: {}", relationshipId, e);
            throw new RuntimeException("Failed to reject relationship: " + e.getMessage(), e);
        }
    }
}
