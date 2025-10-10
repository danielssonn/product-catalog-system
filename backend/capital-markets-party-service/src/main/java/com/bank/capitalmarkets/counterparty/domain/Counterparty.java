package com.bank.capitalmarkets.counterparty.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Capital Markets Counterparty entity.
 * Represents trading counterparties in the capital markets system.
 */
@Document(collection = "counterparties")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Counterparty {

    @Id
    private String id;

    @Indexed(unique = true)
    private String counterpartyId; // CM-001, CM-002, etc.

    private String legalName;

    @Indexed
    private String lei; // Legal Entity Identifier (ISO 17442)

    private String jurisdiction;
    private String jurisdictionCode; // ISO 3166-1

    private String riskRating; // AAA, AA, A, BBB, BB, B, CCC, CC, C, D
    private String internalRating; // Internal risk assessment

    private Double exposureLimit; // in millions USD
    private Double currentExposure; // in millions USD

    @Builder.Default
    private List<String> productTypes = new ArrayList<>(); // DERIVATIVES, FX, FIXED_INCOME, EQUITY, COMMODITIES

    private String creditRating; // External credit rating
    private String creditRatingAgency; // S&P, Moody's, Fitch

    @Builder.Default
    private List<String> tradingRegions = new ArrayList<>(); // AMERICAS, EMEA, APAC

    private String counterpartyType; // BANK, HEDGE_FUND, ASSET_MANAGER, CORPORATE, SOVEREIGN

    private String relationshipManager;
    private String salesCoverage;

    @Builder.Default
    private Map<String, Double> productExposures = new HashMap<>(); // Product -> Exposure amount

    private Boolean isPrimaryDealer;
    private Boolean isQualifiedCounterparty;

    private String settlementInstructions;

    @Builder.Default
    private List<String> authorizedTraders = new ArrayList<>();

    // Compliance flags
    private String kycStatus; // APPROVED, PENDING, EXPIRED
    private LocalDateTime kycExpiryDate;
    private String sanctionsScreening; // CLEAR, FLAGGED
    private LocalDateTime lastScreeningDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
