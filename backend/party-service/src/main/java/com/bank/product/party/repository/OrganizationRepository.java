package com.bank.product.party.repository;

import com.bank.product.party.domain.Organization;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Organization entities
 */
@Repository
public interface OrganizationRepository extends Neo4jRepository<Organization, String> {

    /**
     * Find organization by LEI
     */
    Optional<Organization> findByLei(String lei);

    /**
     * Find organization by legal name (exact match)
     */
    Optional<Organization> findByLegalName(String legalName);

    /**
     * Search organizations by name (case-insensitive)
     */
    @Query("MATCH (o:Organization) WHERE toLower(o.name) CONTAINS toLower($name) OR toLower(o.legalName) CONTAINS toLower($name) RETURN o")
    List<Organization> searchByName(@Param("name") String name);

    /**
     * Find organization hierarchy (all subsidiaries)
     */
    @Query("""
            MATCH path = (parent:Organization {federatedId: $parentId})-[:PARENT_OF*]->(subsidiary)
            RETURN parent, collect(relationships(path)), collect(subsidiary)
            """)
    Organization findHierarchy(@Param("parentId") String parentId);

    /**
     * Find ultimate parent organization
     */
    @Query("""
            MATCH path = (child:Organization {federatedId: $childId})-[:SUBSIDIARY_OF*]->(parent:Organization)
            WHERE NOT (parent)-[:SUBSIDIARY_OF]->()
            RETURN parent
            ORDER BY length(path) DESC
            LIMIT 1
            """)
    Optional<Organization> findUltimateParent(@Param("childId") String childId);

    /**
     * Find all subsidiaries at any depth
     */
    @Query("""
            MATCH (parent:Organization {federatedId: $parentId})-[:PARENT_OF*]->(subsidiary)
            RETURN subsidiary,
                   size((parent)-[:PARENT_OF*]->(subsidiary)) AS depth
            ORDER BY depth
            """)
    List<Organization> findAllSubsidiaries(@Param("parentId") String parentId);

    /**
     * Find organizations with "operates on behalf of" relationships
     */
    @Query("""
            MATCH (agent:Organization)-[r:OPERATES_ON_BEHALF_OF]->(principal:Organization)
            WHERE $sourceSystem IN r.sourceSystems
              AND r.validFrom <= date() <= coalesce(r.validTo, date())
            RETURN agent, collect(r), collect(principal)
            """)
    List<Organization> findOperatesOnBehalfOf(@Param("sourceSystem") String sourceSystem);

    /**
     * Find relationship path between two organizations
     */
    @Query("""
            MATCH path = shortestPath(
              (org1:Organization {federatedId: $org1Id})-[*1..5]-(org2:Organization {federatedId: $org2Id})
            )
            RETURN path,
                   [r in relationships(path) | type(r)] AS relationshipTypes,
                   length(path) AS hops
            """)
    List<Object[]> findRelationshipPath(@Param("org1Id") String org1Id, @Param("org2Id") String org2Id);

    /**
     * Get consolidated risk rating across hierarchy
     */
    @Query("""
            MATCH (parent:Organization {federatedId: $parentId})-[:PARENT_OF*]->(subsidiary)
            WITH parent,
                 collect(subsidiary.riskRating) AS riskRatings,
                 count(subsidiary) AS totalSubsidiaries
            RETURN parent.name AS name,
                   totalSubsidiaries,
                   riskRatings,
                   CASE
                     WHEN 'HIGH' IN riskRatings THEN 'HIGH'
                     WHEN 'MEDIUM' IN riskRatings THEN 'MEDIUM'
                     ELSE 'LOW'
                   END AS consolidatedRisk
            """)
    Object getConsolidatedRisk(@Param("parentId") String parentId);
}
