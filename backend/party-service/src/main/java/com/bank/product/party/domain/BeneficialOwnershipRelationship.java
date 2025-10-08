package com.bank.product.party.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents beneficial ownership relationship.
 * Tracks ultimate beneficial owners (UBOs) per FinCEN and AML regulations.
 */
@RelationshipProperties
@Data
public class BeneficialOwnershipRelationship {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private Party target;

    /**
     * Ownership percentage (direct + indirect)
     */
    private Double ownershipPercentage;

    /**
     * Level of control (DIRECT, INDIRECT, ULTIMATE)
     */
    private String controlLevel;

    /**
     * Whether this is an Ultimate Beneficial Owner (>25% threshold)
     */
    private Boolean ubo;

    /**
     * Verification date for KYC/AML compliance
     */
    private LocalDate verificationDate;

    /**
     * Verification status
     */
    private String verificationStatus;

    /**
     * Source of information
     */
    private List<String> sourceSystems = new ArrayList<>();

    /**
     * Path description (e.g., "owns 60% of Company A, which owns 50% of target")
     */
    private String ownershipPath;

    /**
     * Number of intermediate entities in ownership chain
     */
    private Integer chainLength;

    /**
     * Confidence score in this relationship
     */
    private Double confidence;

    public void addSourceSystem(String sourceSystem) {
        if (this.sourceSystems == null) {
            this.sourceSystems = new ArrayList<>();
        }
        if (!this.sourceSystems.contains(sourceSystem)) {
            this.sourceSystems.add(sourceSystem);
        }
    }

    /**
     * Check if this meets UBO threshold (>25% per FinCEN)
     */
    public boolean meetsUboThreshold() {
        return ownershipPercentage != null && ownershipPercentage >= 25.0;
    }

    /**
     * Check if verification is current (within last 12 months)
     */
    public boolean isVerificationCurrent() {
        if (verificationDate == null) return false;
        return verificationDate.isAfter(LocalDate.now().minusMonths(12));
    }

    /**
     * Check if this is a UBO
     */
    public boolean isUbo() {
        return Boolean.TRUE.equals(ubo);
    }
}
