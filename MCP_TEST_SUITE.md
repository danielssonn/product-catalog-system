# MCP Validation Test Suite

**Version**: 1.0
**Date**: January 7, 2025
**Purpose**: Comprehensive test suite for validating MCP (Model Context Protocol) document validation using Claude AI

---

## Table of Contents

1. [Overview](#overview)
2. [Test Categories](#test-categories)
3. [Test Case Definitions](#test-case-definitions)
4. [Sample Documents](#sample-documents)
5. [Expected Outcomes](#expected-outcomes)
6. [Test Execution](#test-execution)
7. [Validation Criteria](#validation-criteria)

---

## Overview

### Purpose

This test suite validates the MCP validator's ability to analyze banking product documentation using Claude AI for:
- Document completeness assessment
- Regulatory compliance verification
- Risk identification
- Pricing consistency analysis
- Professional quality evaluation

### Validator Configuration

- **Validator Type**: MCP (Claude-powered)
- **Model**: claude-sonnet-4-5-20250929
- **Temperature**: 0.3
- **Max Tokens**: 4096
- **Execution Mode**: SYNC_ENRICHMENT

### Success Metrics

- **Accuracy**: Claude correctly identifies document issues
- **Completeness**: All required regulatory checks performed
- **Consistency**: Similar documents receive similar assessments
- **Enrichment**: Meaningful metadata added for DMN rule evaluation
- **Performance**: Response time < 60 seconds

---

## Test Categories

### Category 1: Complete and Compliant Documents
Test Claude's ability to recognize well-formed, compliant documents.

### Category 2: Incomplete Documents
Test detection of missing sections, incomplete disclosures, and draft content.

### Category 3: Regulatory Compliance Issues
Test identification of specific regulatory violations and missing disclosures.

### Category 4: Pricing and Fee Problems
Test detection of pricing inconsistencies, excessive fees, and competitive issues.

### Category 5: Marketing and Communication Issues
Test identification of misleading claims, false advertising, and UDAAP violations.

### Category 6: Risk Assessment
Test Claude's ability to assess product risk levels accurately.

---

## Test Case Definitions

### Test Case 1: Complete Checking Account (PASS)

**Document**: `premium-checking-terms.md`

**Description**: Comprehensive, compliant checking account terms and conditions

**Expected Results**:
- ✅ Red Flag: FALSE
- ✅ Confidence Score: 0.9 - 1.0
- ✅ Document Completeness: 95-100%
- ✅ Regulatory Compliance: COMPLIANT
- ✅ Risk Level: LOW

**Validation Checks**:
- [ ] All required regulatory disclosures present (FDIC, Reg E, Reg D, Truth in Savings)
- [ ] Fee schedule complete and clear
- [ ] Terms and conditions comprehensive
- [ ] Contact information provided
- [ ] Effective dates present
- [ ] No misleading claims

**Enrichment Data Expected**:
```json
{
  "documentCompleteness": 100,
  "regulatoryCompliance": "COMPLIANT",
  "disclosuresPresent": ["FDIC", "RegE", "RegD", "TruthInSavings", "PATRIOT"],
  "riskIndicators": [],
  "pricingIssues": [],
  "missingElements": []
}
```

---

### Test Case 2: Complete Savings Account (PASS)

**Document**: `high-yield-savings-terms.md`

**Description**: Complete high-yield savings account documentation with all regulatory disclosures

**Expected Results**:
- ✅ Red Flag: FALSE
- ✅ Confidence Score: 0.9 - 1.0
- ✅ Document Completeness: 95-100%
- ✅ Regulatory Compliance: COMPLIANT
- ✅ Risk Level: LOW

**Validation Checks**:
- [ ] Interest rate structure clearly defined
- [ ] Regulation D transaction limits disclosed
- [ ] Truth in Savings Act compliance
- [ ] FDIC insurance disclosure
- [ ] Fee schedule present and reasonable
- [ ] Account limitations clearly stated

**Enrichment Data Expected**:
```json
{
  "documentCompleteness": 100,
  "regulatoryCompliance": "COMPLIANT",
  "interestBearing": true,
  "tieredRates": true,
  "disclosuresPresent": ["FDIC", "RegD", "TruthInSavings", "RegE"],
  "riskIndicators": []
}
```

---

### Test Case 3: Complete Mortgage Disclosure (PASS)

**Document**: `mortgage-loan-disclosure.md`

**Description**: Comprehensive mortgage loan disclosure with TILA, RESPA, and all required notices

**Expected Results**:
- ✅ Red Flag: FALSE
- ✅ Confidence Score: 0.85 - 1.0
- ✅ Document Completeness: 95-100%
- ✅ Regulatory Compliance: COMPLIANT
- ✅ Risk Level: LOW-MEDIUM (mortgage products inherently higher risk)

**Validation Checks**:
- [ ] TILA disclosure present
- [ ] RESPA compliance
- [ ] Loan Estimate and Closing Disclosure mentioned
- [ ] APR disclosed
- [ ] Fee schedule comprehensive
- [ ] Foreclosure warnings present
- [ ] Counseling information provided

**Enrichment Data Expected**:
```json
{
  "documentCompleteness": 100,
  "regulatoryCompliance": "COMPLIANT",
  "productType": "MORTGAGE",
  "loanTerm": 360,
  "disclosuresPresent": ["TILA", "RESPA", "ECOA", "FCRA", "HMDA", "SAFE"],
  "riskLevel": "MEDIUM"
}
```

---

### Test Case 4: Complete Credit Card Fee Schedule (PASS)

**Document**: `credit-card-fee-schedule.md`

**Description**: Detailed credit card fee schedule with complete regulatory compliance

**Expected Results**:
- ✅ Red Flag: FALSE
- ✅ Confidence Score: 0.9 - 1.0
- ✅ Document Completeness: 95-100%
- ✅ Regulatory Compliance: COMPLIANT
- ✅ Risk Level: LOW

**Validation Checks**:
- [ ] All fee types clearly disclosed
- [ ] APR information present
- [ ] Credit CARD Act compliance
- [ ] SCRA and MLA benefits disclosed
- [ ] Fee comparison information helpful
- [ ] Penalty fees within regulatory guidelines

**Enrichment Data Expected**:
```json
{
  "documentCompleteness": 100,
  "regulatoryCompliance": "COMPLIANT",
  "productType": "CREDIT_CARD",
  "disclosuresPresent": ["TILA", "CreditCARDAct", "SCRA", "MLA"],
  "penaltyFeesCompliant": true
}
```

---

### Test Case 5: Incomplete Student Account Draft (RED FLAG)

**Document**: `incomplete-product-draft.md`

**Description**: Draft document with multiple missing sections and TBD items

**Expected Results**:
- 🚩 Red Flag: TRUE
- ⚠️ Confidence Score: 0.9 - 1.0
- ⚠️ Document Completeness: 30-50%
- ⚠️ Regulatory Compliance: NON-COMPLIANT
- ⚠️ Risk Level: HIGH

**Validation Checks**:
- [ ] Multiple "[TBD]" placeholders detected
- [ ] Missing regulatory disclosures
- [ ] Incomplete fee schedule
- [ ] No terms and conditions
- [ ] Marked as "DRAFT"
- [ ] Internal approval checklist visible

**Enrichment Data Expected**:
```json
{
  "documentCompleteness": 40,
  "regulatoryCompliance": "NON_COMPLIANT",
  "isDraft": true,
  "missingElements": [
    "Complete fee schedule",
    "Terms and conditions",
    "Regulatory disclosures",
    "FDIC disclosure",
    "Truth in Savings",
    "Electronic Funds Transfer disclosure"
  ],
  "riskIndicators": [
    "Document marked as DRAFT",
    "Multiple TBD items",
    "Pending legal review",
    "Incomplete compliance sections"
  ]
}
```

**Expected Reasoning**:
- Document is clearly a draft
- Critical sections incomplete
- Cannot be offered to customers
- Requires legal and compliance review

---

### Test Case 6: Problematic Pricing Document (RED FLAG)

**Document**: `problematic-pricing.md`

**Description**: Business checking account with multiple pricing issues and regulatory concerns

**Expected Results**:
- 🚩 Red Flag: TRUE
- ⚠️ Confidence Score: 0.95 - 1.0
- ⚠️ Document Completeness: 70-80% (document complete but problematic)
- ⚠️ Regulatory Compliance: NON-COMPLIANT
- ⚠️ Risk Level: HIGH

**Validation Checks**:
- [ ] Missing Truth in Savings disclosure detected
- [ ] Excessive overdraft fees identified (>$150/day)
- [ ] Pricing variance issues flagged (>15% over standard)
- [ ] Non-competitive pricing identified
- [ ] UDAAP risk flagged
- [ ] Regulatory compliance issues noted

**Enrichment Data Expected**:
```json
{
  "documentCompleteness": 75,
  "regulatoryCompliance": "NON_COMPLIANT",
  "pricingIssues": [
    "Missing Truth in Savings disclosure",
    "Overdraft fees exceed guidance ($195 > $150)",
    "Pricing variance 47% vs 15% threshold",
    "Non-competitive minimum balance requirement",
    "Potential UDAAP violation - coin fees"
  ],
  "riskIndicators": [
    "Regulatory risk: HIGH",
    "Reputational risk: MEDIUM-HIGH",
    "Competitive risk: HIGH",
    "Executive approval required",
    "Blocked for market launch"
  ],
  "riskLevel": "HIGH"
}
```

**Expected Reasoning**:
- Multiple regulatory violations
- Missing required disclosures
- Pricing significantly above market
- High regulatory and reputational risk

---

### Test Case 7: Non-Compliant Marketing Material (RED FLAG)

**Document**: `non-compliant-marketing.md`

**Description**: CD marketing with false claims, misleading statements, and UDAAP violations

**Expected Results**:
- 🚩 Red Flag: TRUE
- ⚠️ Confidence Score: 1.0 (clear violations)
- ⚠️ Document Completeness: N/A (marketing, not product terms)
- ⚠️ Regulatory Compliance: SEVERE_VIOLATIONS
- ⚠️ Risk Level: CRITICAL

**Validation Checks**:
- [ ] False/misleading claims detected ("ZERO RISK", "CANNOT LOSE")
- [ ] Deceptive urgency tactics identified
- [ ] Missing required disclosures
- [ ] False testimonials flagged
- [ ] Unsubstantiated comparative claims
- [ ] UDAAP violations throughout
- [ ] FTC Act violations

**Enrichment Data Expected**:
```json
{
  "documentCompleteness": 0,
  "regulatoryCompliance": "SEVERE_VIOLATIONS",
  "violationType": "MARKETING_COMPLIANCE",
  "violations": [
    "Regulation DD - Misleading rate disclosure",
    "UDAAP - False 'zero risk' claims",
    "FTC Act - Unfair comparisons",
    "FTC Endorsement - False testimonials",
    "UDAAP - Deceptive urgency tactics",
    "FDIC Rules - Misleading insurance claims",
    "Securities - Inappropriate investment advice"
  ],
  "riskIndicators": [
    "FTC enforcement action risk",
    "Regulatory sanctions likely",
    "Severe reputational damage",
    "Potential consent order",
    "Legal liability exposure"
  ],
  "riskLevel": "CRITICAL",
  "prohibited": true
}
```

**Expected Reasoning**:
- Material violates multiple federal regulations
- False and misleading claims throughout
- High-pressure sales tactics
- Deceptive advertising
- Poses significant legal and regulatory risk
- Must not be used under any circumstances

---

### Test Case 8: Missing Documents Scenario (RED FLAG)

**Document**: None provided (empty submission)

**Description**: Product configuration submitted without any supporting documentation

**Expected Results**:
- 🚩 Red Flag: TRUE
- ⚠️ Confidence Score: 1.0
- ⚠️ Document Completeness: 0%
- ⚠️ Regulatory Compliance: NON_COMPLIANT
- ⚠️ Risk Level: CRITICAL

**Validation Checks**:
- [ ] No documentation provided
- [ ] Cannot assess compliance
- [ ] Cannot evaluate pricing
- [ ] No regulatory disclosures
- [ ] Product cannot be offered

**Enrichment Data Expected**:
```json
{
  "documentCompleteness": 0,
  "regulatoryCompliance": "NON_COMPLIANT",
  "missingElements": [
    "Terms and Conditions",
    "Fee Schedule",
    "Disclosure Statement",
    "Regulatory notices",
    "Product documentation"
  ],
  "riskIndicators": [
    "No documentation provided",
    "Cannot verify regulatory compliance",
    "Product cannot be offered to consumers"
  ],
  "riskLevel": "CRITICAL"
}
```

---

### Test Case 9: Minimal but Compliant (CONDITIONAL PASS)

**Scenario**: Simple savings account with minimal but sufficient documentation

**Test Data**:
```json
{
  "solutionName": "Basic Savings Account",
  "description": "Simple savings with FDIC insurance, 0.50% APY, $25 minimum",
  "productType": "SAVINGS",
  "documents": "Basic T&C with FDIC, Reg D, Truth in Savings disclosures"
}
```

**Expected Results**:
- ⚠️ Red Flag: FALSE (but low completeness)
- ✅ Confidence Score: 0.7 - 0.8
- ⚠️ Document Completeness: 60-70%
- ✅ Regulatory Compliance: MINIMUM_COMPLIANT
- ✅ Risk Level: LOW-MEDIUM

**Expected Reasoning**:
- Meets minimum regulatory requirements
- Could benefit from more comprehensive documentation
- Suitable for simple, low-risk products
- Recommend enhanced documentation for better customer experience

---

### Test Case 10: High-Risk Product Category (ELEVATED SCRUTINY)

**Scenario**: Complex investment product requiring enhanced documentation

**Test Data**:
```json
{
  "solutionName": "Variable Annuity with Living Benefits",
  "description": "Variable annuity with guaranteed lifetime withdrawal benefit",
  "productType": "INVESTMENT",
  "pricingVariance": 25.5,
  "riskLevel": "HIGH"
}
```

**Expected Results**:
- 🚩 Red Flag: TRUE (requires enhanced review)
- ⚠️ Confidence Score: 0.9 - 1.0
- ⚠️ Regulatory Compliance: REQUIRES_ENHANCED_REVIEW
- ⚠️ Risk Level: HIGH

**Expected Reasoning**:
- Investment products require securities compliance
- Complex product needs extensive disclosures
- Higher pricing variance requires justification
- Enhanced documentation and approvals needed

---

## Sample Documents

### Document Storage

All test documents are stored in:
```
/test-data/documents/
├── premium-checking-terms.md (Complete, Compliant)
├── high-yield-savings-terms.md (Complete, Compliant)
├── mortgage-loan-disclosure.md (Complete, Compliant)
├── credit-card-fee-schedule.md (Complete, Compliant)
├── incomplete-product-draft.md (Incomplete, RED FLAG)
├── problematic-pricing.md (Compliance Issues, RED FLAG)
└── non-compliant-marketing.md (Severe Violations, RED FLAG)
```

### Document Characteristics

| Document | Size | Completeness | Compliance | Expected Result |
|----------|------|--------------|------------|-----------------|
| Premium Checking | 8KB | 100% | ✅ Compliant | PASS |
| High-Yield Savings | 9KB | 100% | ✅ Compliant | PASS |
| Mortgage Disclosure | 12KB | 100% | ✅ Compliant | PASS |
| Credit Card Fees | 11KB | 100% | ✅ Compliant | PASS |
| Student Draft | 2KB | 40% | ❌ Non-Compliant | RED FLAG |
| Problematic Pricing | 5KB | 75% | ❌ Non-Compliant | RED FLAG |
| Marketing Violations | 6KB | N/A | ❌ Severe | RED FLAG |

---

## Expected Outcomes

### Outcome Matrix

| Test Case | Red Flag | Confidence | Completeness | Compliance | Risk Level |
|-----------|----------|------------|--------------|------------|------------|
| TC1: Premium Checking | ❌ | 0.9-1.0 | 95-100% | ✅ | LOW |
| TC2: High-Yield Savings | ❌ | 0.9-1.0 | 95-100% | ✅ | LOW |
| TC3: Mortgage | ❌ | 0.85-1.0 | 95-100% | ✅ | MEDIUM |
| TC4: Credit Card | ❌ | 0.9-1.0 | 95-100% | ✅ | LOW |
| TC5: Incomplete Draft | ✅ | 0.9-1.0 | 30-50% | ❌ | HIGH |
| TC6: Pricing Issues | ✅ | 0.95-1.0 | 70-80% | ❌ | HIGH |
| TC7: Marketing | ✅ | 1.0 | 0% | ❌❌ | CRITICAL |
| TC8: No Docs | ✅ | 1.0 | 0% | ❌ | CRITICAL |
| TC9: Minimal | ❌ | 0.7-0.8 | 60-70% | ⚠️ | LOW-MED |
| TC10: High Risk | ✅ | 0.9-1.0 | TBD | ⚠️ | HIGH |

### Enrichment Data Quality

Each test should validate that Claude provides:

1. **Quantitative Metrics**:
   - Document completeness score (0-100)
   - Confidence score (0.0-1.0)
   - Number of missing elements
   - Number of risk indicators

2. **Qualitative Assessments**:
   - Regulatory compliance status
   - Risk level categorization
   - Specific violations identified
   - Recommended actions

3. **Structured Data**:
   - Lists of present/missing disclosures
   - Array of risk indicators
   - Array of pricing issues
   - Category classifications

4. **Contextual Reasoning**:
   - Why red flag was raised
   - Specific regulatory citations
   - Business impact assessment
   - Remediation recommendations

---

## Test Execution

### Manual Execution

Each test case can be executed via:

```bash
./test-mcp-validation.sh <test-case-number> <document-name>
```

Example:
```bash
./test-mcp-validation.sh 1 premium-checking-terms.md
```

### Automated Test Suite

Full test suite execution:

```bash
./run-mcp-test-suite.sh
```

This will:
1. Execute all 10 test cases
2. Capture Claude responses
3. Validate enrichment data
4. Generate test report
5. Compare results against expected outcomes

### Test Workflow

```
┌─────────────────────────────────────────────────┐
│ 1. Submit Workflow with Test Data              │
├─────────────────────────────────────────────────┤
│ 2. MCP Validator Calls Claude                  │
├─────────────────────────────────────────────────┤
│ 3. Claude Analyzes Documents                   │
├─────────────────────────────────────────────────┤
│ 4. Parse Response & Extract Enrichment Data    │
├─────────────────────────────────────────────────┤
│ 5. Validate Against Expected Results           │
├─────────────────────────────────────────────────┤
│ 6. Document Results in Test Report             │
└─────────────────────────────────────────────────┘
```

---

## Validation Criteria

### Pass Criteria

A test case passes if:

1. **Red Flag Accuracy**: Claude correctly identifies whether a red flag should be raised
2. **Confidence Appropriateness**: Confidence score reflects the certainty of the assessment
3. **Completeness Assessment**: Document completeness score is within ±10% of expected
4. **Compliance Detection**: Regulatory issues correctly identified
5. **Enrichment Quality**: Metadata provides actionable insights for DMN rules
6. **Performance**: Response time < 60 seconds

### Fail Criteria

A test case fails if:

1. **False Positive**: Red flag raised on compliant document
2. **False Negative**: Red flag NOT raised on non-compliant document
3. **Poor Reasoning**: Explanation doesn't justify the decision
4. **Missing Enrichment**: Required metadata fields not populated
5. **Timeout**: Claude doesn't respond within timeout period
6. **Error**: API error or parsing failure

### Metrics to Track

- **Accuracy Rate**: % of correct red flag determinations
- **Precision**: True Positives / (True Positives + False Positives)
- **Recall**: True Positives / (True Positives + False Negatives)
- **F1 Score**: Harmonic mean of precision and recall
- **Average Confidence**: Mean confidence score across all tests
- **Average Response Time**: Mean Claude API response time
- **Enrichment Completeness**: % of expected metadata fields populated

### Success Thresholds

- **Accuracy**: ≥ 90%
- **Precision**: ≥ 95%
- **Recall**: ≥ 90%
- **F1 Score**: ≥ 0.92
- **Avg Response Time**: ≤ 45 seconds
- **Enrichment Completeness**: ≥ 85%

---

## Test Report Template

### Test Execution Summary

```
MCP Validation Test Suite Results
Execution Date: [DATE]
Model: claude-sonnet-4-5-20250929
Total Test Cases: 10
Duration: [TIME]

RESULTS SUMMARY:
- Passed: X / 10
- Failed: Y / 10
- Accuracy: Z%
- Precision: P%
- Recall: R%
- F1 Score: F

PERFORMANCE:
- Avg Response Time: X seconds
- Min Response Time: Y seconds
- Max Response Time: Z seconds
- Timeout Errors: N

ENRICHMENT QUALITY:
- Avg Completeness Score: X%
- Avg Confidence Score: Y
- Fields Populated: Z%
```

### Individual Test Results

```
TEST CASE 1: Premium Checking Terms
Status: PASS ✅
Red Flag: FALSE (Expected: FALSE) ✅
Confidence: 0.95 (Expected: 0.9-1.0) ✅
Completeness: 98% (Expected: 95-100%) ✅
Compliance: COMPLIANT (Expected: COMPLIANT) ✅
Response Time: 28.3s

Claude Reasoning:
"The Premium Checking Account documentation is comprehensive and compliant..."

Enrichment Data:
{
  "documentCompleteness": 98,
  "regulatoryCompliance": "COMPLIANT",
  ...
}
```

---

## Conclusion

This test suite provides comprehensive validation of the MCP validator's ability to:
- Assess document quality and completeness
- Identify regulatory compliance issues
- Detect pricing and fee problems
- Recognize misleading marketing claims
- Provide actionable enrichment data for workflow decisions

Execute this test suite regularly to:
- Validate MCP validator accuracy
- Ensure consistent Claude performance
- Document test coverage
- Support regulatory compliance
- Build confidence in AI-powered validation

---

**Next Steps**:
1. Execute baseline test suite
2. Document results
3. Establish performance benchmarks
4. Schedule regular regression testing
5. Expand test cases as new scenarios emerge

---

**Maintained By**: Workflow Service Team
**Last Updated**: January 7, 2025
**Version**: 1.0
