package com.bank.product.party.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents raw data from a source system.
 * Tracks data lineage and provenance.
 */
@Node("SourceRecord")
@Data
public class SourceRecord {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    private String id;

    /**
     * Source system identifier (COMMERCIAL_BANKING, CAPITAL_MARKETS, etc.)
     */
    private String sourceSystem;

    /**
     * ID of the entity in the source system
     */
    private String sourceId;

    /**
     * Full payload from source system (stored as JSON string)
     */
    private String sourceDataJson;

    /**
     * When this record was last synchronized
     */
    private Instant syncedAt;

    /**
     * Version number (incremented on each update)
     */
    private Integer version;

    /**
     * Checksum of source data (for change detection)
     */
    private String checksum;

    /**
     * Whether this is the master/authoritative source for this party
     */
    private Boolean masterSource;

    /**
     * Quality score of this source system (0.0 - 1.0)
     */
    private Double qualityScore;

    /**
     * Field-level quality scores (stored as JSON string)
     */
    private String fieldQualityScoresJson;

    public SourceRecord() {
        this.syncedAt = Instant.now();
        this.version = 1;
        this.masterSource = false;
        this.qualityScore = 0.8; // default
    }

    public boolean isMasterSource() {
        return Boolean.TRUE.equals(masterSource);
    }

    public void incrementVersion() {
        this.version = (this.version == null ? 0 : this.version) + 1;
        this.syncedAt = Instant.now();
    }

    // Helper methods for sourceData
    public void setSourceData(Map<String, Object> sourceData) {
        try {
            this.sourceDataJson = sourceData == null || sourceData.isEmpty() ? null : objectMapper.writeValueAsString(sourceData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize sourceData", e);
        }
    }

    public Map<String, Object> getSourceData() {
        try {
            return sourceDataJson == null || sourceDataJson.isEmpty() ? new HashMap<>() : objectMapper.readValue(sourceDataJson, new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    // Helper methods for fieldQualityScores
    public void setFieldQualityScores(Map<String, Double> scores) {
        try {
            this.fieldQualityScoresJson = scores == null || scores.isEmpty() ? null : objectMapper.writeValueAsString(scores);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize fieldQualityScores", e);
        }
    }

    public Map<String, Double> getFieldQualityScores() {
        try {
            return fieldQualityScoresJson == null || fieldQualityScoresJson.isEmpty() ? new HashMap<>() : objectMapper.readValue(fieldQualityScoresJson, new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public void setFieldQuality(String field, Double score) {
        Map<String, Double> scores = getFieldQualityScores();
        scores.put(field, score);
        setFieldQualityScores(scores);
    }

    public Double getFieldQuality(String field) {
        Map<String, Double> scores = getFieldQualityScores();
        return scores.getOrDefault(field, qualityScore);
    }
}
