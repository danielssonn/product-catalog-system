package com.bank.product.party.repository;

import com.bank.product.party.domain.Individual;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Individual entities
 */
@Repository
public interface IndividualRepository extends Neo4jRepository<Individual, String> {

    /**
     * Find individual by full name
     */
    List<Individual> findByFullName(String fullName);

    /**
     * Find individual by national ID
     */
    Optional<Individual> findByNationalId(String nationalId);

    /**
     * Find PEPs (Politically Exposed Persons)
     */
    List<Individual> findByPepStatus(Boolean pepStatus);

    /**
     * Find individuals with beneficial ownership
     */
    @Query("MATCH (i:Individual)-[:BENEFICIAL_OWNER_OF]->() RETURN DISTINCT i")
    List<Individual> findBeneficialOwners();

    /**
     * Find entities controlled by individual
     */
    @Query("""
            MATCH (i:Individual {federatedId: $individualId})-[r:BENEFICIAL_OWNER_OF|OFFICER_OF|BOARD_MEMBER_OF]->(entity)
            RETURN entity, type(r) AS relationshipType, r
            """)
    List<Object[]> findControlledEntities(@Param("individualId") String individualId);
}
