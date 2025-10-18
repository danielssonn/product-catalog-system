package com.bank.product.entitlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Constraints applied to an entitlement
 * These are conditions that must be met for the entitlement to be valid
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntitlementConstraints implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Maximum transaction amount allowed
     */
    private BigDecimal maxAmount;

    /**
     * Minimum transaction amount allowed
     */
    private BigDecimal minAmount;

    /**
     * Currency for amount limits
     */
    private String currency;

    /**
     * Daily transaction limit
     */
    private BigDecimal dailyLimit;

    /**
     * Monthly transaction limit
     */
    private BigDecimal monthlyLimit;

    /**
     * Allowed channels for this entitlement
     * Empty = all channels allowed
     */
    @Builder.Default
    private Set<String> allowedChannels = new HashSet<>();

    /**
     * Blocked channels for this entitlement
     */
    @Builder.Default
    private Set<String> blockedChannels = new HashSet<>();

    /**
     * Allowed countries for transactions
     * Empty = all countries allowed
     */
    @Builder.Default
    private Set<String> allowedCountries = new HashSet<>();

    /**
     * Blocked countries for transactions
     */
    @Builder.Default
    private Set<String> blockedCountries = new HashSet<>();

    /**
     * Allowed product types (for product catalog/solution resources)
     */
    @Builder.Default
    private Set<String> allowedProductTypes = new HashSet<>();

    /**
     * Time-based constraints: valid from
     */
    private LocalDate validFrom;

    /**
     * Time-based constraints: valid until
     */
    private LocalDate validUntil;

    /**
     * Time of day constraints: valid from (HH:mm format, e.g., "09:00")
     */
    private String validFromTime;

    /**
     * Time of day constraints: valid until (HH:mm format, e.g., "17:00")
     */
    private String validUntilTime;

    /**
     * Days of week when entitlement is valid (MONDAY, TUESDAY, etc.)
     * Empty = all days allowed
     */
    @Builder.Default
    private Set<String> allowedDaysOfWeek = new HashSet<>();

    /**
     * Requires additional approval beyond standard workflow
     */
    @Builder.Default
    private boolean requiresApproval = false;

    /**
     * Approval threshold amount (requires approval above this amount)
     */
    private BigDecimal approvalThreshold;

    /**
     * Roles required to approve (if requiresApproval=true)
     */
    @Builder.Default
    private Set<String> approverRoles = new HashSet<>();

    /**
     * Requires multi-factor authentication
     */
    @Builder.Default
    private boolean requiresMfa = false;

    /**
     * Requires specific security clearance level
     */
    private String requiredClearanceLevel;

    /**
     * IP address restrictions (CIDR notation)
     */
    @Builder.Default
    private Set<String> allowedIpRanges = new HashSet<>();

    /**
     * Geolocation restrictions (country codes)
     */
    @Builder.Default
    private Set<String> allowedGeolocations = new HashSet<>();

    /**
     * Maximum number of operations per time period
     */
    private Integer rateLimit;

    /**
     * Rate limit period in seconds
     */
    private Integer rateLimitPeriodSeconds;

    /**
     * Custom constraints as key-value pairs for extensibility
     */
    @Builder.Default
    private java.util.Map<String, String> customConstraints = new java.util.HashMap<>();

    /**
     * Check if constraints are currently valid (time-based)
     */
    public boolean isCurrentlyValid() {
        LocalDate now = LocalDate.now();

        // Check date range
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }

        if (validUntil != null && now.isAfter(validUntil)) {
            return false;
        }

        // Check day of week
        if (!allowedDaysOfWeek.isEmpty()) {
            String today = now.getDayOfWeek().name();
            if (!allowedDaysOfWeek.contains(today)) {
                return false;
            }
        }

        // TODO: Check time of day constraints (validFromTime, validUntilTime)
        // Would require comparing current time against HH:mm strings

        return true;
    }

    /**
     * Check if amount is within limits
     */
    public boolean isAmountAllowed(BigDecimal amount) {
        if (amount == null) {
            return false;
        }

        if (minAmount != null && amount.compareTo(minAmount) < 0) {
            return false;
        }

        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            return false;
        }

        return true;
    }

    /**
     * Check if channel is allowed
     */
    public boolean isChannelAllowed(String channel) {
        if (channel == null) {
            return false;
        }

        // Check blocked list first
        if (blockedChannels.contains(channel)) {
            return false;
        }

        // If allowed list is empty, all channels are allowed
        if (allowedChannels.isEmpty()) {
            return true;
        }

        return allowedChannels.contains(channel);
    }

    /**
     * Check if country is allowed
     */
    public boolean isCountryAllowed(String countryCode) {
        if (countryCode == null) {
            return false;
        }

        // Check blocked list first
        if (blockedCountries.contains(countryCode)) {
            return false;
        }

        // If allowed list is empty, all countries are allowed
        if (allowedCountries.isEmpty()) {
            return true;
        }

        return allowedCountries.contains(countryCode);
    }

    /**
     * Check if product type is allowed
     */
    public boolean isProductTypeAllowed(String productType) {
        if (productType == null) {
            return false;
        }

        // Empty set means all product types allowed
        if (allowedProductTypes.isEmpty()) {
            return true;
        }

        return allowedProductTypes.contains(productType);
    }

    /**
     * Create default constraints (no restrictions)
     */
    public static EntitlementConstraints none() {
        return EntitlementConstraints.builder()
                .requiresApproval(false)
                .requiresMfa(false)
                .build();
    }

    /**
     * Create strict constraints (requires approval, MFA, etc.)
     */
    public static EntitlementConstraints strict() {
        return EntitlementConstraints.builder()
                .requiresApproval(true)
                .requiresMfa(true)
                .maxAmount(new BigDecimal("10000"))
                .build();
    }
}
