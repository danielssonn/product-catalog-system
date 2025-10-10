package com.bank.commercialbanking.party.repository;

import com.bank.commercialbanking.party.domain.CommercialParty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommercialPartyRepository extends MongoRepository<CommercialParty, String> {

    Optional<CommercialParty> findByPartyId(String partyId);

    List<CommercialParty> findByTier(String tier);

    List<CommercialParty> findByRiskRating(String riskRating);

    List<CommercialParty> findByLegalNameContainingIgnoreCase(String name);

    List<CommercialParty> findByRegistrationNumber(String registrationNumber);
}
