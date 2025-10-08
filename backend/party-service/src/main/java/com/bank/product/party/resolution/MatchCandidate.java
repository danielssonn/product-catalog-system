package com.bank.product.party.resolution;

import com.bank.product.party.domain.Party;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a potential match candidate during entity resolution
 */
@Data
public class MatchCandidate {
    private Party existingParty;
    private Double score;
    private List<String> matchingFields = new ArrayList<>();
    private MatchAction recommendedAction;
}
