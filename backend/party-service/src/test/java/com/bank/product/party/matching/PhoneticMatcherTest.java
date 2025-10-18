package com.bank.product.party.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for PhoneticMatcher.
 *
 * Tests phonetic similarity algorithms for name matching in entity resolution.
 * Verifies handling of:
 * - Name variations (JPMorgan vs J.P. Morgan)
 * - Punctuation differences
 * - Spelling variations
 * - Common prefix matching (Jaro-Winkler)
 */
class PhoneticMatcherTest {

    private PhoneticMatcher phoneticMatcher;

    @BeforeEach
    void setUp() {
        phoneticMatcher = new PhoneticMatcher();
    }

    // ===== Phonetic Similarity Tests =====

    @Test
    @DisplayName("Should return 1.0 for identical strings")
    void testIdenticalStrings() {
        double similarity = phoneticMatcher.calculatePhoneticSimilarity(
                "Goldman Sachs", "Goldman Sachs"
        );

        assertEquals(1.0, similarity, 0.01,
                "Identical strings should have perfect similarity");
    }

    @Test
    @DisplayName("Should handle JPMorgan variations")
    void testJPMorganVariations() {
        // Test common variations of JPMorgan Chase
        double similarity1 = phoneticMatcher.calculatePhoneticSimilarity(
                "JPMorgan Chase", "J.P. Morgan Chase"
        );

        double similarity2 = phoneticMatcher.calculatePhoneticSimilarity(
                "JPMorgan", "JP Morgan"
        );

        assertTrue(similarity1 > 0.85,
                "JPMorgan vs J.P. Morgan should have high similarity, got: " + similarity1);
        assertTrue(similarity2 > 0.85,
                "JPMorgan vs JP Morgan should have high similarity, got: " + similarity2);
    }

    @Test
    @DisplayName("Should handle punctuation variations")
    void testPunctuationVariations() {
        double similarity = phoneticMatcher.calculatePhoneticSimilarity(
                "Wells Fargo & Company", "Wells Fargo and Company"
        );

        assertTrue(similarity > 0.80,
                "Punctuation variations should have high similarity, got: " + similarity);
    }

    @Test
    @DisplayName("Should handle corporate suffix variations")
    void testCorporateSuffixVariations() {
        double similarity1 = phoneticMatcher.calculatePhoneticSimilarity(
                "Microsoft Corporation", "Microsoft Corp"
        );

        double similarity2 = phoneticMatcher.calculatePhoneticSimilarity(
                "Apple Inc", "Apple Incorporated"
        );

        assertTrue(similarity1 > 0.85,
                "Corporation vs Corp should have high similarity, got: " + similarity1);
        assertTrue(similarity2 > 0.80,
                "Inc vs Incorporated should have high similarity, got: " + similarity2);
    }

    @Test
    @DisplayName("Should handle LLC variations")
    void testLLCVariations() {
        double similarity = phoneticMatcher.calculatePhoneticSimilarity(
                "ABC Company LLC", "ABC Company Limited Liability Company"
        );

        assertTrue(similarity > 0.75,
                "LLC vs Limited Liability Company should have reasonable similarity, got: " + similarity);
    }

    @Test
    @DisplayName("Should return low similarity for different companies")
    void testDifferentCompanies() {
        double similarity = phoneticMatcher.calculatePhoneticSimilarity(
                "Goldman Sachs", "Morgan Stanley"
        );

        assertTrue(similarity < 0.50,
                "Different companies should have low similarity, got: " + similarity);
    }

    @Test
    @DisplayName("Should handle null inputs gracefully")
    void testNullInputs() {
        double similarity1 = phoneticMatcher.calculatePhoneticSimilarity(null, "Test");
        double similarity2 = phoneticMatcher.calculatePhoneticSimilarity("Test", null);
        double similarity3 = phoneticMatcher.calculatePhoneticSimilarity(null, null);

        assertEquals(0.0, similarity1, "Null input should return 0.0");
        assertEquals(0.0, similarity2, "Null input should return 0.0");
        assertEquals(0.0, similarity3, "Both null should return 0.0");
    }

