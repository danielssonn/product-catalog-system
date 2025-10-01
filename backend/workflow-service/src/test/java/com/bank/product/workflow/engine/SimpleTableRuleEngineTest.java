package com.bank.product.workflow.engine;

import com.bank.product.workflow.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SimpleTableRuleEngine
 */
class SimpleTableRuleEngineTest {

    private SimpleTableRuleEngine ruleEngine;
    private ConditionEvaluator conditionEvaluator;

    @BeforeEach
    void setUp() {
        conditionEvaluator = new ConditionEvaluator();
        ruleEngine = new SimpleTableRuleEngine(conditionEvaluator);
    }

    @Test
    void testFirstHitPolicy_SimpleRule() {
        // Arrange
        WorkflowTemplate template = createTemplateWithSimpleRule();
        Map<String, Object> metadata = Map.of(
                "solutionType", "CHECKING",
                "pricingVariance", 5
        );

        // Act
        ComputedApprovalPlan plan = ruleEngine.evaluate(template, metadata);

        // Assert
        assertTrue(plan.isApprovalRequired());
        assertEquals(1, plan.getRequiredApprovals());
        assertEquals(List.of("PRODUCT_MANAGER"), plan.getApproverRoles());
        assertFalse(plan.isSequential());
    }

    @Test
    void testFirstHitPolicy_HighVariance() {
        // Arrange
        WorkflowTemplate template = createTemplateWithMultipleRules();
        Map<String, Object> metadata = Map.of(
                "solutionType", "CHECKING",
                "pricingVariance", 15,
                "riskLevel", "MEDIUM"
        );

        // Act
        ComputedApprovalPlan plan = ruleEngine.evaluate(template, metadata);

        // Assert
        assertTrue(plan.isApprovalRequired());
        assertEquals(2, plan.getRequiredApprovals());
        assertEquals(List.of("PRODUCT_MANAGER", "RISK_MANAGER"), plan.getApproverRoles());
        assertTrue(plan.isSequential());
    }

    @Test
    void testPriorityHitPolicy() {
        // Arrange
        WorkflowTemplate template = createTemplateWithPriorityRules();
        Map<String, Object> metadata = Map.of(
                "loanAmount", 600000,
                "creditScore", 720
        );

        // Act
        ComputedApprovalPlan plan = ruleEngine.evaluate(template, metadata);

        // Assert
        assertTrue(plan.isApprovalRequired());
        // Should match the high-value rule (higher priority)
        assertEquals(List.of("CREDIT_COMMITTEE"), plan.getApproverRoles());
    }

    @Test
    void testAutoApprovalRule() {
        // Arrange
        WorkflowTemplate template = createTemplateWithAutoApproval();
        Map<String, Object> metadata = Map.of(
                "creditScore", 780,
                "loanAmount", 100000
        );

        // Act
        ComputedApprovalPlan plan = ruleEngine.evaluate(template, metadata);

        // Assert
        assertFalse(plan.isApprovalRequired());
        assertEquals(0, plan.getRequiredApprovals());
    }

    @Test
    void testNoMatchingRule_UsesDefault() {
        // Arrange
        WorkflowTemplate template = createTemplateWithSimpleRule();
        Map<String, Object> metadata = Map.of(
                "solutionType", "UNKNOWN",
                "pricingVariance", 999
        );

        // Act
        ComputedApprovalPlan plan = ruleEngine.evaluate(template, metadata);

        // Assert
        // Should use default values or default rule
        assertNotNull(plan);
    }

    @Test
    void testEmptyTemplate_ReturnsDefaultPlan() {
        // Arrange
        WorkflowTemplate template = WorkflowTemplate.builder()
                .templateId("EMPTY_TEMPLATE")
                .entityType("TEST")
                .decisionTables(List.of())
                .build();

        Map<String, Object> metadata = Map.of("key", "value");

        // Act
        ComputedApprovalPlan plan = ruleEngine.evaluate(template, metadata);

        // Assert
        assertNotNull(plan);
        assertTrue(plan.isApprovalRequired());
        assertEquals(1, plan.getRequiredApprovals());
    }

    // Helper methods to create test templates

