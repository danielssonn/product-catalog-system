package com.bank.product.party.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Structured data extracted from Certificate of Incorporation
 *
 * Certificate of Incorporation is a legal document establishing a corporation,
 * filed with state/country authorities.
 *
 * Key identifiers:
 * - Registration Number
 * - Legal Name
 * - Jurisdiction
 * - Incorporation Date
 *
 * Confidence: 0.98 (very high) - Government-issued document
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncorporationData {

    /**
     * Legal registered name of the corporation
     */
    private String legalName;

    /**
     * Registration/Company number issued by the jurisdiction
     */
    private String registrationNumber;

    /**
     * Jurisdiction of incorporation (state for US, country for international)
     * Examples: "Delaware", "California", "United Kingdom", "Cayman Islands"
     */
    private String jurisdiction;

    /**
     * Date of incorporation
     */
    private LocalDate incorporationDate;

    /**
     * Registered office address
     */
    private String registeredOfficeAddress;

    /**
     * Registered agent name
     */
    private String registeredAgentName;

    /**
     * Registered agent address
     */
    private String registeredAgentAddress;

    /**
     * Type of corporation
     * Examples: "C Corporation", "S Corporation", "LLC", "PLC", "Limited Company"
     */
    private String corporationType;

    /**
     * Authorized shares (if available)
     */
    private Long authorizedShares;

    /**
     * Par value of shares (if available)
     */
    private Double parValue;

    /**
     * Initial directors/officers (if listed)
     */
    private List<String> initialDirectors;

    /**
     * Incorporators (if listed)
     */
    private List<String> incorporators;

    /**
     * Business purpose / activities
     */
    private String businessPurpose;

    /**
     * Parent company mention (extracted from legal name patterns)
     * Example: "ABC Corporation, a subsidiary of XYZ Holdings"
     */
    private String parentCompanyMention;

    /**
     * Parent company jurisdiction (if mentioned)
     */
    private String parentCompanyJurisdiction;

    /**
     * Confidence score of extraction (0.0-1.0)
     */
    private Double confidence;

    /**
     * Check if this is likely a subsidiary based on name patterns
     */
    public boolean hasSubsidiaryIndicators() {
        if (legalName == null) {
            return false;
        }

        String lowerName = legalName.toLowerCase();
        return lowerName.contains("subsidiary of") ||
               lowerName.contains("affiliate of") ||
               lowerName.contains("division of") ||
               lowerName.contains("wholly owned") ||
               parentCompanyMention != null;
    }

    /**
     * Check if registered agent is a corporate service provider (vs the company itself)
     */
    public boolean hasThirdPartyRegisteredAgent() {
        if (registeredAgentName == null) {
            return false;
        }

        String lowerAgent = registeredAgentName.toLowerCase();
        return lowerAgent.contains("corporation service") ||
               lowerAgent.contains("ct corporation") ||
               lowerAgent.contains("registered agent") ||
               lowerAgent.contains("corporate services");
    }
}
