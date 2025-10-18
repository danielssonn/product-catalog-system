package com.bank.product.party.resolution;

import com.bank.product.party.domain.Organization;
import com.bank.product.party.domain.Party;
import com.bank.product.party.domain.PartyStatus;
import com.bank.product.party.domain.PartyType;
import com.bank.product.party.matching.AddressNormalizer;
import com.bank.product.party.matching.PhoneticMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for EntityMatcher with integrated PhoneticMatcher and AddressNormalizer.
 *
 * Tests the enhanced entity matching with multi-strategy approach:
 * - Exact identifier matching (LEI, Registration Number + Jurisdiction)
 * - Phonetic name matching
 * - Jaro-Winkler similarity
 * - Levenshtein distance
 * - Weighted scoring across all strategies
 */
class EntityMatcherTest {

    private EntityMatcher entityMatcher;
    private PhoneticMatcher phoneticMatcher;
    private AddressNormalizer addressNormalizer;

    @BeforeEach
    void setUp() {
        phoneticMatcher = new PhoneticMatcher();
        addressNormalizer = new AddressNormalizer();
        entityMatcher = new EntityMatcher(phoneticMatcher, addressNormalizer);
    }

    // ===== LEI Matching Tests =====

    @Test
    @DisplayName("Should auto-merge on exact LEI match")
    void testExactLEIMatch() {
        Organization org1 = createOrganization("Goldman Sachs", "5493000F4ZO33MV32P92", null, "US");
        Organization org2 = createOrganization("Goldman Sachs Group Inc", "5493000F4ZO33MV32P92", null, "US");

        List<Party> existing = Arrays.asList(org2);
        List<MatchCandidate> candidates = entityMatcher.findCandidates(org1, existing);

        assertFalse(candidates.isEmpty(), "Should find LEI match");
        MatchCandidate best = candidates.get(0);
        assertTrue(best.getScore() >= 0.95, "LEI match should have very high score, got: " + best.getScore());
        assertEquals(MatchAction.AUTO_MERGE, best.getRecommendedAction(), "Should recommend auto-merge");
        assertTrue(best.getMatchingFields().contains("lei"), "Should identify LEI as matching field");
    }

    @Test
    @DisplayName("Should not match on different LEIs")
    void testDifferentLEIs() {
        Organization org1 = createOrganization("Goldman Sachs", "5493000F4ZO33MV32P92", null, "US");
        Organization org2 = createOrganization("JPMorgan Chase", "8I5DZWZKVSZI1NUHU748", null, "US");

        List<Party> existing = Arrays.asList(org2);
        List<MatchCandidate> candidates = entityMatcher.findCandidates(org1, existing);

        // Should not match based on LEI alone (different LEIs)
        if (!candidates.isEmpty()) {
            MatchCandidate best = candidates.get(0);
            assertTrue(best.getScore() < 0.95, "Different LEIs should not score highly");
        }
    }

    // ===== Registration Number + Jurisdiction Tests =====

    @Test
    @DisplayName("Should match on registration number + jurisdiction")
    void testRegistrationNumberJurisdictionMatch() {
        Organization org1 = createOrganization("Apple Inc", null, "C0806592", "California");
        Organization org2 = createOrganization("Apple Incorporated", null, "C0806592", "California");

        List<Party> existing = Arrays.asList(org2);
        List<MatchCandidate> candidates = entityMatcher.findCandidates(org1, existing);

        assertFalse(candidates.isEmpty(), "Should find registration match");
        MatchCandidate best = candidates.get(0);
        assertTrue(best.getScore() >= 0.95, "Registration + jurisdiction match should score high: " + best.getScore());
        assertTrue(best.getMatchingFields().contains("registrationNumber"), "Should identify registration number as matching");
    }

    @Test
    @DisplayName("Should not match on registration number with different jurisdiction")
    void testDifferentJurisdictions() {
        Organization org1 = createOrganization("ABC Corp", null, "12345", "Delaware");
        Organization org2 = createOrganization("ABC Corporation", null, "12345", "Nevada");

        List<Party> existing = Arrays.asList(org2);
        List<MatchCandidate> candidates = entityMatcher.findCandidates(org1, existing);

        // Should not match if jurisdictions are different
        if (!candidates.isEmpty()) {
            MatchCandidate best = candidates.get(0);
            assertFalse(best.getMatchingFields().contains("registrationNumber"),
                    "Should not match registration with different jurisdiction");
        }
    }

