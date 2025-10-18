package com.bank.product.party.resolution;

import com.bank.product.party.domain.Organization;
import com.bank.product.party.domain.Party;
import com.bank.product.party.matching.PhoneticMatcher;
import com.bank.product.party.matching.AddressNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity matching component for entity resolution.
 * Implements various matching strategies to identify duplicate entities.
 *
 * Enhanced with phonetic matching and address normalization for improved accuracy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EntityMatcher {

    private final PhoneticMatcher phoneticMatcher;
    private final AddressNormalizer addressNormalizer;

    // Matching thresholds
    private static final double AUTO_MERGE_THRESHOLD = 0.95;
    private static final double MANUAL_REVIEW_THRESHOLD = 0.75;
    private static final double FUZZY_NAME_THRESHOLD = 0.85;
    private static final double PHONETIC_NAME_THRESHOLD = 0.90;
    private static final double ADDRESS_SIMILARITY_THRESHOLD = 0.85;

    /**
     * Find candidate matches for a party
     */
    public List<MatchCandidate> findCandidates(Party party, List<Party> existingParties) {
        List<MatchCandidate> candidates = new ArrayList<>();

        for (Party existing : existingParties) {
            // Skip if same party
            if (party.getFederatedId() != null &&
                    party.getFederatedId().equals(existing.getFederatedId())) {
                continue;
            }

            // Skip if different types
            if (party.getPartyType() != existing.getPartyType()) {
                continue;
            }

            double score = calculateSimilarity(party, existing);

            if (score >= MANUAL_REVIEW_THRESHOLD) {
                MatchCandidate candidate = new MatchCandidate();
                candidate.setExistingParty(existing);
                candidate.setScore(score);
                candidate.setMatchingFields(getMatchingFields(party, existing));
                candidate.setRecommendedAction(
                        score >= AUTO_MERGE_THRESHOLD ? MatchAction.AUTO_MERGE : MatchAction.MANUAL_REVIEW
                );
                candidates.add(candidate);
            }
        }

        candidates.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return candidates;
    }

    /**
     * Calculate overall similarity between two parties
     */
    private double calculateSimilarity(Party p1, Party p2) {
        if (p1 instanceof Organization && p2 instanceof Organization) {
            return calculateOrganizationSimilarity((Organization) p1, (Organization) p2);
        }
        // Add other party types as needed
        return 0.0;
    }

    /**
     * Calculate similarity for organizations
     * Enhanced with phonetic matching and address normalization
     */
    private double calculateOrganizationSimilarity(Organization org1, Organization org2) {
        List<Double> scores = new ArrayList<>();
        List<Double> weights = new ArrayList<>();

        // LEI match (highest confidence - exact identifier)
        if (org1.getLei() != null && org2.getLei() != null) {
            scores.add(org1.getLei().equals(org2.getLei()) ? 1.0 : 0.0);
            weights.add(1.0); // Highest weight
        }

        // Registration number + Jurisdiction (near-exact identifier)
        if (org1.getRegistrationNumber() != null && org2.getRegistrationNumber() != null &&
                org1.getJurisdiction() != null && org2.getJurisdiction() != null) {
            boolean regMatch = org1.getRegistrationNumber().equals(org2.getRegistrationNumber());
            boolean jurMatch = org1.getJurisdiction().equalsIgnoreCase(org2.getJurisdiction());
            scores.add(regMatch && jurMatch ? 1.0 : 0.0);
            weights.add(0.95);
        }

        // Legal name - Multi-strategy fuzzy matching
        if (org1.getLegalName() != null && org2.getLegalName() != null) {
            // Strategy 1: Phonetic similarity (handles "JPMorgan" vs "J.P. Morgan")
            double phoneticScore = phoneticMatcher.calculatePhoneticSimilarity(
                    org1.getLegalName(),
                    org2.getLegalName()
            );

            // Strategy 2: Jaro-Winkler (better for common prefixes)
            double jaroScore = phoneticMatcher.jaroWinklerSimilarity(
                    normalizeLegalName(org1.getLegalName()),
                    normalizeLegalName(org2.getLegalName())
            );

            // Strategy 3: Levenshtein (character-level similarity)
            double levenScore = calculateStringSimilarity(
                    normalizeLegalName(org1.getLegalName()),
                    normalizeLegalName(org2.getLegalName())
            );

            // Take maximum score from all strategies
            double bestNameScore = Math.max(phoneticScore, Math.max(jaroScore, levenScore));
            scores.add(bestNameScore);
            weights.add(0.85);

            log.debug("Name matching scores - Phonetic: {}, Jaro-Winkler: {}, Levenshtein: {}, Best: {}",
                    phoneticScore, jaroScore, levenScore, bestNameScore);
        }

        // TODO: Address similarity with normalization (when Address relationship is added to Organization)
        // Neo4j graph model currently doesn't have address fields on Organization node

        // Jurisdiction match (lower weight, supplementary)
        if (org1.getJurisdiction() != null && org2.getJurisdiction() != null) {
            scores.add(org1.getJurisdiction().equalsIgnoreCase(org2.getJurisdiction()) ? 1.0 : 0.0);
            weights.add(0.5);
        }

        // Industry code (weak signal, but useful tie-breaker)
        if (org1.getIndustryCode() != null && org2.getIndustryCode() != null) {
            scores.add(org1.getIndustryCode().equals(org2.getIndustryCode()) ? 1.0 : 0.0);
            weights.add(0.3);
        }

        double finalScore = calculateWeightedAverage(scores, weights);
        log.debug("Organization similarity: {} (from {} signals)", finalScore, scores.size());

        return finalScore;
    }

    /**
     * Normalize legal name for comparison
     */
    private String normalizeLegalName(String legalName) {
        return legalName
                .toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll(",\\s*(inc|llc|ltd|corp|corporation|limited|plc)\\.?$", "")
                .trim();
    }

    /**
     * Calculate string similarity using Levenshtein distance
     */
    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        if (s1.equals(s2)) return 1.0;

        int distance = levenshteinDistance(s1, s2);
        int maxLen = Math.max(s1.length(), s2.length());
        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Calculate Levenshtein distance
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Calculate weighted average
     */
    private double calculateWeightedAverage(List<Double> scores, List<Double> weights) {
        if (scores.isEmpty()) return 0.0;

        double totalScore = 0.0;
        double totalWeight = 0.0;

        for (int i = 0; i < scores.size(); i++) {
            totalScore += scores.get(i) * weights.get(i);
            totalWeight += weights.get(i);
        }

        return totalWeight > 0 ? totalScore / totalWeight : 0.0;
    }

    /**
     * Get list of fields that matched
     */
    private List<String> getMatchingFields(Party p1, Party p2) {
        List<String> matches = new ArrayList<>();

        if (p1 instanceof Organization && p2 instanceof Organization) {
            Organization org1 = (Organization) p1;
            Organization org2 = (Organization) p2;

            if (org1.getLei() != null && org1.getLei().equals(org2.getLei())) {
                matches.add("lei");
            }
            if (org1.getRegistrationNumber() != null &&
                    org1.getRegistrationNumber().equals(org2.getRegistrationNumber())) {
                matches.add("registrationNumber");
            }
            if (org1.getLegalName() != null &&
                    calculateStringSimilarity(
                            normalizeLegalName(org1.getLegalName()),
                            normalizeLegalName(org2.getLegalName())
                    ) >= FUZZY_NAME_THRESHOLD) {
                matches.add("legalName");
            }
            if (org1.getJurisdiction() != null &&
                    org1.getJurisdiction().equalsIgnoreCase(org2.getJurisdiction())) {
                matches.add("jurisdiction");
            }
        }

        return matches;
    }
}
