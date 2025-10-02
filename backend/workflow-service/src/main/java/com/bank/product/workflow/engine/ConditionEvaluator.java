package com.bank.product.workflow.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates condition expressions against metadata values
 *
 * Supported operators:
 * - Equality: ==, !=
 * - Comparison: >, <, >=, <=
 * - Logical: &&, ||
 * - String: contains, startsWith, endsWith
 * - Regex: matches
 */
@Slf4j
@Component
public class ConditionEvaluator {

    // Regex patterns for different condition types
    private static final Pattern COMPARISON_PATTERN = Pattern.compile("^\\s*([><=!]+)\\s*(.+)$");
    private static final Pattern RANGE_PATTERN = Pattern.compile("^\\s*([><=]+)\\s*(.+?)\\s*(&&|\\|\\|)\\s*([><=]+)\\s*(.+)$");
    private static final Pattern STRING_OP_PATTERN = Pattern.compile("^\\s*(contains|startsWith|endsWith|matches)\\s+['\"](.+)['\"]$");
    private static final Pattern OR_PATTERN = Pattern.compile("\\|");

    /**
     * Evaluate a condition expression against a value
     *
     * @param value actual value from metadata
     * @param conditionExpression condition expression to evaluate
     * @return true if condition matches
     */
    public boolean evaluate(Object value, String conditionExpression) {
        if (value == null || conditionExpression == null) {
            return false;
        }

        String condition = conditionExpression.trim();

        try {
            // Handle OR conditions (pipe-separated values)
            if (OR_PATTERN.matcher(condition).find()) {
                return evaluateOrCondition(value, condition);
            }

            // Handle range conditions (e.g., "> 10 && <= 100")
            Matcher rangeMatcher = RANGE_PATTERN.matcher(condition);
            if (rangeMatcher.matches()) {
                return evaluateRangeCondition(value, rangeMatcher);
            }

            // Handle string operations
            if (value instanceof String) {
                Matcher stringOpMatcher = STRING_OP_PATTERN.matcher(condition);
                if (stringOpMatcher.matches()) {
                    return evaluateStringOperation((String) value, stringOpMatcher);
                }
            }

            // Handle comparison operators
            Matcher comparisonMatcher = COMPARISON_PATTERN.matcher(condition);
            if (comparisonMatcher.matches()) {
                return evaluateComparison(value, comparisonMatcher);
            }

            // Handle direct equality (no operator)
            return evaluateEquality(value, condition);

        } catch (Exception e) {
            log.warn("Error evaluating condition '{}' against value '{}': {}", condition, value, e.getMessage());
            return false;
        }
    }

    /**
     * Evaluate OR condition (e.g., "CHECKING|SAVINGS|LOAN")
     */
    private boolean evaluateOrCondition(Object value, String condition) {
        String[] options = condition.split("\\|");
        String valueStr = value.toString();

        for (String option : options) {
            if (valueStr.equalsIgnoreCase(option.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Evaluate range condition (e.g., "> 10 && <= 100")
     */
    private boolean evaluateRangeCondition(Object value, Matcher matcher) {
        String operator1 = matcher.group(1);
        String value1 = matcher.group(2);
        String logicalOp = matcher.group(3);
        String operator2 = matcher.group(4);
        String value2 = matcher.group(5);

        boolean result1 = evaluateNumericComparison(value, operator1, value1);
        boolean result2 = evaluateNumericComparison(value, operator2, value2);

        if ("&&".equals(logicalOp)) {
            return result1 && result2;
        } else {
            return result1 || result2;
        }
    }

    /**
     * Evaluate string operation (contains, startsWith, endsWith, matches)
     */
    private boolean evaluateStringOperation(String value, Matcher matcher) {
        String operation = matcher.group(1);
        String operand = matcher.group(2);

        return switch (operation) {
            case "contains" -> value.contains(operand);
            case "startsWith" -> value.startsWith(operand);
            case "endsWith" -> value.endsWith(operand);
            case "matches" -> value.matches(operand);
            default -> false;
        };
    }

    /**
     * Evaluate comparison (>, <, >=, <=, ==, !=)
     */
    private boolean evaluateComparison(Object value, Matcher matcher) {
        String operator = matcher.group(1);
        String operand = matcher.group(2).trim();

        // Remove quotes if present
        operand = operand.replaceAll("^['\"]|['\"]$", "");

        // Numeric comparison
        if (value instanceof Number || isNumeric(value.toString())) {
            return evaluateNumericComparison(value, operator, operand);
        }

        // String comparison
        return evaluateStringComparison(value.toString(), operator, operand);
    }

    /**
     * Evaluate numeric comparison
     */
    private boolean evaluateNumericComparison(Object value, String operator, String operand) {
        try {
            double valueNum = parseNumber(value);
            double operandNum = parseNumber(operand);

            return switch (operator) {
                case ">" -> valueNum > operandNum;
                case "<" -> valueNum < operandNum;
                case ">=" -> valueNum >= operandNum;
                case "<=" -> valueNum <= operandNum;
                case "==" -> Math.abs(valueNum - operandNum) < 0.0001;
                case "!=" -> Math.abs(valueNum - operandNum) >= 0.0001;
                default -> false;
            };
        } catch (NumberFormatException e) {
            log.warn("Cannot parse numeric values: {} and {}", value, operand);
            return false;
        }
    }

    /**
     * Evaluate string comparison
     */
    private boolean evaluateStringComparison(String value, String operator, String operand) {
        return switch (operator) {
            case "==" -> value.equalsIgnoreCase(operand);
            case "!=" -> !value.equalsIgnoreCase(operand);
            default -> false;
        };
    }

    /**
     * Evaluate direct equality
     */
    private boolean evaluateEquality(Object value, String condition) {
        // Remove quotes if present
        String cleanCondition = condition.replaceAll("^['\"]|['\"]$", "");

        if (value instanceof Boolean) {
            return value.equals(Boolean.parseBoolean(cleanCondition));
        }

        return value.toString().equalsIgnoreCase(cleanCondition);
    }

    /**
     * Check if string is numeric
     */
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Parse number from object or string
     */
    private double parseNumber(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString().trim());
    }
}
