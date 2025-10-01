package com.bank.product.workflow.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConditionEvaluator
 */
class ConditionEvaluatorTest {

    private ConditionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ConditionEvaluator();
    }

    @Test
    void testNumericGreaterThan() {
        assertTrue(evaluator.evaluate(100, "> 50"));
        assertFalse(evaluator.evaluate(30, "> 50"));
        assertTrue(evaluator.evaluate(50.5, "> 50"));
    }

    @Test
    void testNumericLessThan() {
        assertTrue(evaluator.evaluate(30, "< 50"));
        assertFalse(evaluator.evaluate(70, "< 50"));
        assertTrue(evaluator.evaluate(49.9, "< 50"));
    }

    @Test
    void testNumericGreaterThanOrEqual() {
        assertTrue(evaluator.evaluate(50, ">= 50"));
        assertTrue(evaluator.evaluate(51, ">= 50"));
        assertFalse(evaluator.evaluate(49, ">= 50"));
    }

    @Test
    void testNumericLessThanOrEqual() {
        assertTrue(evaluator.evaluate(50, "<= 50"));
        assertTrue(evaluator.evaluate(49, "<= 50"));
        assertFalse(evaluator.evaluate(51, "<= 50"));
    }

    @Test
    void testNumericEquals() {
        assertTrue(evaluator.evaluate(50, "== 50"));
        assertFalse(evaluator.evaluate(51, "== 50"));
        assertTrue(evaluator.evaluate(50.0, "== 50"));
    }

    @Test
    void testNumericNotEquals() {
        assertTrue(evaluator.evaluate(51, "!= 50"));
        assertFalse(evaluator.evaluate(50, "!= 50"));
    }

    @Test
    void testRangeCondition() {
        assertTrue(evaluator.evaluate(50, "> 10 && <= 100"));
        assertTrue(evaluator.evaluate(11, "> 10 && <= 100"));
        assertTrue(evaluator.evaluate(100, "> 10 && <= 100"));
        assertFalse(evaluator.evaluate(10, "> 10 && <= 100"));
        assertFalse(evaluator.evaluate(101, "> 10 && <= 100"));
    }

    @Test
    void testStringEquality() {
        assertTrue(evaluator.evaluate("CHECKING", "== 'CHECKING'"));
        assertTrue(evaluator.evaluate("CHECKING", "CHECKING"));
        assertTrue(evaluator.evaluate("checking", "CHECKING")); // Case insensitive
        assertFalse(evaluator.evaluate("SAVINGS", "CHECKING"));
    }

    @Test
    void testStringNotEquals() {
        assertTrue(evaluator.evaluate("SAVINGS", "!= 'CHECKING'"));
        assertFalse(evaluator.evaluate("CHECKING", "!= 'CHECKING'"));
    }

    @Test
    void testOrCondition() {
        assertTrue(evaluator.evaluate("CHECKING", "CHECKING|SAVINGS|LOAN"));
        assertTrue(evaluator.evaluate("SAVINGS", "CHECKING|SAVINGS|LOAN"));
        assertTrue(evaluator.evaluate("LOAN", "CHECKING|SAVINGS|LOAN"));
        assertFalse(evaluator.evaluate("CREDIT_CARD", "CHECKING|SAVINGS|LOAN"));
    }

    @Test
    void testStringContains() {
        assertTrue(evaluator.evaluate("Premium Checking", "contains 'Premium'"));
        assertTrue(evaluator.evaluate("High-Yield Savings", "contains 'Yield'"));
        assertFalse(evaluator.evaluate("Basic Checking", "contains 'Premium'"));
    }

    @Test
    void testStringStartsWith() {
        assertTrue(evaluator.evaluate("Premium Checking", "startsWith 'Premium'"));
        assertFalse(evaluator.evaluate("Basic Checking", "startsWith 'Premium'"));
    }

    @Test
    void testStringEndsWith() {
        assertTrue(evaluator.evaluate("Premium Checking", "endsWith 'Checking'"));
        assertFalse(evaluator.evaluate("Premium Savings", "endsWith 'Checking'"));
    }

    @Test
    void testBooleanValue() {
        assertTrue(evaluator.evaluate(true, "true"));
        assertFalse(evaluator.evaluate(false, "true"));
        assertTrue(evaluator.evaluate(false, "false"));
    }

    @Test
    void testNullValues() {
        assertFalse(evaluator.evaluate(null, "> 50"));
        assertFalse(evaluator.evaluate(50, null));
        assertFalse(evaluator.evaluate(null, null));
    }

    @Test
    void testInvalidConditions() {
        // Should not throw exceptions, just return false
        assertFalse(evaluator.evaluate("not a number", "> 50"));
        assertFalse(evaluator.evaluate(50, "invalid operator"));
    }
}
