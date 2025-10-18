package com.bank.product.party.repository;

import com.bank.product.party.domain.Party;
import com.bank.product.party.domain.PartyStatus;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Party entities in Neo4j
 */
@Repository
public interface PartyRepository extends Neo4jRepository<Party, String> {

    /**
     * Find party by federated ID
     */
    Optional<Party> findByFederatedId(String federatedId);

    /**
     * Find active parties
     */
    List<Party> findByStatus(PartyStatus status);

    /**
     * Find organization by legal name (case-insensitive)
     * Used for entity reference resolution
     */
    @Query("MATCH (o:Organization) WHERE toLower(o.legalName) = toLower($legalName) RETURN o")
    List<Party> findByLegalNameIgnoreCase(@Param("legalName") String legalName);

    /**
     * Find parties sourced from a specific source system
     */
    @Query("MATCH (p:Party)-[:SOURCED_FROM]->(s:SourceRecord {sourceSystem: $sourceSystem}) RETURN p")
    List<Party> findBySourceSystem(@Param("sourceSystem") String sourceSystem);

    /**
     * Find parties sourced from a specific source ID
     */
    @Query("MATCH (p:Party)-[:SOURCED_FROM]->(s:SourceRecord {sourceSystem: $sourceSystem, sourceId: $sourceId}) RETURN p")
    Optional<Party> findBySourceSystemAndSourceId(
            @Param("sourceSystem") String sourceSystem,
            @Param("sourceId") String sourceId
    );

    /**
     * Find duplicate candidates above threshold
     */
    @Query("""
            MATCH (p1:Party)-[d:DUPLICATES]->(p2:Party)
            WHERE p1.status = 'ACTIVE' AND p2.status = 'ACTIVE' AND d.similarityScore >= $threshold
            RETURN p1, collect(d), collect(p2)
            ORDER BY d.similarityScore DESC
            """)
    List<Party> findDuplicateCandidates(@Param("threshold") Double threshold);

    /**
     * Find parties that exist in multiple source systems
     */
    @Query("""
            MATCH (p:Party)-[:SOURCED_FROM]->(s:SourceRecord)
            WITH p, collect(DISTINCT s.sourceSystem) AS systems
            WHERE size(systems) >= $minSystems
            RETURN p, systems
            """)
    List<Party> findCrossDomainParties(@Param("minSystems") Integer minSystems);

    /**
     * Find organization that employs an individual (for tenant resolution)
     */
    @Query("""
            MATCH (ind:Individual {federatedId: $individualId})-[:EMPLOYED_BY]->(org:Organization)
            WHERE org.status = 'ACTIVE'
            RETURN org
            LIMIT 1
            """)
    Optional<Party> findEmployerOrganization(@Param("individualId") String individualId);

    /**
     * Find parent organization for a legal entity (for tenant resolution)
     */
    @Query("""
            MATCH (le:LegalEntity {federatedId: $legalEntityId})<-[:HAS_LEGAL_ENTITY]-(org:Organization)
            WHERE org.status = 'ACTIVE'
            RETURN org
            LIMIT 1
            """)
    Optional<Party> findParentOrganization(@Param("legalEntityId") String legalEntityId);

    /**
     * Delete party by federated ID
     */
    void deleteByFederatedId(String federatedId);
}
