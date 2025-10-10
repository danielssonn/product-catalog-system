package com.bank.product.party.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for all party entities in the federated party model.
 * Represents any entity (organization, legal entity, individual) that participates
 * in business relationships.
 */
@Node("Party")
@Data
@EqualsAndHashCode(of = "federatedId")
public abstract class Party {

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String federatedId;

    private PartyType partyType;
    private PartyStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Confidence score in entity resolution (0.0 - 1.0)
     * Higher score indicates higher confidence that this entity is correctly resolved
     */
    private Double confidence;

    /**
     * Source records from which this party entity was composed
     */
    @Relationship(type = "SOURCED_FROM", direction = Relationship.Direction.OUTGOING)
    private List<SourceRecord> sourceRecords = new ArrayList<>();

    /**
     * Parties that were merged into this party during entity resolution
     */
    @Relationship(type = "MERGED_FROM", direction = Relationship.Direction.OUTGOING)
    private List<PartyMerge> mergedFrom = new ArrayList<>();

    /**
     * Candidate duplicate parties that need review
     */
    @Relationship(type = "DUPLICATES", direction = Relationship.Direction.OUTGOING)
    private List<DuplicateCandidate> duplicateCandidates = new ArrayList<>();

    public Party() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = PartyStatus.ACTIVE;
    }

    public void addSourceRecord(SourceRecord sourceRecord) {
        if (this.sourceRecords == null) {
            this.sourceRecords = new ArrayList<>();
        }
        this.sourceRecords.add(sourceRecord);
    }

    public void markUpdated() {
        this.updatedAt = Instant.now();
    }

    /**
     * Get the master source record (highest priority source)
     */
    public SourceRecord getMasterSource() {
        return sourceRecords.stream()
                .filter(SourceRecord::isMasterSource)
                .findFirst()
                .orElse(null);
    }
}
