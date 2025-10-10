package com.bank.product.party.sync;

import com.bank.product.party.domain.Party;

import java.util.List;
import java.util.Map;

/**
 * Interface for source system adapters.
 * Each source system (Commercial Banking, Capital Markets, etc.) implements this interface.
 */
public interface SourceSystemAdapter {

    /**
     * Get source system identifier
     */
    String getSourceSystemId();

    /**
     * Fetch all party IDs from source system
     */
    List<String> fetchAllPartyIds();

    /**
     * Fetch party data by ID
     */
    Map<String, Object> fetchParty(String partyId);

    /**
     * Transform source system data to Party entity
     */
    Party transformToParty(Map<String, Object> sourceData);

    /**
     * Check if source system is available
     */
    boolean isAvailable();
}
