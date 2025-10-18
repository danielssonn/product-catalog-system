package com.bank.product.party.matching;

import com.bank.product.party.domain.Address;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Address normalization component for fuzzy address matching.
 * Handles abbreviations, formatting, and standardization.
 */
@Component
@Slf4j
public class AddressNormalizer {

    private static final Map<String, String> STREET_TYPE_ABBREV = new HashMap<>();
    private static final Map<String, String> DIRECTIONAL_ABBREV = new HashMap<>();
    private static final Map<String, String> UNIT_TYPE_ABBREV = new HashMap<>();

    static {
        // Street type abbreviations (USPS standard)
        STREET_TYPE_ABBREV.put("ALLEY", "ALY");
        STREET_TYPE_ABBREV.put("AVENUE", "AVE");
        STREET_TYPE_ABBREV.put("BOULEVARD", "BLVD");
        STREET_TYPE_ABBREV.put("CIRCLE", "CIR");
        STREET_TYPE_ABBREV.put("COURT", "CT");
        STREET_TYPE_ABBREV.put("DRIVE", "DR");
        STREET_TYPE_ABBREV.put("EXPRESSWAY", "EXPY");
        STREET_TYPE_ABBREV.put("HIGHWAY", "HWY");
        STREET_TYPE_ABBREV.put("LANE", "LN");
        STREET_TYPE_ABBREV.put("PARKWAY", "PKWY");
        STREET_TYPE_ABBREV.put("PLACE", "PL");
        STREET_TYPE_ABBREV.put("ROAD", "RD");
        STREET_TYPE_ABBREV.put("SQUARE", "SQ");
        STREET_TYPE_ABBREV.put("STREET", "ST");
        STREET_TYPE_ABBREV.put("TERRACE", "TER");
        STREET_TYPE_ABBREV.put("TRAIL", "TRL");
        STREET_TYPE_ABBREV.put("WAY", "WAY");

        // Directional abbreviations
        DIRECTIONAL_ABBREV.put("NORTH", "N");
        DIRECTIONAL_ABBREV.put("SOUTH", "S");
        DIRECTIONAL_ABBREV.put("EAST", "E");
        DIRECTIONAL_ABBREV.put("WEST", "W");
        DIRECTIONAL_ABBREV.put("NORTHEAST", "NE");
        DIRECTIONAL_ABBREV.put("NORTHWEST", "NW");
        DIRECTIONAL_ABBREV.put("SOUTHEAST", "SE");
        DIRECTIONAL_ABBREV.put("SOUTHWEST", "SW");

        // Unit type abbreviations
        UNIT_TYPE_ABBREV.put("APARTMENT", "APT");
        UNIT_TYPE_ABBREV.put("BUILDING", "BLDG");
        UNIT_TYPE_ABBREV.put("FLOOR", "FL");
        UNIT_TYPE_ABBREV.put("SUITE", "STE");
        UNIT_TYPE_ABBREV.put("UNIT", "UNIT");
        UNIT_TYPE_ABBREV.put("ROOM", "RM");
    }

    /**
     * Normalize an address to a standard format
     */
    public String normalize(Address address) {
        if (address == null) {
            return "";
        }

        StringBuilder normalized = new StringBuilder();

        // Address Line 1
        if (address.getAddressLine1() != null) {
            normalized.append(normalizeStreetAddress(address.getAddressLine1()));
        }

        // Address Line 2 (optional)
        if (address.getAddressLine2() != null) {
            if (normalized.length() > 0) normalized.append(" ");
            normalized.append(normalizeStreetAddress(address.getAddressLine2()));
        }

        // City
        if (address.getCity() != null) {
            if (normalized.length() > 0) normalized.append(", ");
            normalized.append(normalizeCity(address.getCity()));
        }

        // State/Province
        if (address.getStateProvince() != null) {
            if (normalized.length() > 0) normalized.append(", ");
            normalized.append(normalizeState(address.getStateProvince()));
        }

        // Postal Code
        if (address.getPostalCode() != null) {
            if (normalized.length() > 0) normalized.append(" ");
            normalized.append(normalizePostalCode(address.getPostalCode()));
        }

        // Country Code
        if (address.getCountryCode() != null) {
            if (normalized.length() > 0) normalized.append(", ");
            normalized.append(address.getCountryCode().toUpperCase());
        }

        return normalized.toString().toUpperCase();
    }

