package com.bank.product.workflow.kafka;

import com.bank.product.workflow.domain.model.ApprovalDecision;
import com.bank.product.workflow.domain.model.WorkflowSubject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishWorkflowApproved(WorkflowSubject subject, List<ApprovalDecision> approvals) {
        log.info("Publishing workflow approved event: workflowId={}", subject.getWorkflowId());

        Map<String, Object> event = new HashMap<>();
        event.put("workflowId", subject.getWorkflowId());
        event.put("workflowInstanceId", subject.getWorkflowInstanceId());
        event.put("entityType", subject.getEntityType());
        event.put("entityId", subject.getEntityId());
        event.put("tenantId", subject.getTenantId());
        event.put("entityData", subject.getEntityData());
        event.put("entityMetadata", subject.getEntityMetadata());
        event.put("initiatedBy", subject.getInitiatedBy());
        event.put("submittedAt", subject.getInitiatedAt());
        event.put("approvedAt", LocalDateTime.now());

        // Convert approvals
        List<Map<String, Object>> approvalInfos = approvals.stream()
                .map(approval -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("approverId", approval.getApproverId());
                    info.put("approverRole", ""); // Role not tracked in ApprovalDecision
                    info.put("comments", approval.getComments());
                    info.put("approvedAt", approval.getTimestamp());
                    return info;
                })
                .collect(Collectors.toList());

        event.put("approvals", approvalInfos);

        kafkaTemplate.send("workflow.approved", subject.getWorkflowId(), event);
        log.info("Published workflow approved event to Kafka topic: workflow.approved");
    }

    public void publishWorkflowRejected(WorkflowSubject subject, List<ApprovalDecision> rejections) {
        log.info("Publishing workflow rejected event: workflowId={}", subject.getWorkflowId());

        // Find the rejection decision
        ApprovalDecision rejection = rejections.stream()
                .filter(d -> "REJECT".equals(d.getDecision()))
                .findFirst()
                .orElse(null);

        Map<String, Object> event = new HashMap<>();
        event.put("workflowId", subject.getWorkflowId());
        event.put("workflowInstanceId", subject.getWorkflowInstanceId());
        event.put("entityType", subject.getEntityType());
        event.put("entityId", subject.getEntityId());
        event.put("tenantId", subject.getTenantId());
        event.put("entityData", subject.getEntityData());
        event.put("initiatedBy", subject.getInitiatedBy());
        event.put("submittedAt", subject.getInitiatedAt());
        event.put("rejectedAt", LocalDateTime.now());

        if (rejection != null) {
            event.put("rejectedBy", rejection.getApproverId());
            event.put("rejectionReason", rejection.getRejectionReason());
            event.put("rejectionComments", rejection.getComments());
        }

        kafkaTemplate.send("workflow.rejected", subject.getWorkflowId(), event);
        log.info("Published workflow rejected event to Kafka topic: workflow.rejected");
    }
}
