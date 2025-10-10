package com.bank.product.party.service;

import com.bank.product.party.domain.Organization;
import com.bank.product.party.domain.Party;
import com.bank.product.party.domain.SourceRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for resolving conflicts when merging party data from multiple sources.
 * Implements various conflict resolution strategies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConflictResolutionService {

    // Field-level quality scores per source system
    private static final Map<String, Map<String, Double>> SOURCE_QUALITY_SCORES = new HashMap<>();

    static {
        // Commercial Banking scores
        Map<String, Double> commercialBanking = new HashMap<>();
        commercialBanking.put("legalName", 0.95);
        commercialBanking.put("registeredAddress", 0.98);
        commercialBanking.put("industry", 0.70);
        commercialBanking.put("riskRating", 0.85);
        SOURCE_QUALITY_SCORES.put("COMMERCIAL_BANKING", commercialBanking);

        // Capital Markets scores
        Map<String, Double> capitalMarkets = new HashMap<>();
        capitalMarkets.put("legalName", 0.90);
        capitalMarkets.put("riskRating", 0.95);
        capitalMarkets.put("lei", 0.99);
        capitalMarkets.put("industry", 0.80);
        SOURCE_QUALITY_SCORES.put("CAPITAL_MARKETS", capitalMarkets);

        // KYC System scores (master for legal/compliance data)
        Map<String, Double> kycSystem = new HashMap<>();
        kycSystem.put("legalName", 0.99);
        kycSystem.put("lei", 1.0);
        kycSystem.put("registrationNumber", 0.99);
        kycSystem.put("jurisdiction", 0.99);
        kycSystem.put("taxId", 0.99);
        SOURCE_QUALITY_SCORES.put("KYC_SYSTEM", kycSystem);
    }

    /**
     * Merge updates from a new source into existing party
     */
    public Party mergeUpdates(Party existing, Party updates, SourceRecord newSource) {
        log.info("Merging updates for party: {}", existing.getFederatedId());

        if (existing instanceof Organization && updates instanceof Organization) {
            return mergeOrganization((Organization) existing, (Organization) updates, newSource);
        }

        // Add other party types as needed
        return existing;
    }

    /**
     * Merge organization data with conflict resolution
     */
    private Organization mergeOrganization(Organization existing, Organization updates, SourceRecord newSource) {
        String sourceSystem = newSource.getSourceSystem();

        // Legal name - use highest quality source
        existing.setLegalName(resolveField(
                "legalName",
                existing.getLegalName(),
                updates.getLegalName(),
                existing.getMasterSource() != null ? existing.getMasterSource().getSourceSystem() : null,
                sourceSystem
        ));

        // LEI - KYC system is master
        if (updates.getLei() != null) {
            if ("KYC_SYSTEM".equals(sourceSystem) || existing.getLei() == null) {
                existing.setLei(updates.getLei());
            }
        }

        // Registration number - use highest quality
        existing.setRegistrationNumber(resolveField(
                "registrationNumber",
                existing.getRegistrationNumber(),
                updates.getRegistrationNumber(),
                existing.getMasterSource() != null ? existing.getMasterSource().getSourceSystem() : null,
                sourceSystem
        ));

        // Jurisdiction - use highest quality
        existing.setJurisdiction(resolveField(
                "jurisdiction",
                existing.getJurisdiction(),
                updates.getJurisdiction(),
                existing.getMasterSource() != null ? existing.getMasterSource().getSourceSystem() : null,
                sourceSystem
        ));

        // Risk rating - Capital Markets is authoritative
        if ("CAPITAL_MARKETS".equals(sourceSystem) && updates.getRiskRating() != null) {
            existing.setRiskRating(updates.getRiskRating());
        } else if (existing.getRiskRating() == null && updates.getRiskRating() != null) {
            existing.setRiskRating(updates.getRiskRating());
        }

        // Industry - use most specific available
        if (updates.getIndustryCode() != null && updates.getIndustryCode().length() > 4) {
            // Prefer more detailed NAICS codes (6-digit vs 4-digit)
            if (existing.getIndustryCode() == null ||
                    existing.getIndustryCode().length() < updates.getIndustryCode().length()) {
                existing.setIndustryCode(updates.getIndustryCode());
                existing.setIndustry(updates.getIndustry());
            }
        }

        // AML status - use most recent from high-quality source
        if (updates.getAmlStatus() != null && getFieldQuality(sourceSystem, "amlStatus") >= 0.85) {
            existing.setAmlStatus(updates.getAmlStatus());
        }

        // Website, phone, email - use most recent non-null
        if (updates.getWebsite() != null) existing.setWebsite(updates.getWebsite());
        if (updates.getPhoneNumber() != null) existing.setPhoneNumber(updates.getPhoneNumber());
        if (updates.getEmail() != null) existing.setEmail(updates.getEmail());

        // Employee count and revenue - use most recent
        if (updates.getEmployeeCount() != null) existing.setEmployeeCount(updates.getEmployeeCount());
        if (updates.getAnnualRevenue() != null) existing.setAnnualRevenue(updates.getAnnualRevenue());

        return existing;
    }

    /**
     * Resolve field value based on quality scores
     */
    private <T> T resolveField(String fieldName, T existingValue, T newValue,
                               String existingSource, String newSource) {
        // If existing is null, use new value
        if (existingValue == null) {
            return newValue;
        }

        // If new is null, keep existing
        if (newValue == null) {
            return existingValue;
        }

        // If values are the same, no conflict
        if (existingValue.equals(newValue)) {
            return existingValue;
        }

        // Compare quality scores
        double existingQuality = getFieldQuality(existingSource, fieldName);
        double newQuality = getFieldQuality(newSource, fieldName);

        log.debug("Field conflict for {}: existing={} (quality={}), new={} (quality={})",
                fieldName, existingValue, existingQuality, newValue, newQuality);

        // Use higher quality source
        if (newQuality > existingQuality) {
            log.info("Resolving {} conflict: choosing new value from {} (quality: {} > {})",
                    fieldName, newSource, newQuality, existingQuality);
            return newValue;
        }

        // Keep existing value
        return existingValue;
    }

    /**
     * Get field quality score for a source system
     */
    private double getFieldQuality(String sourceSystem, String fieldName) {
        if (sourceSystem == null) return 0.5;

        Map<String, Double> scores = SOURCE_QUALITY_SCORES.get(sourceSystem);
        if (scores == null) return 0.8; // Default quality

        return scores.getOrDefault(fieldName, 0.8);
    }

    /**
     * Get master source for a field (for documentation/audit purposes)
     */
    public String getMasterSourceForField(String fieldName) {
        return switch (fieldName) {
            case "lei", "legalName", "registrationNumber", "jurisdiction", "taxId" -> "KYC_SYSTEM";
            case "riskRating" -> "CAPITAL_MARKETS";
            case "registeredAddress" -> "COMMERCIAL_BANKING";
            default -> "HIGHEST_QUALITY";
        };
    }
}
