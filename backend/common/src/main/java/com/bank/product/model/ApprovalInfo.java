package com.bank.product.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApprovalInfo {

    private boolean requiresApproval;

    private ApprovalStatus approvalStatus;

    private String approvedBy;

    private LocalDateTime approvedAt;

    private String rejectedBy;

    private LocalDateTime rejectedAt;

    private String rejectionReason;

    private List<ApprovalStep> approvalSteps;

    private String currentApprover;

    private String comments;
}

enum ApprovalStatus {
    NOT_REQUIRED,
    PENDING,
    APPROVED,
    REJECTED
}