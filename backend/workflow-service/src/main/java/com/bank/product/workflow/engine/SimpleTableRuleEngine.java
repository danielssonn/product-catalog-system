package com.bank.product.workflow.engine;

import com.bank.product.workflow.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple JSON-based decision table rule engine
 * Evaluates rules without external DMN/Drools engines
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimpleTableRuleEngine implements RuleEngine {

    private final ConditionEvaluator conditionEvaluator;

    /**
     * Record for tracking evaluation results with matched rule IDs
     */
    private record EvaluationResult(Map<String, Object> outputs, List<String> matchedRuleIds) {}

    @Override
    public ComputedApprovalPlan evaluate(WorkflowTemplate template, Map<String, Object> entityMetadata) {
        log.debug("Evaluating template '{}' against metadata: {}", template.getTemplateId(), entityMetadata);

        if (template.getDecisionTables() == null || template.getDecisionTables().isEmpty()) {
            log.warn("No decision tables found in template '{}'", template.getTemplateId());
            return createDefaultApprovalPlan();
        }

        // Aggregate results from all decision tables
        Map<String, Object> aggregatedOutputs = new HashMap<>();
        List<String> matchedRuleIds = new ArrayList<>();

        for (DecisionTable table : template.getDecisionTables()) {
            EvaluationResult result = evaluateDecisionTableWithTracking(table, entityMetadata);
            aggregatedOutputs.putAll(result.outputs());
            matchedRuleIds.addAll(result.matchedRuleIds());
        }

        // Build approval plan from aggregated outputs
        return buildApprovalPlan(aggregatedOutputs, template, matchedRuleIds);
    }

    @Override
    public Map<String, Object> testEvaluate(WorkflowTemplate template, Map<String, Object> entityMetadata) {
        log.debug("Test evaluating template '{}' against metadata: {}", template.getTemplateId(), entityMetadata);

        Map<String, Object> allOutputs = new HashMap<>();

        if (template.getDecisionTables() != null) {
            for (DecisionTable table : template.getDecisionTables()) {
                Map<String, Object> tableOutputs = evaluateDecisionTable(table, entityMetadata);
                allOutputs.put(table.getName(), tableOutputs);
            }
        }

        return allOutputs;
    }

    /**
     * Evaluate a single decision table
     */
    private Map<String, Object> evaluateDecisionTable(DecisionTable table, Map<String, Object> entityMetadata) {
        return evaluateDecisionTableWithTracking(table, entityMetadata).outputs();
    }

    /**
     * Evaluate a single decision table with tracking of matched rules
     */
    private EvaluationResult evaluateDecisionTableWithTracking(DecisionTable table, Map<String, Object> entityMetadata) {
        log.debug("Evaluating decision table: {}", table.getName());

        HitPolicy hitPolicy = table.getHitPolicy() != null ? table.getHitPolicy() : HitPolicy.FIRST;

        // Sort rules by priority if using PRIORITY hit policy
        List<DecisionRule> sortedRules = table.getRules();
        if (hitPolicy == HitPolicy.PRIORITY) {
            sortedRules = table.getRules().stream()
                    .sorted(Comparator.comparingInt(DecisionRule::getPriority).reversed())
                    .collect(Collectors.toList());
        }

        // Evaluate rules based on hit policy
        return switch (hitPolicy) {
            case FIRST -> evaluateFirstHitPolicyWithTracking(sortedRules, entityMetadata, table);
            case ALL -> evaluateAllHitPolicyWithTracking(sortedRules, entityMetadata, table);
            case PRIORITY -> evaluatePriorityHitPolicyWithTracking(sortedRules, entityMetadata, table);
            case COLLECT -> evaluateCollectHitPolicyWithTracking(sortedRules, entityMetadata, table);
        };
    }

    /**
     * FIRST hit policy: Return outputs of first matching rule
     */
    private Map<String, Object> evaluateFirstHitPolicy(List<DecisionRule> rules,
                                                        Map<String, Object> entityMetadata,
                                                        DecisionTable table) {
        for (DecisionRule rule : rules) {
            if (ruleMatches(rule, entityMetadata)) {
                log.debug("Rule '{}' matched (FIRST policy)", rule.getRuleId());
                return rule.getOutputs();
            }
        }

        // No rules matched - use default rule or default values
        return getDefaultOutputs(table);
    }

    /**
     * ALL hit policy: Merge outputs from all matching rules
     */
    private Map<String, Object> evaluateAllHitPolicy(List<DecisionRule> rules,
                                                      Map<String, Object> entityMetadata,
                                                      DecisionTable table) {
        Map<String, Object> mergedOutputs = new HashMap<>();

        for (DecisionRule rule : rules) {
            if (ruleMatches(rule, entityMetadata)) {
                log.debug("Rule '{}' matched (ALL policy)", rule.getRuleId());
                mergedOutputs.putAll(rule.getOutputs());
            }
        }

        if (mergedOutputs.isEmpty()) {
            return getDefaultOutputs(table);
        }

        return mergedOutputs;
    }

    /**
     * PRIORITY hit policy: Return outputs of highest priority matching rule
     */
    private Map<String, Object> evaluatePriorityHitPolicy(List<DecisionRule> rules,
                                                           Map<String, Object> entityMetadata,
                                                           DecisionTable table) {
        // Rules already sorted by priority
        for (DecisionRule rule : rules) {
            if (ruleMatches(rule, entityMetadata)) {
                log.debug("Rule '{}' matched with priority {} (PRIORITY policy)",
                         rule.getRuleId(), rule.getPriority());
                return rule.getOutputs();
            }
        }

        return getDefaultOutputs(table);
    }

    /**
     * COLLECT hit policy: Collect all outputs from matching rules into lists
     */
    private Map<String, Object> evaluateCollectHitPolicy(List<DecisionRule> rules,
                                                          Map<String, Object> entityMetadata,
                                                          DecisionTable table) {
        Map<String, List<Object>> collectedOutputs = new HashMap<>();

        for (DecisionRule rule : rules) {
            if (ruleMatches(rule, entityMetadata)) {
                log.debug("Rule '{}' matched (COLLECT policy)", rule.getRuleId());

                for (Map.Entry<String, Object> entry : rule.getOutputs().entrySet()) {
                    collectedOutputs.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                            .add(entry.getValue());
                }
            }
        }

        if (collectedOutputs.isEmpty()) {
            return getDefaultOutputs(table);
        }

        // Convert to regular map
        return new HashMap<>(collectedOutputs);
    }

    /**
     * FIRST hit policy with tracking: Return outputs of first matching rule
     */
    private EvaluationResult evaluateFirstHitPolicyWithTracking(List<DecisionRule> rules,
                                                                  Map<String, Object> entityMetadata,
                                                                  DecisionTable table) {
        for (DecisionRule rule : rules) {
            if (ruleMatches(rule, entityMetadata)) {
                log.debug("Rule '{}' matched (FIRST policy)", rule.getRuleId());
                return new EvaluationResult(rule.getOutputs(), List.of(rule.getRuleId()));
            }
        }

        // No rules matched - use default rule or default values
        return new EvaluationResult(getDefaultOutputs(table), List.of());
    }

    /**
     * ALL hit policy with tracking: Merge outputs from all matching rules
     */
    private EvaluationResult evaluateAllHitPolicyWithTracking(List<DecisionRule> rules,
                                                                Map<String, Object> entityMetadata,
                                                                DecisionTable table) {
        Map<String, Object> mergedOutputs = new HashMap<>();
        List<String> matchedRuleIds = new ArrayList<>();

        for (DecisionRule rule : rules) {
            if (ruleMatches(rule, entityMetadata)) {
                log.debug("Rule '{}' matched (ALL policy)", rule.getRuleId());
                mergedOutputs.putAll(rule.getOutputs());
                matchedRuleIds.add(rule.getRuleId());
            }
        }

        if (mergedOutputs.isEmpty()) {
            return new EvaluationResult(getDefaultOutputs(table), List.of());
        }

        return new EvaluationResult(mergedOutputs, matchedRuleIds);
    }

    /**
     * PRIORITY hit policy with tracking: Return outputs of highest priority matching rule
     */
    private EvaluationResult evaluatePriorityHitPolicyWithTracking(List<DecisionRule> rules,
                                                                     Map<String, Object> entityMetadata,
                                                                     DecisionTable table) {
        // Rules already sorted by priority
        for (DecisionRule rule : rules) {
            if (ruleMatches(rule, entityMetadata)) {
                log.debug("Rule '{}' matched with priority {} (PRIORITY policy)",
                         rule.getRuleId(), rule.getPriority());
                return new EvaluationResult(rule.getOutputs(), List.of(rule.getRuleId()));
            }
        }

        return new EvaluationResult(getDefaultOutputs(table), List.of());
    }

    /**
     * COLLECT hit policy with tracking: Collect all outputs from matching rules into lists
     */
    private EvaluationResult evaluateCollectHitPolicyWithTracking(List<DecisionRule> rules,
                                                                    Map<String, Object> entityMetadata,
                                                                    DecisionTable table) {
        Map<String, List<Object>> collectedOutputs = new HashMap<>();
        List<String> matchedRuleIds = new ArrayList<>();

        for (DecisionRule rule : rules) {
            if (ruleMatches(rule, entityMetadata)) {
                log.debug("Rule '{}' matched (COLLECT policy)", rule.getRuleId());
                matchedRuleIds.add(rule.getRuleId());

                for (Map.Entry<String, Object> entry : rule.getOutputs().entrySet()) {
                    collectedOutputs.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                            .add(entry.getValue());
                }
            }
        }

        if (collectedOutputs.isEmpty()) {
            return new EvaluationResult(getDefaultOutputs(table), List.of());
        }

        // Convert to regular map
        return new EvaluationResult(new HashMap<>(collectedOutputs), matchedRuleIds);
    }

    /**
     * Check if a rule matches the entity metadata
     */
    private boolean ruleMatches(DecisionRule rule, Map<String, Object> entityMetadata) {
        if (rule.getConditions() == null || rule.getConditions().isEmpty()) {
            return true; // Rule with no conditions always matches
        }

        // All conditions must match (AND logic)
        for (Map.Entry<String, String> condition : rule.getConditions().entrySet()) {
            String inputName = condition.getKey();
            String conditionExpression = condition.getValue();

            Object value = entityMetadata.get(inputName);

            if (!conditionEvaluator.evaluate(value, conditionExpression)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Get default outputs for a table
     */
    private Map<String, Object> getDefaultOutputs(DecisionTable table) {
        Map<String, Object> defaults = new HashMap<>();

        // Use default rule if specified
        if (table.getDefaultRuleId() != null && table.getRules() != null) {
            for (DecisionRule rule : table.getRules()) {
                if (table.getDefaultRuleId().equals(rule.getRuleId())) {
                    log.debug("Using default rule: {}", rule.getRuleId());
                    return rule.getOutputs();
                }
            }
        }

        // Use default values from output definitions
        if (table.getOutputs() != null) {
            for (DecisionOutput output : table.getOutputs()) {
                if (output.getDefaultValue() != null) {
                    defaults.put(output.getName(), output.getDefaultValue());
                }
            }
        }

        return defaults;
    }

    /**
     * Build approval plan from decision table outputs
     */
    private ComputedApprovalPlan buildApprovalPlan(Map<String, Object> outputs,
                                                    WorkflowTemplate template,
                                                    List<String> matchedRuleIds) {
        ComputedApprovalPlan.ComputedApprovalPlanBuilder builder = ComputedApprovalPlan.builder();

        // Extract standard fields
        builder.approvalRequired(getBoolean(outputs, "approvalRequired", true));
        builder.requiredApprovals(getInteger(outputs, "approvalCount", 1));
        builder.sequential(getBoolean(outputs, "isSequential", false));

        // Extract approver roles
        List<String> approverRoles = getStringList(outputs, "approverRoles");
        builder.approverRoles(approverRoles);

        // Extract specific approvers
        List<String> specificApprovers = getStringList(outputs, "specificApprovers");
        builder.specificApprovers(specificApprovers);

        // Extract SLA
        Integer slaHours = getInteger(outputs, "slaHours", 24);
        builder.sla(Duration.ofHours(slaHours));

        // Use escalation rules from template
        if (template.getEscalationRules() != null) {
            builder.escalationRules(template.getEscalationRules());
        }

        // Set matched rule IDs
        builder.matchedRules(matchedRuleIds);

        // Store all outputs as additional config
        builder.additionalConfig(outputs);

        return builder.build();
    }

    /**
     * Create default approval plan (single approval required)
     */
    private ComputedApprovalPlan createDefaultApprovalPlan() {
        return ComputedApprovalPlan.builder()
                .approvalRequired(true)
                .requiredApprovals(1)
                .approverRoles(List.of("MANAGER"))
                .sequential(false)
                .sla(Duration.ofHours(24))
                .additionalConfig(new HashMap<>())
                .build();
    }

    // Helper methods for extracting typed values

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private Integer getInteger(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof List) {
            return ((List<?>) value).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        // Single value - wrap in list
        return List.of(value.toString());
    }
}
