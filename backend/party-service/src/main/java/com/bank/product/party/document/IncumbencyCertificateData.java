package com.bank.product.party.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Structured data extracted from Incumbency Certificate
 *
 * Incumbency Certificate (also called Certificate of Incumbency or Certificate of Officers)
 * is a document that lists the current officers, directors, and authorized signers of a company.
 *
 * Key relational data:
 * - Officers (CEO, CFO, COO, etc.)
 * - Directors / Board Members
 * - Authorized Signers
 * - Beneficial Owners (if listed)
 *
 * Confidence: 0.95 (very high) - Company-issued official document
 *
 * This document is critical for predictive graph construction as it establishes
 * OFFICER_OF, DIRECTOR_OF, and AUTHORIZED_SIGNER relationships.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncumbencyCertificateData {

    /**
     * Company legal name
     */
    private String companyLegalName;

    /**
     * Company registration number (if stated)
     */
    private String companyRegistrationNumber;

    /**
     * Jurisdiction (if stated)
     */
    private String jurisdiction;

    /**
     * Certificate issue date
     */
    private LocalDate issueDate;

    /**
     * Certificate expiration date (if applicable)
     */
    private LocalDate expirationDate;

    /**
     * Officers with their titles
     * Example: [{"name": "John Smith", "title": "Chief Executive Officer"}]
     */
    private List<OfficerInfo> officers;

    /**
     * Directors / Board Members
     * Example: [{"name": "Jane Doe", "title": "Director"}]
     */
    private List<DirectorInfo> directors;

    /**
     * Authorized signers (individuals who can sign on behalf of the company)
     */
    private List<AuthorizedSignerInfo> authorizedSigners;

    /**
     * Beneficial owners (if disclosed)
     * Typically individuals with 25%+ ownership
     */
    private List<BeneficialOwnerInfo> beneficialOwners;

    /**
     * Secretary who issued the certificate
     */
    private String issuedBy;

    /**
     * Signature present
     */
    private Boolean signed;

    /**
     * Notarized
     */
    private Boolean notarized;

    /**
     * Confidence score of extraction (0.0-1.0)
     */
    private Double confidence;

    /**
     * Officer information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfficerInfo {
        private String name;
        private String title; // CEO, CFO, COO, President, Vice President, Secretary, Treasurer
        private LocalDate appointmentDate;
        private String email;
        private String phoneNumber;
    }

    /**
     * Director information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectorInfo {
        private String name;
        private String title; // Director, Chairperson, Vice Chairperson, Lead Independent Director
        private LocalDate appointmentDate;
        private Boolean independent; // Independent vs Non-Independent Director
    }

    /**
     * Authorized signer information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorizedSignerInfo {
        private String name;
        private String title;
        private String authorityLevel; // Full authority, Limited authority, Requires co-signer
        private Double amountLimit; // Maximum transaction amount (if limited)
        private String authorityScope; // "Banking", "Contracts", "All transactions"
    }

    /**
     * Beneficial owner information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BeneficialOwnerInfo {
        private String name;
        private Double ownershipPercentage;
        private String ownershipType; // Direct, Indirect, Voting Rights, Economic Interest
        private String nationality;
        private String residenceCountry;
    }

    /**
     * Get all unique individuals mentioned in the certificate
     */
    public int getTotalIndividualsCount() {
        int count = 0;
        if (officers != null) count += officers.size();
        if (directors != null) count += directors.size();
        if (authorizedSigners != null) count += authorizedSigners.size();
        if (beneficialOwners != null) count += beneficialOwners.size();
        return count;
    }

    /**
     * Check if this certificate is still valid
     */
    public boolean isValid() {
        if (expirationDate == null) {
            // If no expiration, assume valid for 1 year from issue
            return issueDate != null && issueDate.plusYears(1).isAfter(LocalDate.now());
        }
        return expirationDate.isAfter(LocalDate.now());
    }
}
