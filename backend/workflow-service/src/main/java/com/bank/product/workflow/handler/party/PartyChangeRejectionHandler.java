package com.bank.product.workflow.handler.party;

import com.bank.product.workflow.domain.model.WorkflowSubject;
import com.bank.product.workflow.domain.service.WorkflowCallbackHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handles rejection of Change in Circumstance (CIC) events for parties
 * Logs the rejection - does not sync changes to federated system
 */
@Component("partyChangeRejectionHandler")
@RequiredArgsConstructor
@Slf4j
public class PartyChangeRejectionHandler implements WorkflowCallbackHandler {

    @Override
    public void handle(WorkflowSubject subject) throws Exception {
        log.info("Handling party change rejection for workflow: {}", subject.getWorkflowId());

        Map<String, Object> entityData = subject.getEntityData();
        String partyId = (String) entityData.get("partyId");
        String sourceSystem = (String) entityData.get("sourceSystem");
        String eventType = (String) entityData.get("eventType");
        String rejectionReason = subject.getErrorMessage();

        log.warn("Rejected CIC for party {} from {} - Event: {} - Reason: {}",
                partyId, sourceSystem, eventType, rejectionReason);

        // Could send notification to source system or compliance team
        log.info("CIC rejected - changes will NOT be synced to federated party system");

        // In a real implementation, might:
        // 1. Send alert to compliance team
        // 2. Notify source system of rejection
        // 3. Create audit record
        // 4. Escalate if needed
    }
}
