package com.bank.product.party.controller;

import com.bank.product.party.domain.Party;
import com.bank.product.party.repository.PartyRepository;
import com.bank.product.party.resolution.EntityResolutionService;
import com.bank.product.party.resolution.ResolutionResult;
import com.bank.product.party.service.PartyFederationService;
import com.bank.product.party.service.SyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for party federation operations
 */
@RestController
@RequestMapping("/api/v1/parties")
@RequiredArgsConstructor
public class PartyRestController {

    private final PartyRepository partyRepository;
    private final PartyFederationService federationService;
    private final EntityResolutionService resolutionService;

    /**
     * Get party by ID
     */
    @GetMapping("/{federatedId}")
    public ResponseEntity<Party> getParty(@PathVariable String federatedId) {
        return partyRepository.findByFederatedId(federatedId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Sync party from source system
     */
    @PostMapping("/sync")
    public ResponseEntity<ResolutionResult> syncParty(
            @RequestParam String sourceSystem,
            @RequestParam String sourceId
    ) {
        ResolutionResult result = federationService.syncFromSource(sourceSystem, sourceId);
        return ResponseEntity.ok(result);
    }

    /**
     * Trigger full sync from source system
     */
    @PostMapping("/sync/full")
    public ResponseEntity<SyncResult> fullSync(@RequestParam String sourceSystem) {
        SyncResult result = federationService.syncAllFromSource(sourceSystem);
        return ResponseEntity.ok(result);
    }

    /**
     * Create cross-domain relationship
     */
    @PostMapping("/relationships/cross-domain")
    public ResponseEntity<Void> createCrossDomainRelationship(
            @RequestBody Map<String, Object> request
    ) {
        federationService.createCrossDomainRelationship(
                (String) request.get("agentId"),
                (String) request.get("principalId"),
                (String) request.get("relationshipType"),
                (Map<String, Object>) request.get("properties"),
                (List<String>) request.get("sourceSystems")
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Get duplicate candidates
     */
    @GetMapping("/duplicates")
    public ResponseEntity<List<Party>> getDuplicates(@RequestParam(defaultValue = "0.75") Double threshold) {
        List<Party> duplicates = resolutionService.findDuplicates(threshold);
        return ResponseEntity.ok(duplicates);
    }

    /**
     * Approve merge
     */
    @PostMapping("/merge")
    public ResponseEntity<Party> approveMerge(
            @RequestParam String sourceId,
            @RequestParam String targetId,
            @RequestParam String approvedBy
    ) {
        Party merged = resolutionService.approveMerge(sourceId, targetId, approvedBy);
        return ResponseEntity.ok(merged);
    }

    /**
     * Mark as not duplicate
     */
    @PostMapping("/not-duplicate")
    public ResponseEntity<Void> markNotDuplicate(
            @RequestParam String partyId,
            @RequestParam String candidateId,
            @RequestParam String reviewedBy
    ) {
        resolutionService.markNotDuplicate(partyId, candidateId, reviewedBy);
        return ResponseEntity.ok().build();
    }

    /**
     * Get parties from multiple systems
     */
    @GetMapping("/cross-domain")
    public ResponseEntity<List<Party>> getCrossDomainParties(
            @RequestParam(defaultValue = "2") Integer minSystems
    ) {
        List<Party> parties = partyRepository.findCrossDomainParties(minSystems);
        return ResponseEntity.ok(parties);
    }
}
