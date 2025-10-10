package com.bank.product.party.resolution;

import com.bank.product.party.domain.Party;
import lombok.Data;

/**
 * Result of entity resolution process
 */
@Data
public class ResolutionResult {
    private ResolutionAction action;
    private Party resultParty;
    private Party matchedParty;
    private Double matchScore;

    public enum ResolutionAction {
        CREATED,        // New party created
        MERGED,         // Automatically merged
        NEEDS_REVIEW    // Requires manual review
    }

    public static ResolutionResult created(Party party) {
        ResolutionResult result = new ResolutionResult();
        result.setAction(ResolutionAction.CREATED);
        result.setResultParty(party);
        return result;
    }

    public static ResolutionResult merged(Party resultParty, Party matchedParty) {
        ResolutionResult result = new ResolutionResult();
        result.setAction(ResolutionAction.MERGED);
        result.setResultParty(resultParty);
        result.setMatchedParty(matchedParty);
        return result;
    }

    public static ResolutionResult needsReview(Party party, Party matchedParty, Double score) {
        ResolutionResult result = new ResolutionResult();
        result.setAction(ResolutionAction.NEEDS_REVIEW);
        result.setResultParty(party);
        result.setMatchedParty(matchedParty);
        result.setMatchScore(score);
        return result;
    }
}
