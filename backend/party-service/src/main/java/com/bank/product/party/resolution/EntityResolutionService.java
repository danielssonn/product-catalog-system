package com.bank.product.party.resolution;

import com.bank.product.party.domain.*;
import com.bank.product.party.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Service for entity resolution - identifying and merging duplicate parties.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EntityResolutionService {

    private final PartyRepository partyRepository;
    private final EntityMatcher entityMatcher;

    /**
     * Resolve a new party against existing parties
     */
    @Transactional
    public ResolutionResult resolve(Party newParty) {
        log.info("Starting entity resolution for party: {}", newParty);

        // Find all existing parties of the same type
        List<Party> existingParties = partyRepository.findByStatus(PartyStatus.ACTIVE);

        // Find candidate matches
        List<MatchCandidate> candidates = entityMatcher.findCandidates(newParty, existingParties);

        if (candidates.isEmpty()) {
            log.info("No matches found, creating new party");
            newParty.setConfidence(1.0);
            Party saved = partyRepository.save(newParty);
            return ResolutionResult.created(saved);
        }

        MatchCandidate bestMatch = candidates.get(0);
        log.info("Best match found with score: {}, action: {}",
                bestMatch.getScore(), bestMatch.getRecommendedAction());

        if (bestMatch.getRecommendedAction() == MatchAction.AUTO_MERGE) {
            // Auto-merge
            Party merged = merge(newParty, bestMatch.getExistingParty(), bestMatch.getScore(), true);
            return ResolutionResult.merged(merged, bestMatch.getExistingParty());
        } else {
            // Create duplicate relationship for manual review
            createDuplicateCandidate(newParty, bestMatch);
            newParty.setStatus(PartyStatus.UNDER_REVIEW);
            Party saved = partyRepository.save(newParty);
            return ResolutionResult.needsReview(saved, bestMatch.getExistingParty(), bestMatch.getScore());
        }
    }

    /**
     * Merge two parties
     */
    @Transactional
    public Party merge(Party source, Party target, Double confidence, boolean automatic) {
        log.info("Merging party {} into {}", source.getFederatedId(), target.getFederatedId());

        // Create merge relationship
        PartyMerge mergeRel = new PartyMerge();
        mergeRel.setSourceparty(source);
        mergeRel.setMergeDate(Instant.now());
        mergeRel.setMergeReason("Entity resolution");
        mergeRel.setConfidenceScore(confidence);
        mergeRel.setAutomatic(automatic);

        target.getMergedFrom().add(mergeRel);

        // Merge source records
        if (source.getSourceRecords() != null) {
            for (SourceRecord sourceRecord : source.getSourceRecords()) {
                target.addSourceRecord(sourceRecord);
            }
        }

        // Update target confidence
        target.setConfidence(Math.max(target.getConfidence(), confidence));
        target.markUpdated();

        // Mark source as merged
        source.setStatus(PartyStatus.MERGED);

        partyRepository.save(source);
        return partyRepository.save(target);
    }

    /**
     * Create duplicate candidate for manual review
     */
    private void createDuplicateCandidate(Party party, MatchCandidate match) {
        DuplicateCandidate duplicate = new DuplicateCandidate();
        duplicate.setCandidateParty(match.getExistingParty());
        duplicate.setSimilarityScore(match.getScore());
        duplicate.setResolutionStatus("NEEDS_REVIEW");

        for (String field : match.getMatchingFields()) {
            duplicate.addMatchingField(field);
        }

        party.getDuplicateCandidates().add(duplicate);
    }

    /**
     * Find duplicate candidates above threshold
     */
    public List<Party> findDuplicates(Double threshold) {
        return partyRepository.findDuplicateCandidates(threshold);
    }

    /**
     * Manually approve a merge
     */
    @Transactional
    public Party approveMerge(String sourceId, String targetId, String approvedBy) {
        Party source = partyRepository.findByFederatedId(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source party not found"));
        Party target = partyRepository.findByFederatedId(targetId)
                .orElseThrow(() -> new IllegalArgumentException("Target party not found"));

        return merge(source, target, 1.0, false);
    }

    /**
     * Mark as not duplicate
     */
    @Transactional
    public void markNotDuplicate(String partyId, String candidateId, String reviewedBy) {
        Party party = partyRepository.findByFederatedId(partyId)
                .orElseThrow(() -> new IllegalArgumentException("Party not found"));

        party.getDuplicateCandidates().stream()
                .filter(dc -> dc.getCandidateParty().getFederatedId().equals(candidateId))
                .forEach(dc -> {
                    dc.setResolutionStatus("NOT_DUPLICATE");
                    dc.setResolvedAt(Instant.now());
                    dc.setResolvedBy(reviewedBy);
                });

        if (party.getDuplicateCandidates().stream().noneMatch(DuplicateCandidate::isPending)) {
            party.setStatus(PartyStatus.ACTIVE);
        }

        partyRepository.save(party);
    }
}