    // ===== Phonetic Name Matching Tests =====

    @Test
    @DisplayName("Should match JPMorgan variations using phonetic similarity")
    void testJPMorganPhoneticMatch() {
        Organization org1 = createOrganization("JPMorgan Chase", null, null, "US");
        Organization org2 = createOrganization("J.P. Morgan Chase & Co", null, null, "US");

        List<Party> existing = Arrays.asList(org2);
        List<MatchCandidate> candidates = entityMatcher.findCandidates(org1, existing);

        assertFalse(candidates.isEmpty(), "Should find phonetic match");
        MatchCandidate best = candidates.get(0);
        assertTrue(best.getScore() >= 0.75, "Phonetic match should score >= 0.75: " + best.getScore());
        assertFalse(best.getMatchingFields().isEmpty(), "Should have matching fields");
    }

    @Test
    @DisplayName("Should match Bank of America variations")
    void testBankOfAmericaVariations() {
        Organization org1 = createOrganization("Bank of America Corporation", null, null, "US");
        Organization org2 = createOrganization("Bank of America Corp", null, null, "US");

        List<Party> existing = Arrays.asList(org2);
        List<MatchCandidate> candidates = entityMatcher.findCandidates(org1, existing);

        assertFalse(candidates.isEmpty(), "Should find name match");
        MatchCandidate best = candidates.get(0);
        assertTrue(best.getScore() >= 0.90, "Corporation vs Corp should match closely: " + best.getScore());
        assertEquals(MatchAction.AUTO_MERGE, best.getRecommendedAction(), "Should recommend auto-merge");
    }

    @Test
    @DisplayName("Should handle LLC variations")
    void testLLCVariations() {
        Organization org1 = createOrganization("Acme Company LLC", null, null, "Delaware");
        Organization org2 = createOrganization("Acme Company Limited Liability Company", null, null, "Delaware");

        List<Party> existing = Arrays.asList(org2);
        List<MatchCandidate> candidates = entityMatcher.findCandidates(org1, existing);

        assertFalse(candidates.isEmpty(), "Should find LLC variation match");
        MatchCandidate best = candidates.get(0);
        assertTrue(best.getScore() >= 0.75, "LLC variations should match: " + best.getScore());
    }

    // ===== Multi-Strategy Matching Tests =====

    @Test
    @DisplayName("Should use maximum score from all strategies")
    void testMultiStrategyMaximum() {
        // Create organizations with variations that different strategies will handle differently
        Organization org1 = createOrganization("Goldman Sachs Group Inc", null, null, "US");
        Organization org2 = createOrganization("The Goldman Sachs Group, Inc.", null, null, "US");

        List<Party> existing = Arrays.asList(org2);
        List<MatchCandidate> candidates = entityMatcher.findCandidates(org1, existing);

        assertFalse(candidates.isEmpty(), "Should find match using multi-strategy");
        MatchCandidate best = candidates.get(0);
        assertTrue(best.getScore() >= 0.85,
                "Multi-strategy should achieve high score: " + best.getScore());
    }

    // ===== Manual Review Threshold Tests =====

    @Test
    @DisplayName("Should recommend manual review for medium confidence matches")
    void testManualReviewThreshold() {
        // Create organizations with moderate similarity
        Organization org1 = createOrganization("Microsoft Corporation", null, null, "Washington");
        Organization org2 = createOrganization("Micro Soft Corp", null, null, "Washington");

        List<Party> existing = Arrays.asList(org2);
        List<MatchCandidate> candidates = entityMatcher.findCandidates(org1, existing);

        if (!candidates.isEmpty()) {
            MatchCandidate best = candidates.get(0);
            if (best.getScore() >= 0.75 && best.getScore() < 0.95) {
                assertEquals(MatchAction.MANUAL_REVIEW, best.getRecommendedAction(),
                        "Score in [0.75, 0.95) should recommend manual review");
            }
        }
    }

