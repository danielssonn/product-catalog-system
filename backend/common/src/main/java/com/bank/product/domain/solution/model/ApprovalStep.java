package com.bank.product.domain.solution.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalStep {

    private int stepNumber;

    private String stepName;

    private String approverRole;

    private String approverId;

    private String approverName;

    private String status;

    private LocalDateTime completedAt;

    private String comments;
}