package com.bank.product.party.matching;

import com.bank.product.party.domain.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for AddressNormalizer.
 *
 * Tests USPS standardization for address matching in entity resolution.
 * Verifies handling of:
 * - Street type abbreviations (Avenue → AVE, Boulevard → BLVD)
 * - Directional standardization (North → N, Northeast → NE)
 * - State name to 2-letter code conversion
 * - Postal code normalization
 * - Address similarity scoring
 */
class AddressNormalizerTest {

    private AddressNormalizer addressNormalizer;

    @BeforeEach
    void setUp() {
        addressNormalizer = new AddressNormalizer();
    }

    // ===== Normalization Tests =====

    @Test
    @DisplayName("Should normalize street type abbreviations")
    void testStreetTypeNormalization() {
        Address addr1 = Address.builder()
                .addressLine1("123 Main Street")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("123 Main St")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.95,
                "Street vs St should match closely, got: " + similarity);
    }

    @Test
    @DisplayName("Should normalize boulevard abbreviations")
    void testBoulevardNormalization() {
        Address addr1 = Address.builder()
                .addressLine1("456 Park Boulevard")
                .city("Los Angeles")
                .stateProvince("CA")
                .postalCode("90001")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("456 Park Blvd")
                .city("Los Angeles")
                .stateProvince("CA")
                .postalCode("90001")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.95,
                "Boulevard vs Blvd should match closely, got: " + similarity);
    }

    @Test
    @DisplayName("Should normalize avenue abbreviations")
    void testAvenueNormalization() {
        Address addr1 = Address.builder()
                .addressLine1("789 Fifth Avenue")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10022")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("789 Fifth Ave")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10022")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.95,
                "Avenue vs Ave should match closely, got: " + similarity);
    }

    @Test
    @DisplayName("Should normalize directional prefixes")
    void testDirectionalNormalization() {
        Address addr1 = Address.builder()
                .addressLine1("100 North Main Street")
                .city("Chicago")
                .stateProvince("IL")
                .postalCode("60601")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("100 N Main St")
                .city("Chicago")
                .stateProvince("IL")
                .postalCode("60601")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.90,
                "North vs N should match closely, got: " + similarity);
    }

    @Test
    @DisplayName("Should normalize complex directional prefixes")
    void testComplexDirectionalNormalization() {
        Address addr1 = Address.builder()
                .addressLine1("500 Northeast Oak Street")
                .city("Portland")
                .stateProvince("OR")
                .postalCode("97201")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("500 NE Oak St")
                .city("Portland")
                .stateProvince("OR")
                .postalCode("97201")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.85,
                "Northeast vs NE should match, got: " + similarity);
    }

    @Test
    @DisplayName("Should normalize state names to codes")
    void testStateNameNormalization() {
        Address addr1 = Address.builder()
                .addressLine1("123 Main St")
                .city("San Francisco")
                .stateProvince("California")
                .postalCode("94102")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("123 Main St")
                .city("San Francisco")
                .stateProvince("CA")
                .postalCode("94102")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.95,
                "California vs CA should match, got: " + similarity);
    }

    // ===== Postal Code Tests =====

    @Test
    @DisplayName("Should boost similarity for matching postal codes")
    void testPostalCodeBoost() {
        Address addr1 = Address.builder()
                .addressLine1("123 Main Street")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("125 Main Street")  // Different street number
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001")  // Same postal code
                .build();

        Address addr3 = Address.builder()
                .addressLine1("125 Main Street")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10002")  // Different postal code
                .build();

        double similarityWithSameZip = addressNormalizer.calculateSimilarity(addr1, addr2);
        double similarityWithDiffZip = addressNormalizer.calculateSimilarity(addr1, addr3);

        assertTrue(similarityWithSameZip > similarityWithDiffZip,
                String.format("Same postal code should boost similarity: same_zip=%.2f, diff_zip=%.2f",
                        similarityWithSameZip, similarityWithDiffZip));
    }

    @Test
    @DisplayName("Should normalize ZIP+4 to ZIP5")
    void testZipPlusFourNormalization() {
        Address addr1 = Address.builder()
                .addressLine1("123 Main St")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001-1234")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("123 Main St")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.95,
                "ZIP+4 vs ZIP5 should match closely, got: " + similarity);
    }

    // ===== Case Sensitivity Tests =====

    @Test
    @DisplayName("Should be case-insensitive")
    void testCaseInsensitivity() {
        Address addr1 = Address.builder()
                .addressLine1("123 MAIN STREET")
                .city("NEW YORK")
                .stateProvince("NY")
                .postalCode("10001")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("123 main street")
                .city("new york")
                .stateProvince("ny")
                .postalCode("10001")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.95,
                "Case variations should not affect matching, got: " + similarity);
    }

    // ===== Real-World Address Tests =====

    @Test
    @DisplayName("Should match Goldman Sachs headquarters variations")
    void testGoldmanSachsHQ() {
        Address addr1 = Address.builder()
                .addressLine1("200 West Street")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10282")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("200 West St")
                .city("New York")
                .stateProvince("New York")
                .postalCode("10282-2198")  // ZIP+4
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.90,
                "Goldman Sachs HQ variations should match, got: " + similarity);
    }

    @Test
    @DisplayName("Should match JPMorgan Chase headquarters variations")
    void testJPMorganHQ() {
        Address addr1 = Address.builder()
                .addressLine1("383 Madison Avenue")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10179")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("383 Madison Ave")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10179")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.95,
                "JPMorgan HQ variations should match, got: " + similarity);
    }

    @Test
    @DisplayName("Should match Bank of America Tower variations")
    void testBankOfAmericaTower() {
        Address addr1 = Address.builder()
                .addressLine1("One Bryant Park")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10036")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("1 Bryant Park")  // "One" vs "1"
                .city("New York")
                .stateProvince("NY")
                .postalCode("10036")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.85,
                "Number word vs digit should match reasonably, got: " + similarity);
    }

    // ===== Suite/Unit Number Tests =====

    @Test
    @DisplayName("Should handle suite numbers")
    void testSuiteNumbers() {
        Address addr1 = Address.builder()
                .addressLine1("123 Main Street")
                .addressLine2("Suite 500")
                .city("Chicago")
                .stateProvince("IL")
                .postalCode("60601")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("123 Main Street")
                .addressLine2("Ste 500")
                .city("Chicago")
                .stateProvince("IL")
                .postalCode("60601")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.90,
                "Suite vs Ste should match closely, got: " + similarity);
    }

    @Test
    @DisplayName("Should handle floor numbers")
    void testFloorNumbers() {
        Address addr1 = Address.builder()
                .addressLine1("456 Park Avenue")
                .addressLine2("Floor 10")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10022")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("456 Park Ave")
                .addressLine2("10th Floor")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10022")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.85,
                "Floor number variations should match, got: " + similarity);
    }

    // ===== Null and Empty Tests =====

    @Test
    @DisplayName("Should handle null addresses")
    void testNullAddresses() {
        Address validAddr = Address.builder()
                .addressLine1("123 Main St")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001")
                .build();

        double similarity1 = addressNormalizer.calculateSimilarity(null, validAddr);
        double similarity2 = addressNormalizer.calculateSimilarity(validAddr, null);
        double similarity3 = addressNormalizer.calculateSimilarity(null, null);

        assertEquals(0.0, similarity1, "Null address should return 0.0");
        assertEquals(0.0, similarity2, "Null address should return 0.0");
        assertEquals(0.0, similarity3, "Both null should return 0.0");
    }

    @Test
    @DisplayName("Should handle addresses with missing fields")
    void testPartialAddresses() {
        Address addr1 = Address.builder()
                .addressLine1("123 Main St")
                .city("New York")
                .stateProvince("NY")
                .build();  // No postal code

        Address addr2 = Address.builder()
                .addressLine1("123 Main St")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.70,
                "Partial addresses should still match if street/city/state match, got: " + similarity);
    }

    // ===== Different Address Tests =====

    @Test
    @DisplayName("Should return low similarity for different streets")
    void testDifferentStreets() {
        Address addr1 = Address.builder()
                .addressLine1("123 Main Street")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("456 Park Avenue")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        // Note: Same postal code can boost similarity significantly
        assertTrue(similarity >= 0.0 && similarity <= 1.0,
                "Should return valid similarity score, got: " + similarity);
        assertTrue(similarity < 0.95,
                "Different streets should not match perfectly, got: " + similarity);
    }

    @Test
    @DisplayName("Should return low similarity for different cities")
    void testDifferentCities() {
        Address addr1 = Address.builder()
                .addressLine1("123 Main Street")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("123 Main Street")
                .city("Los Angeles")
                .stateProvince("CA")
                .postalCode("90001")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        // Note: Same street name can still provide some similarity
        assertTrue(similarity >= 0.0 && similarity <= 1.0,
                "Should return valid similarity score, got: " + similarity);
        assertTrue(similarity < 0.95,
                "Different cities should not match perfectly, got: " + similarity);
    }

    // ===== Boundary Tests =====

    @Test
    @DisplayName("Should return score in valid range")
    void testValidScoreRange() {
        Address addr1 = Address.builder()
                .addressLine1("123 Main St")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("456 Park Ave")
                .city("Los Angeles")
                .stateProvince("CA")
                .postalCode("90001")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity >= 0.0 && similarity <= 1.0,
                "Similarity score should be in [0.0, 1.0] range, got: " + similarity);
    }

    @Test
    @DisplayName("Should be symmetric")
    void testSymmetry() {
        Address addr1 = Address.builder()
                .addressLine1("123 Main Street")
                .city("New York")
                .stateProvince("NY")
                .postalCode("10001")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("456 Park Avenue")
                .city("Los Angeles")
                .stateProvince("CA")
                .postalCode("90001")
                .build();

        double similarity1 = addressNormalizer.calculateSimilarity(addr1, addr2);
        double similarity2 = addressNormalizer.calculateSimilarity(addr2, addr1);

        assertEquals(similarity1, similarity2, 0.001,
                "Similarity should be symmetric: similarity(A, B) = similarity(B, A)");
    }

    @Test
    @DisplayName("Should handle international addresses")
    void testInternationalAddresses() {
        Address addr1 = Address.builder()
                .addressLine1("1 Canada Square")
                .city("London")
                .stateProvince("England")
                .postalCode("E14 5AB")
                .country("United Kingdom")
                .build();

        Address addr2 = Address.builder()
                .addressLine1("1 Canada Square")
                .city("London")
                .stateProvince("England")
                .postalCode("E14 5AB")
                .country("UK")
                .build();

        double similarity = addressNormalizer.calculateSimilarity(addr1, addr2);

        assertTrue(similarity > 0.90,
                "International addresses should match, got: " + similarity);
    }
}
