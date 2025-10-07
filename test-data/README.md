# MCP Test Data

This directory contains sample documents and test data for validating the MCP (Model Context Protocol) validator powered by Claude AI.

## Directory Structure

```
test-data/
├── documents/                      # Sample product documentation
│   ├── premium-checking-terms.md   # Complete, compliant checking account
│   ├── high-yield-savings-terms.md # Complete, compliant savings account
│   ├── mortgage-loan-disclosure.md # Complete, compliant mortgage disclosure
│   ├── credit-card-fee-schedule.md # Complete, compliant credit card fees
│   ├── incomplete-product-draft.md # Incomplete draft (RED FLAG)
│   ├── problematic-pricing.md      # Pricing/compliance issues (RED FLAG)
│   └── non-compliant-marketing.md  # Severe violations (RED FLAG)
└── README.md                       # This file
```

## Document Categories

### ✅ Complete and Compliant Documents

These documents represent high-quality, regulation-compliant product documentation:

1. **premium-checking-terms.md**
   - Complete checking account terms
   - All regulatory disclosures present (FDIC, Reg E, Reg D, Truth in Savings)
   - Comprehensive fee schedule
   - Clear terms and conditions
   - **Expected MCP Result**: PASS (no red flag)

2. **high-yield-savings-terms.md**
   - Complete savings account documentation
   - Tiered interest rate structure (3.50% - 4.50% APY)
   - Regulation D compliance
   - Truth in Savings Act disclosures
   - **Expected MCP Result**: PASS (no red flag)

3. **mortgage-loan-disclosure.md**
   - Comprehensive 30-year mortgage disclosure
   - TILA compliance
   - RESPA requirements
   - Complete fee schedule with examples
   - Foreclosure warnings and counseling information
   - **Expected MCP Result**: PASS (no red flag)

4. **credit-card-fee-schedule.md**
   - Detailed credit card fee schedule
   - Credit CARD Act compliance
   - Military benefits (SCRA/MLA)
   - Fee comparison information
   - **Expected MCP Result**: PASS (no red flag)

### 🚩 Documents with Issues (RED FLAGS)

These documents contain various compliance, completeness, or quality issues:

5. **incomplete-product-draft.md**
   - Draft document with [TBD] placeholders
   - Missing regulatory disclosures
   - Incomplete fee schedule
   - Pending legal/compliance review
   - **Expected MCP Result**: RED FLAG
   - **Issues**: Incompleteness, missing disclosures

6. **problematic-pricing.md**
   - Business checking with pricing problems
   - Missing Truth in Savings disclosure
   - Excessive overdraft fees ($195 vs $150 guidance)
   - Pricing variance 47% over standard (threshold: 15%)
   - Potential UDAAP violations
   - **Expected MCP Result**: RED FLAG
   - **Issues**: Regulatory violations, pricing problems

7. **non-compliant-marketing.md**
   - CD marketing with severe compliance violations
   - False claims ("ZERO RISK", "CANNOT LOSE")
   - Deceptive urgency tactics
   - False testimonials
   - Multiple FTC and UDAAP violations
   - **Expected MCP Result**: RED FLAG (CRITICAL)
   - **Issues**: Severe regulatory violations, prohibited content

## Using Test Documents

### Individual Test

Test a specific document scenario:

```bash
./test-mcp-validation.sh 1    # Premium checking (PASS)
./test-mcp-validation.sh 5    # Incomplete draft (RED FLAG)
./test-mcp-validation.sh 7    # Marketing violations (CRITICAL)
```

### Full Test Suite

Run all test cases:

```bash
./run-mcp-test-suite.sh
```

### Verbose Mode

Get detailed output:

```bash
./test-mcp-validation.sh 6 --verbose
```

### Save Results

Save test results to file:

```bash
./test-mcp-validation.sh 4 --save
```

## Document Descriptions in Tests

When testing, document content is provided to Claude via the `description` field. The actual markdown files are for reference and documentation purposes. In production, documents would be:

1. **Attached as files** to the workflow submission
2. **Stored in document management system** and referenced by ID
3. **Embedded as content** in the request payload

