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
 * Handles approval of Change in Circumstance (CIC) events for parties
 * Triggers sync to federated party system after approval
 */
@Component("partyChangeApprovalHandler")
@RequiredArgsConstructor
@Slf4j
public class PartyChangeApprovalHandler implements WorkflowCallbackHandler {

    private final RestTemplate restTemplate;

    @Override
    public void handle(WorkflowSubject subject) throws Exception {
        log.info("Handling party change approval for workflow: {}", subject.getWorkflowId());

        Map<String, Object> entityData = subject.getEntityData();
        String partyId = (String) entityData.get("partyId");
        String sourceSystem = (String) entityData.get("sourceSystem");
        String eventType = (String) entityData.get("eventType");

        if (partyId == null || sourceSystem == null) {
            throw new IllegalArgumentException("Party ID and source system are required");
        }

        log.info("Approved CIC for party {} from {} - Event: {}", partyId, sourceSystem, eventType);

        // Trigger sync to federated party system
        String partyServiceUrl = "http://party-service:8083/api/v1/parties/sync";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                    Map.of(
                            "sourceSystem", sourceSystem,
                            "sourceId", partyId,
                            "workflowId", subject.getWorkflowId(),
                            "forceSync", true, // Force re-sync even if already exists
                            "changeEvent", eventType
                    ),
                    headers
            );

            restTemplate.postForEntity(partyServiceUrl, request, Void.class);

            log.info("Successfully synced party {} to federated system after CIC approval", partyId);

        } catch (Exception e) {
            log.error("Failed to sync party {} after CIC approval", partyId, e);
            throw new RuntimeException("Failed to sync party: " + e.getMessage(), e);
        }
    }
}
