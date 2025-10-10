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
 * Handles approval of party relationship creation requests
 * Activates the relationship after all approvals are complete
 */
@Component("relationshipApprovalHandler")
@RequiredArgsConstructor
@Slf4j
public class RelationshipApprovalHandler implements WorkflowCallbackHandler {

    private final RestTemplate restTemplate;

    @Override
    public void handle(WorkflowSubject subject) throws Exception {
        log.info("Handling relationship approval for workflow: {}", subject.getWorkflowId());

        Map<String, Object> entityData = subject.getEntityData();
        String relationshipId = (String) entityData.get("relationshipId");
        String relationshipType = (String) entityData.get("relationshipType");

        if (relationshipId == null) {
            throw new IllegalArgumentException("Relationship ID is required");
        }

        log.info("Activating {} relationship: {}", relationshipType, relationshipId);

        // Call party-service to activate the relationship
        String partyServiceUrl = "http://party-service:8083/api/v1/relationships/" + relationshipType + "/" + relationshipId + "/activate";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                    Map.of(
                            "workflowId", subject.getWorkflowId(),
                            "approvalTimestamp", System.currentTimeMillis()
                    ),
                    headers
            );

            restTemplate.postForEntity(partyServiceUrl, request, Void.class);

            log.info("Successfully activated relationship: {}", relationshipId);

        } catch (Exception e) {
            log.error("Failed to activate relationship: {}", relationshipId, e);
            throw new RuntimeException("Failed to activate relationship: " + e.getMessage(), e);
        }
    }
}
