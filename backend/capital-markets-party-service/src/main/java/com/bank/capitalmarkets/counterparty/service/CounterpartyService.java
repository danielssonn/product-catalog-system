package com.bank.capitalmarkets.counterparty.service;

import com.bank.capitalmarkets.counterparty.domain.Counterparty;
import com.bank.capitalmarkets.counterparty.repository.CounterpartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CounterpartyService {

    private final CounterpartyRepository repository;

    public List<Counterparty> getAllCounterparties() {
        return repository.findAll();
    }

    public List<String> getAllCounterpartyIds() {
        return repository.findAll().stream()
                .map(Counterparty::getCounterpartyId)
                .toList();
    }

    public Optional<Counterparty> getCounterpartyById(String counterpartyId) {
        return repository.findByCounterpartyId(counterpartyId);
    }

    public Optional<Counterparty> getCounterpartyByLei(String lei) {
        return repository.findByLei(lei);
    }

    public Counterparty createCounterparty(Counterparty counterparty) {
        counterparty.setCreatedAt(LocalDateTime.now());
        counterparty.setUpdatedAt(LocalDateTime.now());
        log.info("Creating counterparty: {}", counterparty.getCounterpartyId());
        return repository.save(counterparty);
    }

    public Counterparty updateCounterparty(String counterpartyId, Counterparty counterparty) {
        Counterparty existing = repository.findByCounterpartyId(counterpartyId)
                .orElseThrow(() -> new IllegalArgumentException("Counterparty not found: " + counterpartyId));

        existing.setLegalName(counterparty.getLegalName());
        existing.setLei(counterparty.getLei());
        existing.setJurisdiction(counterparty.getJurisdiction());
        existing.setJurisdictionCode(counterparty.getJurisdictionCode());
        existing.setRiskRating(counterparty.getRiskRating());
        existing.setInternalRating(counterparty.getInternalRating());
        existing.setExposureLimit(counterparty.getExposureLimit());
        existing.setCurrentExposure(counterparty.getCurrentExposure());
        existing.setProductTypes(counterparty.getProductTypes());
        existing.setCreditRating(counterparty.getCreditRating());
        existing.setCreditRatingAgency(counterparty.getCreditRatingAgency());
        existing.setTradingRegions(counterparty.getTradingRegions());
        existing.setCounterpartyType(counterparty.getCounterpartyType());
        existing.setRelationshipManager(counterparty.getRelationshipManager());
        existing.setSalesCoverage(counterparty.getSalesCoverage());
        existing.setProductExposures(counterparty.getProductExposures());
        existing.setIsPrimaryDealer(counterparty.getIsPrimaryDealer());
        existing.setIsQualifiedCounterparty(counterparty.getIsQualifiedCounterparty());
        existing.setSettlementInstructions(counterparty.getSettlementInstructions());
        existing.setAuthorizedTraders(counterparty.getAuthorizedTraders());
        existing.setKycStatus(counterparty.getKycStatus());
        existing.setKycExpiryDate(counterparty.getKycExpiryDate());
        existing.setSanctionsScreening(counterparty.getSanctionsScreening());
        existing.setLastScreeningDate(counterparty.getLastScreeningDate());
        existing.setUpdatedAt(LocalDateTime.now());

        log.info("Updating counterparty: {}", counterpartyId);
        return repository.save(existing);
    }

    public List<Counterparty> searchByName(String name) {
        return repository.findByLegalNameContainingIgnoreCase(name);
    }
}
