package com.bank.product.party.repository;

import com.bank.product.party.domain.LegalEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for LegalEntity entities
 */
@Repository
public interface LegalEntityRepository extends Neo4jRepository<LegalEntity, String> {

    /**
     * Find legal entity by LEI
     */
    Optional<LegalEntity> findByLei(String lei);

    /**
     * Find legal entity by tax ID and jurisdiction
     */
    Optional<LegalEntity> findByTaxIdAndJurisdiction(String taxId, String jurisdiction);

    /**
     * Find legal entity by registration number and jurisdiction
     */
    Optional<LegalEntity> findByRegistrationNumberAndJurisdiction(String registrationNumber, String jurisdiction);

    /**
     * Find legal entities by parent organization
     */
    @Query("MATCH (le:LegalEntity)-[:BELONGS_TO]->(org:Organization {federatedId: $orgId}) RETURN le")
    List<LegalEntity> findByParentOrganization(@Param("orgId") String orgId);

    /**
     * Find ultimate beneficial owners (>25% threshold)
     */
    @Query("""
            MATCH path = (owner:Individual)-[:BENEFICIAL_OWNER_OF*]->(entity:LegalEntity {federatedId: $entityId})
            WHERE ALL(r IN relationships(path) WHERE r.ownershipPercentage >= 25.0)
            WITH owner,
                 reduce(ownership = 1.0, r IN relationships(path) | ownership * r.ownershipPercentage) AS effectiveOwnership
            WHERE effectiveOwnership >= 25.0
            RETURN owner, effectiveOwnership
            ORDER BY effectiveOwnership DESC
            """)
    List<Object[]> findUltimateBeneficialOwners(@Param("entityId") String entityId);

    /**
     * Find ownership chain
     */
    @Query("""
            MATCH path = (le:LegalEntity {federatedId: $entityId})<-[:OWNS*]-(owner)
            RETURN path,
                   [r IN relationships(path) | r.ownershipPercentage] AS percentages
            ORDER BY length(path)
            """)
    List<Object[]> findOwnershipChain(@Param("entityId") String entityId);
}
