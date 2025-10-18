package com.bank.product.party.document;

import com.bank.product.party.domain.Organization;
import com.bank.product.party.domain.Party;
import com.bank.product.party.domain.PartyStatus;
import com.bank.product.party.domain.PartyType;
import com.bank.product.party.matching.PhoneticMatcher;
import com.bank.product.party.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for resolving entity references in documents to actual Party records.
 *
 * When a document mentions an entity (e.g., parent company, officer, beneficial owner),
 * this service:
 * 1. Searches for existing parties with matching names
 * 2. If found: Returns the existing party
 * 3. If not found: Creates a PLACEHOLDER party for future verification
 *
 * PLACEHOLDER parties:
 * - Have limited information (name, potentially jurisdiction)
 * - Status = PLACEHOLDER
 * - Confidence score < 1.0
 * - Require human review and verification
 * - Can be merged with existing parties or upgraded to ACTIVE after verification
 *
 * Based on ENTITY_RESOLUTION_DESIGN.md Section 1C: Predictive Graph Construction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntityReferenceResolver {

    private final PartyRepository partyRepository;
    private final PhoneticMatcher phoneticMatcher;

    /**
     * Fuzzy match threshold for entity name matching
     * If similarity >= 0.85, consider it a match to existing party
     */
    private static final double FUZZY_MATCH_THRESHOLD = 0.85;

    /**
     * Resolve an entity reference from a document to a Party
     *
     * @param entityName Name of the entity as mentioned in the document
     * @param context Additional context (document type, extraction source)
     * @return Existing party or newly created PLACEHOLDER party
     */
    @Transactional
    public Party resolveEntityReference(String entityName, String context) {
        return resolveEntityReference(entityName, null, null, context);
    }

    /**
     * Resolve an entity reference with additional identifying information
     *
     * @param entityName Name of the entity
     * @param jurisdiction Jurisdiction if known
     * @param entityType Type of entity (ORGANIZATION, INDIVIDUAL)
     * @param context Additional context
     * @return Existing party or newly created PLACEHOLDER party
     */
    @Transactional
    public Party resolveEntityReference(String entityName, String jurisdiction,
                                        PartyType entityType, String context) {

        log.info("Resolving entity reference: name={}, jurisdiction={}, type={}, context={}",
                entityName, jurisdiction, entityType, context);

        if (entityName == null || entityName.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity name cannot be null or empty");
        }

        // Step 1: Search for exact name match
        List<Party> exactMatches = partyRepository.findByLegalNameIgnoreCase(entityName);

        if (!exactMatches.isEmpty()) {
            Party match = filterBestMatch(exactMatches, jurisdiction);
            log.info("Found exact match for '{}': party={}, status={}",
                    entityName, match.getFederatedId(), match.getStatus());
            return match;
        }

        // Step 2: Search for fuzzy name match (phonetic similarity)
        List<Party> allParties = partyRepository.findByStatus(PartyStatus.ACTIVE);
        Party fuzzyMatch = findFuzzyMatch(entityName, allParties);

        if (fuzzyMatch != null) {
            log.info("Found fuzzy match for '{}': party={}, legalName={}",
                    entityName, fuzzyMatch.getFederatedId(), getLegalName(fuzzyMatch));
            return fuzzyMatch;
        }

        // Step 3: Check for existing PLACEHOLDER with same name
        List<Party> placeholders = partyRepository.findByStatus(PartyStatus.PLACEHOLDER);
        for (Party placeholder : placeholders) {
            String placeholderName = getLegalName(placeholder);
            if (placeholderName != null && placeholderName.equalsIgnoreCase(entityName)) {
                log.info("Found existing PLACEHOLDER for '{}': party={}",
                        entityName, placeholder.getFederatedId());
                return placeholder;
            }
        }

        // Step 4: No match found - create PLACEHOLDER party
        Party placeholder = createPlaceholderParty(entityName, jurisdiction, entityType, context);
        Party saved = partyRepository.save(placeholder);

        log.info("Created PLACEHOLDER party for '{}': party={}, requiresVerification=true",
                entityName, saved.getFederatedId());

        return saved;
    }

    /**
     * Upgrade a PLACEHOLDER party to ACTIVE status after verification
     *
     * @param placeholderParty The placeholder party to upgrade
     * @param verifiedData Complete verified data for the party
     * @return The upgraded party
     */
    @Transactional
    public Party upgradePlaceholderToActive(Party placeholderParty, Map<String, Object> verifiedData) {
        if (placeholderParty.getStatus() != PartyStatus.PLACEHOLDER) {
            throw new IllegalStateException("Can only upgrade PLACEHOLDER parties");
        }

        log.info("Upgrading PLACEHOLDER party to ACTIVE: party={}", placeholderParty.getFederatedId());

        // Update status and confidence
        placeholderParty.setStatus(PartyStatus.ACTIVE);
        placeholderParty.setConfidence(1.0);

        // Apply verified data
        // TODO: Apply verifiedData fields to the party based on party type

        placeholderParty.markUpdated();
        Party upgraded = partyRepository.save(placeholderParty);

        log.info("Successfully upgraded PLACEHOLDER party to ACTIVE: party={}",
                upgraded.getFederatedId());

        return upgraded;
    }

    // ===== Helper Methods =====

    /**
     * Create a PLACEHOLDER party for an unresolved entity reference
     */
    private Party createPlaceholderParty(String entityName, String jurisdiction,
                                         PartyType entityType, String context) {

        // Default to ORGANIZATION if type not specified
        PartyType type = entityType != null ? entityType : PartyType.ORGANIZATION;

        Party placeholder;

        if (type == PartyType.ORGANIZATION) {
            Organization org = new Organization();
            org.setLegalName(entityName);
            org.setJurisdiction(jurisdiction);
            placeholder = org;
        } else {
            // For individuals, we'd create an Individual instance
            // For now, default to Organization
            Organization org = new Organization();
            org.setLegalName(entityName);
            org.setJurisdiction(jurisdiction);
            placeholder = org;
        }

        placeholder.setFederatedId(UUID.randomUUID().toString());
        placeholder.setStatus(PartyStatus.PLACEHOLDER);
        placeholder.setConfidence(0.70); // Lower confidence for unverified entity

        log.debug("Created PLACEHOLDER party: name={}, jurisdiction={}, type={}, context={}",
                entityName, jurisdiction, type, context);

        return placeholder;
    }

    /**
     * Filter best match from multiple exact matches
     * Prefer ACTIVE over PLACEHOLDER, and match by jurisdiction if provided
     */
    private Party filterBestMatch(List<Party> matches, String jurisdiction) {
        if (matches.size() == 1) {
            return matches.get(0);
        }

        // Prefer ACTIVE status over PLACEHOLDER
        Party bestMatch = matches.stream()
                .filter(p -> p.getStatus() == PartyStatus.ACTIVE)
                .findFirst()
                .orElse(matches.get(0));

        // If jurisdiction provided, try to match
        if (jurisdiction != null) {
            Party jurisdictionMatch = matches.stream()
                    .filter(p -> p.getStatus() == PartyStatus.ACTIVE)
                    .filter(p -> matchesJurisdiction(p, jurisdiction))
                    .findFirst()
                    .orElse(bestMatch);

            return jurisdictionMatch;
        }

        return bestMatch;
    }

    /**
     * Find fuzzy match using phonetic similarity
     */
    private Party findFuzzyMatch(String entityName, List<Party> parties) {
        for (Party party : parties) {
            String legalName = getLegalName(party);
            if (legalName == null) {
                continue;
            }

            double similarity = phoneticMatcher.calculatePhoneticSimilarity(
                    entityName, legalName
            );

            if (similarity >= FUZZY_MATCH_THRESHOLD) {
                log.debug("Fuzzy match found: '{}' ~= '{}' (similarity={})",
                        entityName, legalName, similarity);
                return party;
            }
        }

        return null;
    }

    /**
     * Get legal name from party (handles Organization vs Individual)
     */
    private String getLegalName(Party party) {
        if (party instanceof Organization) {
            return ((Organization) party).getLegalName();
        }
        // TODO: Handle Individual when implemented
        return null;
    }

    /**
     * Check if party matches jurisdiction
     */
    private boolean matchesJurisdiction(Party party, String jurisdiction) {
        if (party instanceof Organization) {
            Organization org = (Organization) party;
            return jurisdiction.equalsIgnoreCase(org.getJurisdiction());
        }
        // TODO: Handle Individual jurisdiction when implemented
        return false;
    }
}