    @Test
    @DisplayName("Should not return matches below threshold")
    void testBelowThreshold() {
        Organization org1 = createOrganization("Goldman Sachs", null, null, "US");
        Organization org2 = createOrganization("Morgan Stanley", null, null, "US");

        List<Party> existing = Arrays.asList(org2);
        List<MatchCandidate> candidates = entityMatcher.findCandidates(org1, existing);

        // Should not return candidates below 0.75 threshold
        for (MatchCandidate candidate : candidates) {
            assertTrue(candidate.getScore() >= 0.75,
                    "All returned candidates should be >= 0.75 threshold, got: " + candidate.getScore());
        }
    }

    // ===== Multiple Candidates Tests =====

    @Test
    @DisplayName("Should return candidates sorted by score descending")
    void testCandidatesSortedByScore() {
        Organization newParty = createOrganization("Apple Inc", null, null, "California");

        Organization existing1 = createOrganization("Apple Incorporated", null, null, "California");
        Organization existing2 = createOrganization("Apple Computer", null, null, "California");
        Organization existing3 = createOrganization("The Apple Company", null, null, "California");

        List<Party> existing = Arrays.asList(existing1, existing2, existing3);
        List<MatchCandidate> candidates = entityMatcher.findCandidates(newParty, existing);

        // Verify sorted by score descending
        for (int i = 0; i < candidates.size() - 1; i++) {
            assertTrue(candidates.get(i).getScore() >= candidates.get(i + 1).getScore(),
                    "Candidates should be sorted by score descending");
        }
    }

    @Test
    @DisplayName("Should filter out same party")
    void testFilterSameParty() {
        Organization org = createOrganization("Goldman Sachs", null, null, "US");
        org.setFederatedId("test-id-123");

        List<Party> existing = Arrays.asList(org);  // Same party
        List<MatchCandidate> candidates = entityMatcher.findCandidates(org, existing);

        assertTrue(candidates.isEmpty(), "Should not match against itself");
    }

    @Test
    @DisplayName("Should filter out different party types")
    void testFilterDifferentPartyTypes() {
        Organization org1 = createOrganization("Test Company", null, null, "US");
        org1.setPartyType(PartyType.ORGANIZATION);

        Party org2 = new Party() {};  // Different type
        org2.setPartyType(PartyType.INDIVIDUAL);
        org2.setFederatedId("diff-id");
        org2.setStatus(PartyStatus.ACTIVE);

        List<Party> existing = Arrays.asList(org2);
        List<MatchCandidate> candidates = entityMatcher.findCandidates(org1, existing);

        assertTrue(candidates.isEmpty(), "Should not match different party types");
    }

    // ===== Real-World Comprehensive Tests =====

    @Test
    @DisplayName("Should match Goldman Sachs with multiple variations")
    void testGoldmanSachsComprehensive() {
        Organization newParty = createOrganization(
                "Goldman Sachs Group Inc",
                "5493000F4ZO33MV32P92",
                "13-5108880",
                "Delaware"
        );

        Organization existing1 = createOrganization(
                "The Goldman Sachs Group, Inc.",
                "5493000F4ZO33MV32P92",
                "13-5108880",
                "Delaware"
        );

        Organization existing2 = createOrganization(
                "Goldman Sachs International",
                null,
                "FC024017",
                "United Kingdom"
        );

        List<Party> existing = Arrays.asList(existing1, existing2);
        List<MatchCandidate> candidates = entityMatcher.findCandidates(newParty, existing);

        assertFalse(candidates.isEmpty(), "Should find at least one match");

        // First candidate should be existing1 (exact LEI + Registration match)
        MatchCandidate best = candidates.get(0);
        assertEquals(existing1.getFederatedId(), best.getExistingParty().getFederatedId(),
                "Best match should be the one with matching LEI and registration");
        assertTrue(best.getScore() >= 0.95, "Exact identifier match should score very high");
        assertEquals(MatchAction.AUTO_MERGE, best.getRecommendedAction());
    }

    // ===== Helper Methods =====

    private Organization createOrganization(String legalName, String lei,
                                           String registrationNumber, String jurisdiction) {
        Organization org = new Organization();
        org.setLegalName(legalName);
        org.setLei(lei);
        org.setRegistrationNumber(registrationNumber);
        org.setJurisdiction(jurisdiction);
        org.setPartyType(PartyType.ORGANIZATION);
        org.setStatus(PartyStatus.ACTIVE);
        org.setConfidence(1.0);

        // Set a unique federated ID
        org.setFederatedId("org-" + System.nanoTime() + "-" + (int)(Math.random() * 1000));

        return org;
    }
}