    /**
     * Normalize street address component
     */
    private String normalizeStreetAddress(String streetAddress) {
        String normalized = streetAddress.toUpperCase().trim();

        // Remove extra whitespace
        normalized = normalized.replaceAll("\\s+", " ");

        // Remove periods
        normalized = normalized.replace(".", "");

        // Expand/standardize street types
        for (Map.Entry<String, String> entry : STREET_TYPE_ABBREV.entrySet()) {
            String pattern = "\\b" + entry.getKey() + "\\b";
            normalized = normalized.replaceAll(pattern, entry.getValue());
            // Also handle abbreviated versions
            normalized = normalized.replaceAll("\\b" + entry.getValue() + "\\.?\\b", entry.getValue());
        }

        // Standardize directionals
        for (Map.Entry<String, String> entry : DIRECTIONAL_ABBREV.entrySet()) {
            String pattern = "\\b" + entry.getKey() + "\\b";
            normalized = normalized.replaceAll(pattern, entry.getValue());
        }

        // Standardize unit types
        for (Map.Entry<String, String> entry : UNIT_TYPE_ABBREV.entrySet()) {
            String pattern = "\\b" + entry.getKey() + "\\b";
            normalized = normalized.replaceAll(pattern, entry.getValue());
        }

        // Standardize number ordinals (1ST, 2ND, 3RD, etc.)
        normalized = normalized.replaceAll("(\\d+)(ST|ND|RD|TH)", "$1");

        return normalized.trim();
    }

    /**
     * Normalize city name
     */
    private String normalizeCity(String city) {
        return city.toUpperCase()
                .trim()
                .replaceAll("\\s+", " ")
                .replace(".", "");
    }

    /**
     * Normalize state/province (convert to 2-letter code if possible)
     */
    private String normalizeState(String state) {
        if (state == null) {
            return "";
        }

        String normalized = state.toUpperCase().trim();

        // If already 2-letter code, return as-is
        if (normalized.length() == 2) {
            return normalized;
        }

        // Map full state names to abbreviations (US states)
        Map<String, String> stateMap = Map.ofEntries(
            Map.entry("NEW YORK", "NY"),
            Map.entry("CALIFORNIA", "CA"),
            Map.entry("TEXAS", "TX"),
            Map.entry("FLORIDA", "FL"),
            Map.entry("ILLINOIS", "IL"),
            Map.entry("PENNSYLVANIA", "PA"),
            Map.entry("OHIO", "OH"),
            Map.entry("GEORGIA", "GA"),
            Map.entry("NORTH CAROLINA", "NC"),
            Map.entry("MICHIGAN", "MI"),
            Map.entry("NEW JERSEY", "NJ"),
            Map.entry("VIRGINIA", "VA"),
            Map.entry("WASHINGTON", "WA"),
            Map.entry("ARIZONA", "AZ"),
            Map.entry("MASSACHUSETTS", "MA"),
            Map.entry("TENNESSEE", "TN"),
            Map.entry("INDIANA", "IN"),
            Map.entry("MISSOURI", "MO"),
            Map.entry("MARYLAND", "MD"),
            Map.entry("WISCONSIN", "WI"),
            Map.entry("COLORADO", "CO"),
            Map.entry("MINNESOTA", "MN"),
            Map.entry("SOUTH CAROLINA", "SC"),
            Map.entry("ALABAMA", "AL"),
            Map.entry("LOUISIANA", "LA"),
            Map.entry("KENTUCKY", "KY"),
            Map.entry("OREGON", "OR"),
            Map.entry("OKLAHOMA", "OK"),
            Map.entry("CONNECTICUT", "CT"),
            Map.entry("UTAH", "UT"),
            Map.entry("IOWA", "IA"),
            Map.entry("NEVADA", "NV"),
            Map.entry("ARKANSAS", "AR"),
            Map.entry("MISSISSIPPI", "MS"),
            Map.entry("KANSAS", "KS"),
            Map.entry("NEW MEXICO", "NM"),
            Map.entry("NEBRASKA", "NE"),
            Map.entry("WEST VIRGINIA", "WV"),
            Map.entry("IDAHO", "ID"),
            Map.entry("HAWAII", "HI"),
            Map.entry("NEW HAMPSHIRE", "NH"),
            Map.entry("MAINE", "ME"),
            Map.entry("MONTANA", "MT"),
            Map.entry("RHODE ISLAND", "RI"),
            Map.entry("DELAWARE", "DE"),
            Map.entry("SOUTH DAKOTA", "SD"),
            Map.entry("NORTH DAKOTA", "ND"),
            Map.entry("ALASKA", "AK"),
            Map.entry("VERMONT", "VT"),
            Map.entry("WYOMING", "WY")
        );

        return stateMap.getOrDefault(normalized, normalized);
    }

