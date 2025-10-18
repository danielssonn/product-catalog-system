package com.bank.product.party.resolution;

import com.bank.product.party.domain.Party;
import com.bank.product.party.domain.SourceRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data quality service for scoring and resolving conflicts in entity resolution.
 *
 * Implements three quality dimensions:
 * 1. Completeness - How many required fields are populated
 * 2. Freshness - How recent the data is (exponential decay)
 * 3. Source Authority - Trust level of the data source
 *
 * Based on ENTITY_RESOLUTION_DESIGN.md Section 2: Data Quality Scoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataQualityService {

    // Freshness decay parameters
    private static final int FRESHNESS_HALF_LIFE_DAYS = 180; // 6 months
    private static final double FRESHNESS_DECAY_LAMBDA = Math.log(2) / FRESHNESS_HALF_LIFE_DAYS;

    // Source authority weights
    private static final Map<String, Double> SOURCE_AUTHORITY = new HashMap<>();

    static {
        // Regulatory/Government sources (highest authority)
        SOURCE_AUTHORITY.put("LEI_DATABASE", 1.0);
        SOURCE_AUTHORITY.put("SEC_EDGAR", 0.95);
        SOURCE_AUTHORITY.put("IRS", 0.95);
        SOURCE_AUTHORITY.put("STATE_REGISTRY", 0.90);

        // Direct customer input (high authority)
        SOURCE_AUTHORITY.put("CUSTOMER_ONBOARDING", 0.85);
        SOURCE_AUTHORITY.put("KYC_VERIFICATION", 0.85);

        // Internal systems (medium authority)
        SOURCE_AUTHORITY.put("CRM", 0.75);
        SOURCE_AUTHORITY.put("CORE_BANKING", 0.75);
        SOURCE_AUTHORITY.put("LOAN_ORIGINATION", 0.70);

        // External data providers (medium-low authority)
        SOURCE_AUTHORITY.put("DUN_AND_BRADSTREET", 0.65);
        SOURCE_AUTHORITY.put("EXPERIAN", 0.65);
        SOURCE_AUTHORITY.put("BLOOMBERG", 0.65);

        // Web scraping / public sources (low authority)
        SOURCE_AUTHORITY.put("WEB_SCRAPE", 0.40);
        SOURCE_AUTHORITY.put("PUBLIC_RECORDS", 0.50);

        // Default for unknown sources
        SOURCE_AUTHORITY.put("UNKNOWN", 0.30);
    }

    /**
     * Calculate overall data quality score for a party
     *
     * @param party The party to score
     * @return Quality score 0.0-1.0 (1.0 = highest quality)
     */
    public double calculateQualityScore(Party party) {
        // Get master source (highest priority source record)
        SourceRecord masterSource = party.getMasterSource();
        if (masterSource == null) {
            log.warn("No master source for party {}, using defaults", party.getFederatedId());
            return 0.5; // Default medium quality
        }

        // Calculate three quality dimensions
        double completeness = calculateCompleteness(party);
        double freshness = calculateFreshness(masterSource.getSyncedAt());
        double sourceAuthority = getSourceAuthority(masterSource.getSourceSystem());

        // Weighted average: Completeness (40%), Freshness (30%), Authority (30%)
        double qualityScore = (completeness * 0.4) + (freshness * 0.3) + (sourceAuthority * 0.3);

        log.debug("Quality score for party {}: overall={}, completeness={}, freshness={}, authority={}",
                party.getFederatedId(), qualityScore, completeness, freshness, sourceAuthority);

        return qualityScore;
    }

    /**
     * Calculate completeness score (0.0-1.0)
     *
     * Measures how many required fields are populated
     */
    private double calculateCompleteness(Party party) {
        int totalFields = 0;
        int populatedFields = 0;

        // Check base party fields
        if (party.getFederatedId() != null) {
            totalFields++;
            populatedFields++;
        }
        totalFields++;

        if (party.getPartyType() != null) {
            totalFields++;
            populatedFields++;
        }
        totalFields++;

        if (party.getStatus() != null) {
            totalFields++;
            populatedFields++;
        }
        totalFields++;

        // Check organization-specific fields (if applicable)
        if (party instanceof com.bank.product.party.domain.Organization) {
            com.bank.product.party.domain.Organization org =
                    (com.bank.product.party.domain.Organization) party;

            // Critical fields
            totalFields += 7;
            if (org.getLegalName() != null && !org.getLegalName().isEmpty()) populatedFields++;
            if (org.getRegistrationNumber() != null && !org.getRegistrationNumber().isEmpty()) populatedFields++;
            if (org.getJurisdiction() != null && !org.getJurisdiction().isEmpty()) populatedFields++;
            if (org.getIncorporationDate() != null) populatedFields++;
            if (org.getIndustryCode() != null && !org.getIndustryCode().isEmpty()) populatedFields++;
            if (org.getPhoneNumber() != null && !org.getPhoneNumber().isEmpty()) populatedFields++;
            if (org.getEmail() != null && !org.getEmail().isEmpty()) populatedFields++;

            // Optional fields (lower weight)
            totalFields += 3;
            if (org.getLei() != null && !org.getLei().isEmpty()) populatedFields++;
            if (org.getWebsite() != null && !org.getWebsite().isEmpty()) populatedFields++;
            if (org.getTier() != null && !org.getTier().isEmpty()) populatedFields++;
        }

        // Check individual-specific fields
        if (party instanceof com.bank.product.party.domain.Individual) {
            com.bank.product.party.domain.Individual ind =
                    (com.bank.product.party.domain.Individual) party;

            totalFields += 5;
            if (ind.getFirstName() != null && !ind.getFirstName().isEmpty()) populatedFields++;
            if (ind.getLastName() != null && !ind.getLastName().isEmpty()) populatedFields++;
            if (ind.getDateOfBirth() != null) populatedFields++;
            if (ind.getNationality() != null && !ind.getNationality().isEmpty()) populatedFields++;
            if (ind.getEmail() != null && !ind.getEmail().isEmpty()) populatedFields++;
        }

        return totalFields > 0 ? (double) populatedFields / totalFields : 0.0;
    }

    /**
     * Calculate freshness score using exponential decay
     *
     * @param lastUpdated When the data was last updated
     * @return Freshness score 0.0-1.0 (1.0 = very fresh, 0.5 = half-life age, 0.0 = very stale)
     */
    private double calculateFreshness(Instant lastUpdated) {
        if (lastUpdated == null) {
            log.warn("No lastUpdated timestamp, using minimum freshness");
            return 0.1; // Very stale if we don't know when it was updated
        }

        long daysSinceUpdate = Duration.between(lastUpdated, Instant.now()).toDays();

        // Exponential decay: freshness = e^(-λ * days)
        // After 180 days (half-life), freshness = 0.5
        // After 360 days, freshness = 0.25
        // After 720 days, freshness = 0.0625
        double freshness = Math.exp(-FRESHNESS_DECAY_LAMBDA * daysSinceUpdate);

        return Math.max(0.0, Math.min(1.0, freshness)); // Clamp to [0, 1]
    }

    /**
     * Get source authority weight
     *
     * @param sourceSystem Name of the source system
     * @return Authority weight 0.0-1.0
     */
    private double getSourceAuthority(String sourceSystem) {
        if (sourceSystem == null) {
            return SOURCE_AUTHORITY.get("UNKNOWN");
        }

        return SOURCE_AUTHORITY.getOrDefault(sourceSystem.toUpperCase(), SOURCE_AUTHORITY.get("UNKNOWN"));
    }

    /**
     * Resolve conflicts between two parties by selecting the higher quality value
     *
     * @param party1 First party
     * @param party2 Second party
     * @return The party with higher overall quality
     */
    public Party resolveConflict(Party party1, Party party2) {
        double score1 = calculateQualityScore(party1);
        double score2 = calculateQualityScore(party2);

        log.info("Resolving conflict: party1={} (score={}), party2={} (score={})",
                party1.getFederatedId(), score1, party2.getFederatedId(), score2);

        return score1 >= score2 ? party1 : party2;
    }

    /**
     * Select the best value from multiple source records for a specific field
     *
     * @param sourceRecords List of source records
     * @param fieldExtractor Function to extract field value from source
     * @return The field value from the highest quality source
     */
    public <T> T selectBestValue(List<SourceRecord> sourceRecords,
                                  java.util.function.Function<SourceRecord, T> fieldExtractor) {
        if (sourceRecords == null || sourceRecords.isEmpty()) {
            return null;
        }

        SourceRecord bestSource = sourceRecords.stream()
                .max((s1, s2) -> {
                    double score1 = calculateSourceRecordQuality(s1);
                    double score2 = calculateSourceRecordQuality(s2);
                    return Double.compare(score1, score2);
                })
                .orElse(null);

        return bestSource != null ? fieldExtractor.apply(bestSource) : null;
    }

    /**
     * Calculate quality score for a source record
     */
    private double calculateSourceRecordQuality(SourceRecord sourceRecord) {
        double freshness = calculateFreshness(sourceRecord.getSyncedAt());
        double authority = getSourceAuthority(sourceRecord.getSourceSystem());

        // Weighted: Authority (60%), Freshness (40%)
        return (authority * 0.6) + (freshness * 0.4);
    }

    /**
     * Calculate confidence boost from multiple confirming sources
     *
     * If multiple high-quality sources agree on a value, increase confidence
     *
     * @param sourceRecords List of source records
     * @param fieldExtractor Function to extract field value
     * @return Confidence multiplier 1.0-1.5 (1.0 = single source, 1.5 = 3+ high-quality sources agree)
     */
    public <T> double calculateConfidenceBoost(List<SourceRecord> sourceRecords,
                                                java.util.function.Function<SourceRecord, T> fieldExtractor) {
        if (sourceRecords == null || sourceRecords.size() <= 1) {
            return 1.0; // No boost for single source
        }

        // Group by field value
        Map<T, Integer> valueCount = new HashMap<>();
        Map<T, Double> valueQuality = new HashMap<>();

        for (SourceRecord record : sourceRecords) {
            T value = fieldExtractor.apply(record);
            if (value != null) {
                valueCount.put(value, valueCount.getOrDefault(value, 0) + 1);

                double quality = calculateSourceRecordQuality(record);
                valueQuality.put(value, Math.max(valueQuality.getOrDefault(value, 0.0), quality));
            }
        }

        // Find most common value with highest quality
        int maxCount = valueCount.values().stream().max(Integer::compareTo).orElse(0);
        double maxQuality = valueQuality.values().stream().max(Double::compareTo).orElse(0.0);

        // Boost calculation:
        // - 2 sources agree: 1.1x
        // - 3+ sources agree: 1.2x
        // - High quality (>0.8): additional 0.1x
        double countBoost = 1.0 + Math.min(maxCount - 1, 2) * 0.1;
        double qualityBoost = maxQuality > 0.8 ? 0.1 : 0.0;

        return Math.min(1.5, countBoost + qualityBoost);
    }
}
