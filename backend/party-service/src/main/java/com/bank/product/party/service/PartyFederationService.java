package com.bank.product.party.service;

import com.bank.product.party.domain.*;
import com.bank.product.party.repository.PartyRepository;
import com.bank.product.party.repository.SourceRecordRepository;
import com.bank.product.party.resolution.EntityResolutionService;
import com.bank.product.party.resolution.ResolutionResult;
import com.bank.product.party.sync.SourceSystemAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * Core service for federating party data from multiple source systems.
 * Handles synchronization, entity resolution, and data lineage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartyFederationService {

    private final PartyRepository partyRepository;
    private final SourceRecordRepository sourceRecordRepository;
    private final EntityResolutionService entityResolutionService;
    private final ConflictResolutionService conflictResolutionService;
    private final Map<String, SourceSystemAdapter> sourceSystemAdapters;

    /**
     * Sync party from source system
     */
    @Transactional
    public ResolutionResult syncFromSource(String sourceSystem, String sourceId) {
        log.info("Syncing party from source: {}, id: {}", sourceSystem, sourceId);

        // Get adapter for source system
        SourceSystemAdapter adapter = sourceSystemAdapters.get(sourceSystem);
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter found for source system: " + sourceSystem);
        }

        // Fetch data from source
        Map<String, Object> sourceData = adapter.fetchParty(sourceId);
        String checksum = calculateChecksum(sourceData);

        // Check if we already have this source record
        Optional<SourceRecord> existingSourceRecord = sourceRecordRepository
                .findBySourceSystemAndSourceId(sourceSystem, sourceId);

        if (existingSourceRecord.isPresent()) {
            SourceRecord existing = existingSourceRecord.get();

            // Check if data has changed
            if (checksum.equals(existing.getChecksum())) {
                log.info("No changes detected for {} - {}", sourceSystem, sourceId);
                // Find and return existing party
                Optional<Party> existingParty = partyRepository.findBySourceSystemAndSourceId(sourceSystem, sourceId);
                return existingParty.map(ResolutionResult::created)
                        .orElseGet(() -> processNewSourceData(sourceSystem, sourceId, sourceData, checksum));
            }

            // Data changed, update source record
            log.info("Data changed for {} - {}", sourceSystem, sourceId);
            existing.setSourceData(sourceData);
            existing.setChecksum(checksum);
            existing.incrementVersion();
            sourceRecordRepository.save(existing);

            // Update existing party
            return updateExistingParty(existing, sourceData, adapter);
        }

        // New source record
        return processNewSourceData(sourceSystem, sourceId, sourceData, checksum);
    }

    /**
     * Process new source data
     */
    private ResolutionResult processNewSourceData(
            String sourceSystem,
            String sourceId,
            Map<String, Object> sourceData,
            String checksum
    ) {
        // Create source record
        SourceRecord sourceRecord = new SourceRecord();
        sourceRecord.setSourceSystem(sourceSystem);
        sourceRecord.setSourceId(sourceId);
        sourceRecord.setSourceData(sourceData);
        sourceRecord.setChecksum(checksum);
        sourceRecord.setSyncedAt(Instant.now());
        sourceRecord.setQualityScore(getSourceQualityScore(sourceSystem));
        sourceRecordRepository.save(sourceRecord);

        // Transform to party entity
        SourceSystemAdapter adapter = sourceSystemAdapters.get(sourceSystem);
        Party party = adapter.transformToParty(sourceData);
        party.addSourceRecord(sourceRecord);

        // Run entity resolution
        ResolutionResult result = entityResolutionService.resolve(party);

        log.info("Resolution result: {}", result.getAction());
        return result;
    }

    /**
     * Update existing party with new source data
     */
    private ResolutionResult updateExistingParty(
            SourceRecord sourceRecord,
            Map<String, Object> newData,
            SourceSystemAdapter adapter
    ) {
        // Find party associated with this source record
        Optional<Party> existingPartyOpt = partyRepository.findBySourceSystemAndSourceId(
                sourceRecord.getSourceSystem(),
                sourceRecord.getSourceId()
        );

        if (existingPartyOpt.isEmpty()) {
            log.warn("No party found for source record, creating new");
            return processNewSourceData(
                    sourceRecord.getSourceSystem(),
                    sourceRecord.getSourceId(),
                    newData,
                    sourceRecord.getChecksum()
            );
        }

        Party existingParty = existingPartyOpt.get();

        // Transform new data
        Party updatedData = adapter.transformToParty(newData);

        // Resolve conflicts and merge
        Party merged = conflictResolutionService.mergeUpdates(existingParty, updatedData, sourceRecord);

        merged.markUpdated();
        Party saved = partyRepository.save(merged);

        return ResolutionResult.created(saved);
    }

    /**
     * Sync all parties from a source system (batch)
     */
    @Transactional
    public SyncResult syncAllFromSource(String sourceSystem) {
        log.info("Starting full sync from source: {}", sourceSystem);

        SourceSystemAdapter adapter = sourceSystemAdapters.get(sourceSystem);
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter found for source system: " + sourceSystem);
        }

        SyncResult result = new SyncResult();
        result.setSourceSystem(sourceSystem);
        result.setStartTime(Instant.now());

        // Fetch all party IDs from source
        java.util.List<String> partyIds = adapter.fetchAllPartyIds();
        result.setTotalRecords(partyIds.size());

        for (String partyId : partyIds) {
            try {
                ResolutionResult resolutionResult = syncFromSource(sourceSystem, partyId);
                result.incrementProcessed();

                switch (resolutionResult.getAction()) {
                    case CREATED -> result.incrementCreated();
                    case MERGED -> result.incrementMerged();
                    case NEEDS_REVIEW -> result.incrementNeedsReview();
                }
            } catch (Exception e) {
                log.error("Error syncing party {} from {}: {}", partyId, sourceSystem, e.getMessage(), e);
                result.incrementFailed();
            }
        }

        result.setEndTime(Instant.now());
        log.info("Sync completed: {}", result);

        return result;
    }

    /**
     * Create cross-domain relationship (e.g., operates on behalf of)
     * This synthesizes relationships from multiple source systems
     */
    @Transactional
    public void createCrossDomainRelationship(
            String agentId,
            String principalId,
            String relationshipType,
            Map<String, Object> properties,
            java.util.List<String> sourceSystems
    ) {
        log.info("Creating cross-domain relationship: {} from {} to {}, sources: {}",
                relationshipType, agentId, principalId, sourceSystems);

        Party agent = partyRepository.findByFederatedId(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent party not found"));
        Party principal = partyRepository.findByFederatedId(principalId)
                .orElseThrow(() -> new IllegalArgumentException("Principal party not found"));

        if (agent instanceof Organization && principal instanceof Organization) {
            OperationalRelationship rel = new OperationalRelationship();
            rel.setPrincipal((Organization) principal);
            rel.setAuthorityLevel((String) properties.get("authorityLevel"));
            rel.setScope((String) properties.get("scope"));
            rel.setSourceSystems(sourceSystems);

            ((Organization) agent).getOperatesOnBehalfOf().add(rel);
            partyRepository.save(agent);
        }
    }

    /**
     * Calculate checksum for source data
     */
    private String calculateChecksum(Map<String, Object> data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String jsonString = data.toString(); // In production, use proper JSON serialization
            byte[] hash = md.digest(jsonString.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Get quality score for a source system
     */
    private Double getSourceQualityScore(String sourceSystem) {
        return switch (sourceSystem) {
            case "COMMERCIAL_BANKING" -> 0.95;
            case "CAPITAL_MARKETS" -> 0.90;
            case "CRM" -> 0.75;
            case "KYC_SYSTEM" -> 0.98;
            default -> 0.80;
        };
    }
}
