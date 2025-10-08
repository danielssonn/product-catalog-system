package com.bank.capitalmarkets.counterparty.repository;

import com.bank.capitalmarkets.counterparty.domain.Counterparty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounterpartyRepository extends MongoRepository<Counterparty, String> {

    Optional<Counterparty> findByCounterpartyId(String counterpartyId);

    Optional<Counterparty> findByLei(String lei);

    List<Counterparty> findByRiskRating(String riskRating);

    List<Counterparty> findByCounterpartyType(String counterpartyType);

    List<Counterparty> findByLegalNameContainingIgnoreCase(String name);

    List<Counterparty> findByProductTypesContaining(String productType);
}
