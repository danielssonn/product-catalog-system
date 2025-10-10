package com.bank.commercialbanking.party.controller;

import com.bank.commercialbanking.party.domain.CommercialParty;
import com.bank.commercialbanking.party.service.CommercialPartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Commercial Banking Party System.
 * This API is consumed by the federated party service.
 */
@RestController
@RequestMapping("/api/commercial-banking/parties")
@RequiredArgsConstructor
public class CommercialPartyController {

    private final CommercialPartyService service;

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * Get all party IDs (for federation sync)
     */
    @GetMapping("/ids")
    public ResponseEntity<List<String>> getAllPartyIds() {
        return ResponseEntity.ok(service.getAllPartyIds());
    }

    /**
     * Get all parties
     */
    @GetMapping
    public ResponseEntity<List<CommercialParty>> getAllParties() {
        return ResponseEntity.ok(service.getAllParties());
    }

    /**
     * Get party by ID
     */
    @GetMapping("/{partyId}")
    public ResponseEntity<CommercialParty> getParty(@PathVariable String partyId) {
        return service.getPartyById(partyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search parties by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<CommercialParty>> searchParties(@RequestParam String name) {
        return ResponseEntity.ok(service.searchByName(name));
    }

    /**
     * Create party
     */
    @PostMapping
    public ResponseEntity<CommercialParty> createParty(@RequestBody CommercialParty party) {
        return ResponseEntity.ok(service.createParty(party));
    }

    /**
     * Update party
     */
    @PutMapping("/{partyId}")
    public ResponseEntity<CommercialParty> updateParty(
            @PathVariable String partyId,
            @RequestBody CommercialParty party
    ) {
        return ResponseEntity.ok(service.updateParty(partyId, party));
    }
}
