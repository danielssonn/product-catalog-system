package com.bank.capitalmarkets.counterparty.controller;

import com.bank.capitalmarkets.counterparty.domain.Counterparty;
import com.bank.capitalmarkets.counterparty.service.CounterpartyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for Capital Markets Counterparty System.
 * This API is consumed by the federated party service.
 */
@RestController
@RequestMapping("/api/capital-markets/counterparties")
@RequiredArgsConstructor
public class CounterpartyController {

    private final CounterpartyService service;

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    /**
     * Get all counterparty IDs (for federation sync)
     */
    @GetMapping("/ids")
    public ResponseEntity<List<String>> getAllCounterpartyIds() {
        return ResponseEntity.ok(service.getAllCounterpartyIds());
    }

    /**
     * Get all counterparties
     */
    @GetMapping
    public ResponseEntity<List<Counterparty>> getAllCounterparties() {
        return ResponseEntity.ok(service.getAllCounterparties());
    }

    /**
     * Get counterparty by ID
     */
    @GetMapping("/{counterpartyId}")
    public ResponseEntity<Counterparty> getCounterparty(@PathVariable String counterpartyId) {
        return service.getCounterpartyById(counterpartyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get counterparty by LEI
     */
    @GetMapping("/lei/{lei}")
    public ResponseEntity<Counterparty> getCounterpartyByLei(@PathVariable String lei) {
        return service.getCounterpartyByLei(lei)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search counterparties by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<Counterparty>> searchCounterparties(@RequestParam String name) {
        return ResponseEntity.ok(service.searchByName(name));
    }

    /**
     * Create counterparty
     */
    @PostMapping
    public ResponseEntity<Counterparty> createCounterparty(@RequestBody Counterparty counterparty) {
        return ResponseEntity.ok(service.createCounterparty(counterparty));
    }

    /**
     * Update counterparty
     */
    @PutMapping("/{counterpartyId}")
    public ResponseEntity<Counterparty> updateCounterparty(
            @PathVariable String counterpartyId,
            @RequestBody Counterparty counterparty
    ) {
        return ResponseEntity.ok(service.updateCounterparty(counterpartyId, counterparty));
    }
}
