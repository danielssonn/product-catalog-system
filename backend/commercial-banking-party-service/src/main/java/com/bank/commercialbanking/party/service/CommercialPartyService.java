package com.bank.commercialbanking.party.service;

import com.bank.commercialbanking.party.domain.CommercialParty;
import com.bank.commercialbanking.party.repository.CommercialPartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommercialPartyService {

    private final CommercialPartyRepository repository;

    public List<CommercialParty> getAllParties() {
        return repository.findAll();
    }

    public List<String> getAllPartyIds() {
        return repository.findAll().stream()
                .map(CommercialParty::getPartyId)
                .toList();
    }

    public Optional<CommercialParty> getPartyById(String partyId) {
        return repository.findByPartyId(partyId);
    }

    public CommercialParty createParty(CommercialParty party) {
        party.setCreatedAt(LocalDateTime.now());
        party.setUpdatedAt(LocalDateTime.now());
        log.info("Creating commercial party: {}", party.getPartyId());
        return repository.save(party);
    }

    public CommercialParty updateParty(String partyId, CommercialParty party) {
        CommercialParty existing = repository.findByPartyId(partyId)
                .orElseThrow(() -> new IllegalArgumentException("Party not found: " + partyId));

        existing.setLegalName(party.getLegalName());
        existing.setBusinessName(party.getBusinessName());
        existing.setRegistrationNumber(party.getRegistrationNumber());
        existing.setJurisdiction(party.getJurisdiction());
        existing.setIncorporationDate(party.getIncorporationDate());
        existing.setIndustry(party.getIndustry());
        existing.setIndustryCode(party.getIndustryCode());
        existing.setTier(party.getTier());
        existing.setRiskRating(party.getRiskRating());
        existing.setAmlStatus(party.getAmlStatus());
        existing.setRegisteredAddress(party.getRegisteredAddress());
        existing.setMailingAddress(party.getMailingAddress());
        existing.setPrimaryContact(party.getPrimaryContact());
        existing.setPhoneNumber(party.getPhoneNumber());
        existing.setEmail(party.getEmail());
        existing.setWebsite(party.getWebsite());
        existing.setEmployeeCount(party.getEmployeeCount());
        existing.setAnnualRevenue(party.getAnnualRevenue());
        existing.setAccountManager(party.getAccountManager());
        existing.setRelationship(party.getRelationship());
        existing.setProductTypes(party.getProductTypes());
        existing.setSubsidiaries(party.getSubsidiaries());
        existing.setUpdatedAt(LocalDateTime.now());

        log.info("Updating commercial party: {}", partyId);
        return repository.save(existing);
    }

    public List<CommercialParty> searchByName(String name) {
        return repository.findByLegalNameContainingIgnoreCase(name);
    }
}
