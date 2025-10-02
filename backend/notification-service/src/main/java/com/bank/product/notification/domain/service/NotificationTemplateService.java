package com.bank.product.notification.domain.service;

import com.bank.product.notification.domain.model.NotificationType;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationTemplateService {

    public String generateMessage(NotificationType type, Map<String, Object> templateData) {
        return switch (type) {
            case WORKFLOW_APPROVED -> generateWorkflowApprovedMessage(templateData);
            case WORKFLOW_REJECTED -> generateWorkflowRejectedMessage(templateData);
            case WORKFLOW_SUBMITTED -> generateWorkflowSubmittedMessage(templateData);
            case APPROVAL_TASK_ASSIGNED -> generateApprovalTaskAssignedMessage(templateData);
            case SOLUTION_ACTIVATED -> generateSolutionActivatedMessage(templateData);
            case SOLUTION_REJECTED -> generateSolutionRejectedMessage(templateData);
            default -> "Workflow notification";
        };
    }

    private String generateWorkflowApprovedMessage(Map<String, Object> data) {
        return String.format("""
                Your workflow has been approved!

                Workflow ID: %s
                Entity Type: %s
                Entity ID: %s
                Approved At: %s

                The workflow has been successfully approved and the changes are now active.

                Thank you for using our workflow system.
                """,
                data.get("workflowId"),
                data.get("entityType"),
                data.get("entityId"),
                data.get("approvedAt")
        );
    }

    private String generateWorkflowRejectedMessage(Map<String, Object> data) {
        return String.format("""
                Your workflow has been rejected.

                Workflow ID: %s
                Entity Type: %s
                Entity ID: %s
                Rejected By: %s
                Rejected At: %s

                Rejection Reason: %s

                Comments: %s

                Please review the feedback and resubmit if necessary.
                """,
                data.get("workflowId"),
                data.get("entityType"),
                data.get("entityId"),
                data.get("rejectedBy"),
                data.get("rejectedAt"),
                data.get("rejectionReason"),
                data.get("rejectionComments")
        );
    }

    private String generateWorkflowSubmittedMessage(Map<String, Object> data) {
        return String.format("""
                Your workflow has been submitted for approval.

                Workflow ID: %s
                Entity Type: %s
                Entity ID: %s

                The workflow is now pending approval. You will be notified once a decision is made.
                """,
                data.get("workflowId"),
                data.get("entityType"),
                data.get("entityId")
        );
    }

    private String generateApprovalTaskAssignedMessage(Map<String, Object> data) {
        return String.format("""
                A new approval task has been assigned to you.

                Workflow ID: %s
                Entity Type: %s
                Entity ID: %s

                Please review and approve or reject this workflow.
                """,
                data.get("workflowId"),
                data.get("entityType"),
                data.get("entityId")
        );
    }

    private String generateSolutionActivatedMessage(Map<String, Object> data) {
        return String.format("""
                Solution has been activated!

                Solution ID: %s
                Solution Name: %s

                The solution is now active and available for use.
                """,
                data.get("solutionId"),
                data.get("solutionName")
        );
    }

    private String generateSolutionRejectedMessage(Map<String, Object> data) {
        return String.format("""
                Solution has been rejected.

                Solution ID: %s
                Solution Name: %s

                Reason: %s
                """,
                data.get("solutionId"),
                data.get("solutionName"),
                data.get("reason")
        );
    }
}
