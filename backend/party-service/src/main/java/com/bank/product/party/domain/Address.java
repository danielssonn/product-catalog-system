package com.bank.product.party.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Address information for parties.
 * Embedded as a property rather than a separate node.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateProvince;
    private String postalCode;
    private String country;
    private String countryCode; // ISO 3166-1 alpha-2

    /**
     * Address type (REGISTERED, MAILING, OPERATING, RESIDENTIAL)
     */
    private String addressType;

    /**
     * Get normalized address string for matching
     */
    public String getNormalizedAddress() {
        StringBuilder sb = new StringBuilder();
        if (addressLine1 != null) sb.append(addressLine1.trim().toLowerCase());
        if (city != null) sb.append(" ").append(city.trim().toLowerCase());
        if (stateProvince != null) sb.append(" ").append(stateProvince.trim().toLowerCase());
        if (postalCode != null) sb.append(" ").append(postalCode.replaceAll("\\s+", ""));
        if (countryCode != null) sb.append(" ").append(countryCode.toUpperCase());
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * Calculate similarity score with another address (0.0 - 1.0)
     */
    public double similarity(Address other) {
        if (other == null) return 0.0;

        int matches = 0;
        int total = 0;

        // Postal code match is strongest signal
        if (postalCode != null && other.postalCode != null) {
            total++;
            if (normalizePostalCode(postalCode).equals(normalizePostalCode(other.postalCode))) {
                matches++;
            }
        }

        // Country code
        if (countryCode != null && other.countryCode != null) {
            total++;
            if (countryCode.equalsIgnoreCase(other.countryCode)) {
                matches++;
            }
        }

        // City
        if (city != null && other.city != null) {
            total++;
            if (city.equalsIgnoreCase(other.city)) {
                matches++;
            }
        }

        // State/Province
        if (stateProvince != null && other.stateProvince != null) {
            total++;
            if (stateProvince.equalsIgnoreCase(other.stateProvince)) {
                matches++;
            }
        }

        return total > 0 ? (double) matches / total : 0.0;
    }

    private String normalizePostalCode(String postalCode) {
        return postalCode.replaceAll("[\\s-]", "").toUpperCase();
    }
}
