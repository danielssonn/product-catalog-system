package com.bank.product.party.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured data extracted from IRS Form W-9 (US Tax Form)
 *
 * Form W-9 is used to request the taxpayer identification number (TIN) of a U.S. person
 * (including a resident alien) to report income paid to them.
 *
 * Key identifiers:
 * - TaxID (EIN or SSN)
 * - Legal Name
 * - Business Name (if different)
 * - Address
 *
 * Confidence: 1.00 (highest) - IRS-issued document
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class W9Data {

    /**
     * Legal name (Line 1 of Form W-9)
     * For individuals: Full name as shown on tax return
     * For entities: Legal business name
     */
    private String legalName;

    /**
     * Business name / DBA (Line 2 of Form W-9)
     * Only filled if different from legal name
     */
    private String businessName;

    /**
     * Federal tax classification
     * Options: Individual, C Corporation, S Corporation, Partnership, Trust/Estate, LLC, Other
     */
    private String taxClassification;

    /**
     * If LLC, tax classification (C=C Corporation, S=S Corporation, P=Partnership)
     */
    private String llcTaxClassification;

    /**
     * Taxpayer Identification Number (EIN or SSN)
     * Format: XX-XXXXXXX (EIN) or XXX-XX-XXXX (SSN)
     */
    private String taxId;

    /**
     * Street address (Line 5)
     */
    private String streetAddress;

    /**
     * City (Line 6)
     */
    private String city;

    /**
     * State (Line 6)
     */
    private String state;

    /**
     * ZIP code (Line 6)
     */
    private String zipCode;

    /**
     * Country (default: USA)
     */
    private String country;

    /**
     * Signature date
     */
    private String signatureDate;

    /**
     * Whether the entity is subject to backup withholding
     */
    private Boolean backupWithholding;

    /**
     * Confidence score of extraction (0.0-1.0)
     */
    private Double confidence;

    /**
     * Get full address as single string
     */
    public String getFullAddress() {
        return String.format("%s, %s, %s %s, %s",
                streetAddress, city, state, zipCode, country);
    }

    /**
     * Determine entity type from tax classification
     */
    public String inferEntityType() {
        if (taxClassification == null) {
            return "UNKNOWN";
        }

        return switch (taxClassification.toUpperCase()) {
            case "INDIVIDUAL", "SOLE PROPRIETOR" -> "INDIVIDUAL";
            case "C CORPORATION", "S CORPORATION" -> "CORPORATION";
            case "PARTNERSHIP" -> "PARTNERSHIP";
            case "LLC" -> "LIMITED_LIABILITY_COMPANY";
            case "TRUST/ESTATE", "TRUST", "ESTATE" -> "TRUST";
            default -> "ORGANIZATION";
        };
    }
}
