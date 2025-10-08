package com.bank.product.party.repository;

import com.bank.product.party.domain.SourceRecord;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SourceRecord entities
 */
@Repository
public interface SourceRecordRepository extends Neo4jRepository<SourceRecord, String> {

    /**
     * Find source record by system and ID
     */
    Optional<SourceRecord> findBySourceSystemAndSourceId(String sourceSystem, String sourceId);

    /**
     * Find all records from a source system
     */
    List<SourceRecord> findBySourceSystem(String sourceSystem);

    /**
     * Find master source records
     */
    List<SourceRecord> findByMasterSource(Boolean masterSource);

    /**
     * Find source records by checksum (for change detection)
     */
    List<SourceRecord> findByChecksum(String checksum);

    /**
     * Find source records for a party
     */
    @Query("MATCH (p:Party {federatedId: $partyId})-[:SOURCED_FROM]->(s:SourceRecord) RETURN s ORDER BY s.syncedAt DESC")
    List<SourceRecord> findByParty(@Param("partyId") String partyId);
}