    @Test
    @DisplayName("Should handle empty strings")
    void testEmptyStrings() {
        double similarity1 = phoneticMatcher.calculatePhoneticSimilarity("", "Test");
        double similarity2 = phoneticMatcher.calculatePhoneticSimilarity("Test", "");
        double similarity3 = phoneticMatcher.calculatePhoneticSimilarity("", "");

        assertEquals(0.0, similarity1, "Empty string should return 0.0");
        assertEquals(0.0, similarity2, "Empty string should return 0.0");
        assertEquals(0.0, similarity3, "Both empty should return 0.0");
    }

    // ===== Jaro-Winkler Similarity Tests =====

    @Test
    @DisplayName("Should return 1.0 for identical strings (Jaro-Winkler)")
    void testJaroWinklerIdentical() {
        double similarity = phoneticMatcher.jaroWinklerSimilarity(
                "Goldman Sachs", "Goldman Sachs"
        );

        assertEquals(1.0, similarity, 0.01,
                "Identical strings should have perfect Jaro-Winkler similarity");
    }

    @Test
    @DisplayName("Jaro-Winkler should favor common prefixes")
    void testJaroWinklerCommonPrefix() {
        // Jaro-Winkler gives higher scores for strings with common prefixes
        double similarity1 = phoneticMatcher.jaroWinklerSimilarity(
                "Goldman Sachs International", "Goldman Sachs USA"
        );

        double similarity2 = phoneticMatcher.jaroWinklerSimilarity(
                "Goldman Sachs", "Sachs Goldman"  // Same words, different order
        );

        assertTrue(similarity1 > similarity2,
                "Common prefix should score higher than different order: " +
                        "prefix=" + similarity1 + ", different order=" + similarity2);
    }

    @Test
    @DisplayName("Should handle case sensitivity")
    void testCaseSensitivity() {
        double similarity1 = phoneticMatcher.calculatePhoneticSimilarity(
                "GOLDMAN SACHS", "goldman sachs"
        );

        double similarity2 = phoneticMatcher.jaroWinklerSimilarity(
                "GOLDMAN SACHS", "goldman sachs"
        );

        assertTrue(similarity1 > 0.90,
                "Phonetic matcher should be case-insensitive, got: " + similarity1);
        assertTrue(similarity2 > 0.90,
                "Jaro-Winkler should handle case differences, got: " + similarity2);
    }

    // ===== Real-World Test Cases =====

    @Test
    @DisplayName("Should match Bank of America variations")
    void testBankOfAmericaVariations() {
        double similarity1 = phoneticMatcher.calculatePhoneticSimilarity(
                "Bank of America Corporation", "Bank of America Corp"
        );

        double similarity2 = phoneticMatcher.calculatePhoneticSimilarity(
                "Bank of America", "BofA"
        );

        assertTrue(similarity1 > 0.90,
                "Bank of America Corporation vs Corp should match closely, got: " + similarity1);

        // BofA is abbreviation, might not match perfectly phonetically
        assertTrue(similarity2 < 0.80,
                "Abbreviations may not match phonetically, got: " + similarity2);
    }

    @Test
    @DisplayName("Should match Citigroup variations")
    void testCitigroupVariations() {
        double similarity1 = phoneticMatcher.calculatePhoneticSimilarity(
                "Citigroup Inc", "Citigroup Incorporated"
        );

        double similarity2 = phoneticMatcher.calculatePhoneticSimilarity(
                "Citigroup", "Citi"
        );

        assertTrue(similarity1 > 0.90,
                "Citigroup Inc vs Incorporated should match, got: " + similarity1);
        assertTrue(similarity2 > 0.70,
                "Citigroup vs Citi should have reasonable match, got: " + similarity2);
    }

    @Test
    @DisplayName("Should handle international company names")
    void testInternationalNames() {
        double similarity1 = phoneticMatcher.calculatePhoneticSimilarity(
                "Deutsche Bank AG", "Deutsche Bank Aktiengesellschaft"
        );

        double similarity2 = phoneticMatcher.calculatePhoneticSimilarity(
                "HSBC Holdings plc", "HSBC Holdings Public Limited Company"
        );

        assertTrue(similarity1 > 0.80,
                "Deutsche Bank variations should match, got: " + similarity1);
        assertTrue(similarity2 > 0.85,
                "HSBC plc variations should match, got: " + similarity2);
    }