    private WorkflowTemplate createTemplateWithSimpleRule() {
        DecisionRule rule = DecisionRule.builder()
                .ruleId("RULE_1")
                .priority(1)
                .conditions(Map.of(
                        "solutionType", "CHECKING",
                        "pricingVariance", "<= 10"
                ))
                .outputs(Map.of(
                        "approvalRequired", true,
                        "approverRoles", List.of("PRODUCT_MANAGER"),
                        "approvalCount", 1,
                        "isSequential", false,
                        "slaHours", 24
                ))
                .build();

        DecisionTable table = DecisionTable.builder()
                .name("Solution Approval Rules")
                .hitPolicy(HitPolicy.FIRST)
                .rules(List.of(rule))
                .build();

        return WorkflowTemplate.builder()
                .templateId("SOLUTION_CONFIG_V1")
                .entityType("SOLUTION_CONFIGURATION")
                .decisionTables(List.of(table))
                .build();
    }

    private WorkflowTemplate createTemplateWithMultipleRules() {
        DecisionRule rule1 = DecisionRule.builder()
                .ruleId("LOW_VARIANCE")
                .priority(1)
                .conditions(Map.of(
                        "pricingVariance", "<= 10"
                ))
                .outputs(Map.of(
                        "approvalRequired", true,
                        "approverRoles", List.of("PRODUCT_MANAGER"),
                        "approvalCount", 1,
                        "isSequential", false,
                        "slaHours", 24
                ))
                .build();

        DecisionRule rule2 = DecisionRule.builder()
                .ruleId("HIGH_VARIANCE")
                .priority(2)
                .conditions(Map.of(
                        "pricingVariance", "> 10"
                ))
                .outputs(Map.of(
                        "approvalRequired", true,
                        "approverRoles", List.of("PRODUCT_MANAGER", "RISK_MANAGER"),
                        "approvalCount", 2,
                        "isSequential", true,
                        "slaHours", 48
                ))
                .build();

        DecisionTable table = DecisionTable.builder()
                .name("Solution Approval Rules")
                .hitPolicy(HitPolicy.FIRST)
                .rules(List.of(rule1, rule2))
                .build();

        return WorkflowTemplate.builder()
                .templateId("SOLUTION_CONFIG_V1")
                .entityType("SOLUTION_CONFIGURATION")
                .decisionTables(List.of(table))
                .build();
    }

    private WorkflowTemplate createTemplateWithPriorityRules() {
        DecisionRule highValueRule = DecisionRule.builder()
                .ruleId("HIGH_VALUE")
                .priority(100)
                .conditions(Map.of("loanAmount", "> 500000"))
                .outputs(Map.of(
                        "approvalRequired", true,
                        "approverRoles", List.of("CREDIT_COMMITTEE"),
                        "approvalCount", 1,
                        "slaHours", 72
                ))
                .build();

        DecisionRule standardRule = DecisionRule.builder()
                .ruleId("STANDARD")
                .priority(50)
                .conditions(Map.of("creditScore", ">= 680"))
                .outputs(Map.of(
                        "approvalRequired", true,
                        "approverRoles", List.of("CREDIT_OFFICER"),
                        "approvalCount", 1,
                        "slaHours", 48
                ))
                .build();

        DecisionTable table = DecisionTable.builder()
                .name("Loan Approval Rules")
                .hitPolicy(HitPolicy.PRIORITY)
                .rules(List.of(standardRule, highValueRule)) // Intentionally unsorted
                .build();

        return WorkflowTemplate.builder()
                .templateId("LOAN_APPROVAL_V1")
                .entityType("LOAN_APPLICATION")
                .decisionTables(List.of(table))
                .build();
    }

    private WorkflowTemplate createTemplateWithAutoApproval() {
        DecisionRule autoApprove = DecisionRule.builder()
                .ruleId("AUTO_APPROVE")
                .priority(100)
                .conditions(Map.of(
                        "creditScore", ">= 750",
                        "loanAmount", "<= 200000"
                ))
                .outputs(Map.of(
                        "approvalRequired", false,
                        "approvalCount", 0,
                        "slaHours", 0
                ))
                .build();

        DecisionTable table = DecisionTable.builder()
                .name("Loan Approval Rules")
                .hitPolicy(HitPolicy.FIRST)
                .rules(List.of(autoApprove))
                .build();

        return WorkflowTemplate.builder()
                .templateId("LOAN_AUTO_APPROVAL_V1")
                .entityType("LOAN_APPLICATION")
                .decisionTables(List.of(table))
                .build();
    }
}
