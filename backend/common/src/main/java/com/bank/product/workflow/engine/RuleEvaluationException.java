package com.bank.product.workflow.engine;

/**
 * Exception thrown during rule evaluation
 */
public class RuleEvaluationException extends Exception {

    private String ruleId;
    private String tableId;

    public RuleEvaluationException(String message) {
        super(message);
    }

    public RuleEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuleEvaluationException(String ruleId, String tableId, String message) {
        super(message);
        this.ruleId = ruleId;
        this.tableId = tableId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getTableId() {
        return tableId;
    }
}
