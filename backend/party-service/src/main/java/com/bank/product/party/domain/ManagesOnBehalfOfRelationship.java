package com.bank.product.party.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a "manages on behalf of" relationship between parties
 * Example: Goldman Sachs manages assets on behalf of Tesla
 *
 * The manager party provides services (asset management, custody, trading, etc.)
 * on behalf of the principal party.
 */
@RelationshipProperties
@Data
public class ManagesOnBehalfOfRelationship {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * The party being managed (the principal/client)
     */
    @TargetNode
    private Party principal;

    /**
     * Type of management service
     */
    private ManagementType managementType;

    /**
     * Scope of the management relationship
     */
    private String scope;

    /**
     * Authority level granted to the manager
     */
    private AuthorityLevel authorityLevel;

    /**
     * Start date of the relationship
     */
    private LocalDate startDate;

    /**
     * End date of the relationship (if applicable)
     */
    private LocalDate endDate;

    /**
     * Current status
     */
    private RelationshipStatus status;

    /**
     * Supporting collateral documents
     */
    private List<String> collateralDocumentIds = new ArrayList<>();

    /**
     * Services provided under this relationship
     */
    private List<String> servicesProvided = new ArrayList<>();

    /**
     * Assets under management (if applicable)
     */
    private Double assetsUnderManagement;

    /**
     * Currency for AUM
     */
    private String aumCurrency;

    /**
     * Fee structure
     */
    private String feeStructure;

    /**
     * Relationship manager
     */
    private String relationshipManager;

    /**
     * Contact person at principal
     */
    private String principalContact;

    /**
     * Contact person at manager
     */
    private String managerContact;

    /**
     * Notification requirements
     */
    private String notificationRequirements;

    /**
     * Reporting frequency
     */
    private String reportingFrequency;

    /**
     * Review date for relationship
     */
    private LocalDate reviewDate;

    /**
     * Created timestamp
     */
    private Instant createdAt;

    /**
     * Last updated timestamp
     */
    private Instant updatedAt;

    /**
     * Created by user
     */
    private String createdBy;

    /**
     * Notes or special instructions
     */
    private String notes;

    public ManagesOnBehalfOfRelationship() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = RelationshipStatus.PENDING;
        this.authorityLevel = AuthorityLevel.LIMITED;
    }

    public enum ManagementType {
        ASSET_MANAGEMENT("Asset Management"),
        CUSTODY_SERVICES("Custody Services"),
        TRADING_AUTHORITY("Trading Authority"),
        TREASURY_MANAGEMENT("Treasury Management"),
        COLLATERAL_MANAGEMENT("Collateral Management"),
        INVESTMENT_ADVISORY("Investment Advisory"),
        FIDUCIARY_SERVICES("Fiduciary Services"),
        PORTFOLIO_MANAGEMENT("Portfolio Management"),
        ESCROW_SERVICES("Escrow Services"),
        OTHER("Other");

        private final String displayName;

        ManagementType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum AuthorityLevel {
        LIMITED("Limited - Specific transactions only"),
        DISCRETIONARY("Discretionary - Within guidelines"),
        FULL("Full - Complete authority"),
        ADVISORY("Advisory - Recommendations only");

        private final String description;

        AuthorityLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum RelationshipStatus {
        PENDING("Pending Approval"),
        ACTIVE("Active"),
        SUSPENDED("Suspended"),
        TERMINATED("Terminated"),
        EXPIRED("Expired");

        private final String displayName;

        RelationshipStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public boolean isActive() {
        if (status != RelationshipStatus.ACTIVE) {
            return false;
        }

        LocalDate now = LocalDate.now();

        if (startDate != null && now.isBefore(startDate)) {
            return false;
        }

        if (endDate != null && now.isAfter(endDate)) {
            return false;
        }

        return true;
    }

    public void addCollateralDocument(String documentId) {
        if (!this.collateralDocumentIds.contains(documentId)) {
            this.collateralDocumentIds.add(documentId);
            this.updatedAt = Instant.now();
        }
    }

    public void addService(String service) {
        if (!this.servicesProvided.contains(service)) {
            this.servicesProvided.add(service);
            this.updatedAt = Instant.now();
        }
    }

    public void updateTimestamp() {
        this.updatedAt = Instant.now();
    }
}