    @Test
    @DisplayName("Should handle subsidiary name patterns")
    void testSubsidiaryPatterns() {
        double similarity = phoneticMatcher.calculatePhoneticSimilarity(
                "Goldman Sachs Bank USA", "Goldman Sachs International"
        );

        assertTrue(similarity > 0.70,
                "Subsidiaries with common parent should have reasonable similarity, got: " + similarity);
    }

    // ===== Edge Cases =====

    @Test
    @DisplayName("Should handle very short strings")
    void testVeryShortStrings() {
        double similarity1 = phoneticMatcher.calculatePhoneticSimilarity("AB", "AC");
        double similarity2 = phoneticMatcher.calculatePhoneticSimilarity("X", "Y");

        assertTrue(similarity1 >= 0.0 && similarity1 <= 1.0,
                "Short strings should return valid similarity score");
        assertTrue(similarity2 >= 0.0 && similarity2 <= 1.0,
                "Single characters should return valid similarity score");
    }

    @Test
    @DisplayName("Should handle very long strings")
    void testVeryLongStrings() {
        String long1 = "The Very Long Corporate Name of a Multinational Banking and Financial Services Corporation";
        String long2 = "The Very Long Corporate Name of a Multinational Banking and Financial Services Corp";

        double similarity = phoneticMatcher.calculatePhoneticSimilarity(long1, long2);

        assertTrue(similarity > 0.85,
                "Long strings with minor differences should match closely, got: " + similarity);
    }

    @Test
    @DisplayName("Should handle special characters")
    void testSpecialCharacters() {
        double similarity1 = phoneticMatcher.calculatePhoneticSimilarity(
                "AT&T Inc", "AT and T Incorporated"
        );

        double similarity2 = phoneticMatcher.calculatePhoneticSimilarity(
                "Berkshire Hathaway Inc.", "Berkshire Hathaway Inc"
        );

        assertTrue(similarity1 > 0.75,
                "Special characters should be handled, got: " + similarity1);
        assertTrue(similarity2 > 0.95,
                "Trailing punctuation should not affect similarity, got: " + similarity2);
    }

    @Test
    @DisplayName("Should handle numeric variations")
    void testNumericVariations() {
        double similarity = phoneticMatcher.calculatePhoneticSimilarity(
                "3M Company", "Three M Company"
        );

        // Phonetic matching may not handle number-word conversion well
        assertTrue(similarity >= 0.0 && similarity <= 1.0,
                "Numeric variations should return valid score");
    }

    // ===== Performance and Boundary Tests =====

    @Test
    @DisplayName("Should return score in valid range")
    void testValidScoreRange() {
        String[] testCases = {
                "Apple", "Banana",
                "Microsoft", "Apple",
                "Goldman Sachs", "Morgan Stanley",
                "JPMorgan", "Chase",
                "Bank of America", "Citigroup"
        };

        for (int i = 0; i < testCases.length - 1; i += 2) {
            double similarity = phoneticMatcher.calculatePhoneticSimilarity(
                    testCases[i], testCases[i + 1]
            );

            assertTrue(similarity >= 0.0 && similarity <= 1.0,
                    String.format("Similarity score should be in [0.0, 1.0] range for '%s' vs '%s', got: %.2f",
                            testCases[i], testCases[i + 1], similarity));
        }
    }

    @Test
    @DisplayName("Should be symmetric")
    void testSymmetry() {
        String str1 = "Goldman Sachs";
        String str2 = "J.P. Morgan";

        double similarity1 = phoneticMatcher.calculatePhoneticSimilarity(str1, str2);
        double similarity2 = phoneticMatcher.calculatePhoneticSimilarity(str2, str1);

        assertEquals(similarity1, similarity2, 0.001,
                "Similarity should be symmetric: similarity(A, B) = similarity(B, A)");
    }
}