For these tests, we simulate document content through detailed descriptions that capture:
- Key sections and content
- Regulatory disclosures present/missing
- Known issues or problems
- Completeness status

## Expected MCP Behavior

### For Compliant Documents
- ✅ Red Flag: FALSE
- ✅ Confidence: 0.85 - 1.0
- ✅ Completeness: 95-100%
- ✅ Compliance: COMPLIANT
- ✅ Risk Level: LOW or MEDIUM

### For Incomplete Documents
- 🚩 Red Flag: TRUE
- ⚠️ Confidence: 0.9 - 1.0
- ⚠️ Completeness: 30-50%
- ⚠️ Compliance: NON_COMPLIANT
- ⚠️ Risk Level: HIGH

### For Documents with Violations
- 🚩 Red Flag: TRUE
- ⚠️ Confidence: 0.95 - 1.0
- ⚠️ Completeness: Variable
- ⚠️ Compliance: NON_COMPLIANT or SEVERE_VIOLATIONS
- ⚠️ Risk Level: HIGH or CRITICAL

## Enrichment Data

Claude provides structured enrichment data including:

- **documentCompleteness**: 0-100 score
- **regulatoryCompliance**: COMPLIANT, NON_COMPLIANT, SEVERE_VIOLATIONS
- **disclosuresPresent**: Array of regulatory disclosures found
- **missingElements**: Array of required items not found
- **riskIndicators**: Array of identified risks
- **pricingIssues**: Array of pricing problems
- **violations**: Array of regulatory violations (if any)

This data flows into DMN rule evaluation to determine approval requirements.

## Adding New Test Documents

To add a new test document:

1. **Create the document** in `test-data/documents/`
2. **Add test case** to `test-mcp-validation.sh` TEST_CASES array
3. **Define expectations** in `MCP_TEST_SUITE.md`
4. **Update this README** with document description
5. **Run test** to validate behavior

### Document Template

```markdown
# [Product Name] - [Document Type]

**Product Code**: [CODE]
**Effective Date**: [DATE]
**Version**: [VERSION]

## [Required Sections]

[Content with regulatory disclosures, fees, terms, etc.]

## [Regulatory Disclosures Section]

[FDIC, Reg E, Truth in Savings, etc.]

---

**Member FDIC | Equal Housing Lender**
```

## Test Results

Test results are saved in:
```
test-results/mcp-validation/
├── test-case-1_[timestamp].json
├── test-case-2_[timestamp].json
├── ...
└── test-suite-report_[timestamp].md
```

## Quality Standards

Documents should demonstrate:

### ✅ Complete Documents
- All required regulatory disclosures
- Comprehensive fee schedules
- Clear terms and conditions
- Contact information
- Effective dates
- Proper formatting

### 🚩 Problematic Documents
- Missing required disclosures
- Incomplete sections
- Regulatory violations
- Pricing issues
- False/misleading claims
- Draft or placeholder content

## Regulatory References

Key regulations covered in test documents:

- **FDIC Insurance**: Federal Deposit Insurance Corporation
- **Regulation E**: Electronic Fund Transfers
- **Regulation D**: Reserve Requirements
- **Regulation DD**: Truth in Savings Act
- **TILA**: Truth in Lending Act
- **RESPA**: Real Estate Settlement Procedures Act
- **Credit CARD Act**: Credit Card Accountability Responsibility and Disclosure
- **UDAAP**: Unfair, Deceptive, or Abusive Acts or Practices
- **SCRA**: Servicemembers Civil Relief Act
- **MLA**: Military Lending Act
- **ECOA**: Equal Credit Opportunity Act
- **FCRA**: Fair Credit Reporting Act

## Support

For questions about test documents or MCP validation:

- See **MCP_TEST_SUITE.md** for complete test specifications
- Review **MCP_INTEGRATION_GUIDE.md** for implementation details
- Check workflow service logs for Claude's reasoning
- Contact the Workflow Service team

---

**Last Updated**: January 7, 2025
**Maintained By**: Workflow Service Team
