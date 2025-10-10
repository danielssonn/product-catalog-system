package com.bank.product.core.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Product details retrieved from core banking system.
 * Used for drift detection and reconciliation.
 */
@Data
@Builder
public class CoreProductDetails {
    /**
     * Product ID in core system
     */
    private String coreProductId;

    /**
     * Product name in core system
     */
    private String productName;

    /**
     * Product type/class in core system
     */
    private String productType;

    /**
     * Product status (ACTIVE, INACTIVE, etc.)
     */
    private String status;

    /**
     * Monthly fee
     */
    private BigDecimal monthlyFee;

    /**
     * Annual interest rate (as decimal, e.g., 0.05 for 5%)
     */
    private BigDecimal interestRate;

    /**
     * Minimum balance requirement
     */
    private BigDecimal minimumBalance;

    /**
     * Overdraft limit (if applicable)
     */
    private BigDecimal overdraftLimit;

    /**
     * Whether overdraft is allowed
     */
    private Boolean overdraftAllowed;

    /**
     * Product features
     */
    private Map<String, Object> features;

    /**
     * Additional fees
     */
    private Map<String, BigDecimal> additionalFees;

    /**
     * Product effective date
     */
    private LocalDate effectiveDate;

    /**
     * Product end date (if sunset)
     */
    private LocalDate endDate;

    /**
     * Number of active accounts
     */
    private Integer activeAccountCount;

    /**
     * Complete raw response from core system (for debugging)
     */
    private Map<String, Object> rawResponse;
}