    /**
     * Normalize postal code (remove spaces, hyphens for comparison)
     */
    private String normalizePostalCode(String postalCode) {
        if (postalCode == null) {
            return "";
        }

        // Remove spaces and hyphens
        String normalized = postalCode.replaceAll("[\\s-]", "").toUpperCase();

        // For US ZIP+4, keep only first 5 digits
        if (normalized.length() > 5 && normalized.matches("\\d{9}")) {
            normalized = normalized.substring(0, 5);
        }

        return normalized;
    }

    /**
     * Calculate similarity between two addresses
     * @return score 0.0-1.0 (1.0 = identical)
     */
    public double calculateSimilarity(Address addr1, Address addr2) {
        if (addr1 == null || addr2 == null) {
            return 0.0;
        }

        String norm1 = normalize(addr1);
        String norm2 = normalize(addr2);

        if (norm1.equals(norm2)) {
            return 1.0;
        }

        // Calculate Levenshtein distance on normalized addresses
        int distance = levenshteinDistance(norm1, norm2);
        int maxLen = Math.max(norm1.length(), norm2.length());

        if (maxLen == 0) {
            return 1.0;
        }

        double similarity = 1.0 - ((double) distance / maxLen);

        // Boost score if postal codes match exactly (strong signal)
        if (addr1.getPostalCode() != null && addr2.getPostalCode() != null) {
            String zip1 = normalizePostalCode(addr1.getPostalCode());
            String zip2 = normalizePostalCode(addr2.getPostalCode());
            if (zip1.equals(zip2)) {
                similarity = Math.min(1.0, similarity + 0.15); // Boost by 0.15
            }
        }

        return similarity;
    }

    /**
     * Calculate Levenshtein distance
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Calculate haversine distance between two addresses (if geocoded)
     * @return distance in meters, or Double.MAX_VALUE if geocoding not available
     *
     * Note: Address class does not currently have latitude/longitude fields.
     * This method is a placeholder for future geocoding integration.
     * In production, integrate with:
     * - Google Maps Geocoding API
     * - OpenStreetMap Nominatim
     * - Smarty Streets (USPS data)
     *
     * Implementation steps:
     * 1. Add latitude/longitude fields to Address class
     * 2. Geocode addresses on ingestion (async batch job)
     * 3. Store geocoded coordinates in Neo4j
     * 4. Use this method for distance-based matching
     */
    public double calculateDistance(Address addr1, Address addr2) {
        // TODO: Implement geocoding integration
        // For now, return MAX_VALUE (no distance matching)
        log.debug("Geocoding not yet implemented for addresses: {} and {}",
            addr1.getAddressLine1(), addr2.getAddressLine1());
        return Double.MAX_VALUE;

        /* Future implementation with geocoding:
        if (addr1.getLatitude() == null || addr1.getLongitude() == null ||
            addr2.getLatitude() == null || addr2.getLongitude() == null) {
            return Double.MAX_VALUE; // No geocoding available
        }

        double lat1 = addr1.getLatitude();
        double lon1 = addr1.getLongitude();
        double lat2 = addr2.getLatitude();
        double lon2 = addr2.getLongitude();

        // Haversine formula
        double earthRadius = 6371000; // meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c; // Distance in meters
        */
    }
}
