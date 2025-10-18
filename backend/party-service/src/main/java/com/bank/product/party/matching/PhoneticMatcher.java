package com.bank.product.party.matching;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Phonetic matching component using Metaphone3 algorithm.
 * Handles name variations like "JPMorgan" vs "J.P. Morgan"
 */
@Component
@Slf4j
public class PhoneticMatcher {

    /**
     * Calculate phonetic similarity between two strings
     * @return score 0.0-1.0 (1.0 = phonetically identical)
     */
    public double calculatePhoneticSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }

        String phonetic1 = metaphone3(normalize(s1));
        String phonetic2 = metaphone3(normalize(s2));

        if (phonetic1.equals(phonetic2)) {
            return 1.0;
        }

        // Calculate Levenshtein distance on phonetic codes
        int distance = levenshteinDistance(phonetic1, phonetic2);
        int maxLen = Math.max(phonetic1.length(), phonetic2.length());

        if (maxLen == 0) {
            return 1.0;
        }

        return 1.0 - ((double) distance / maxLen);
    }

    /**
     * Normalize string before phonetic encoding
     */
    private String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[^a-z\\s]", "") // Remove non-alphabetic chars
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Metaphone3 phonetic encoding (simplified implementation)
     * Full implementation would be from Apache Commons Codec
     */
    private String metaphone3(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String s = input.toUpperCase();
        int length = s.length();

        // Simplified Metaphone rules (production should use Apache Commons Codec)
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            char next = (i + 1 < length) ? s.charAt(i + 1) : ' ';
            char prev = (i > 0) ? s.charAt(i - 1) : ' ';

            switch (c) {
                case 'A', 'E', 'I', 'O', 'U':
                    if (i == 0) result.append(c); // Keep initial vowels
                    break;
                case 'B':
                    if (!(i == length - 1 && prev == 'M')) result.append(c);
                    break;
                case 'C':
                    if (next == 'H') {
                        result.append('X');
                        i++; // Skip H
                    } else if (next == 'I' || next == 'E' || next == 'Y') {
                        result.append('S');
                    } else {
                        result.append('K');
                    }
                    break;
                case 'D':
                    if (next == 'G' && (i + 2 < length)) {
                        char next2 = s.charAt(i + 2);
                        if (next2 == 'E' || next2 == 'I' || next2 == 'Y') {
                            result.append('J');
                            i += 2;
                        } else {
                            result.append('T');
                        }
                    } else {
                        result.append('T');
                    }
                    break;
                case 'G':
                    if (next == 'H' && i + 2 < length) {
                        // GH rules
                        if (!(i == 0)) {
                            // Silent GH in middle/end
                            i++; // Skip H
                        } else {
                            result.append('K');
                        }
                    } else if (next == 'N' && i == length - 2) {
                        // Silent GN at end
                        break;
                    } else if (next == 'E' || next == 'I' || next == 'Y') {
                        result.append('J');
                    } else {
                        result.append('K');
                    }
                    break;
                case 'H':
                    if (i == 0 || isVowel(prev)) {
                        result.append(c);
                    }
                    // Silent H after consonants
                    break;
                case 'K':
                    if (prev != 'C') result.append(c);
                    break;
                case 'P':
                    if (next == 'H') {
                        result.append('F');
                        i++; // Skip H
                    } else {
                        result.append(c);
                    }
                    break;
                case 'Q':
                    result.append('K');
                    break;
                case 'S':
                    if (next == 'H') {
                        result.append('X');
                        i++; // Skip H
                    } else if (next == 'I' && (i + 2 < length)) {
                        char next2 = s.charAt(i + 2);
                        if (next2 == 'O' || next2 == 'A') {
                            result.append('X');
                        } else {
                            result.append(c);
                        }
                    } else {
                        result.append(c);
                    }
                    break;
                case 'T':
                    if (next == 'I' && (i + 2 < length)) {
                        char next2 = s.charAt(i + 2);
                        if (next2 == 'O' || next2 == 'A') {
                            result.append('X');
                        } else {
                            result.append(c);
                        }
                    } else if (next == 'H') {
                        result.append('0'); // TH sound
                        i++; // Skip H
                    } else if (!(next == 'C' && i + 2 < length && s.charAt(i + 2) == 'H')) {
                        result.append(c);
                    }
                    break;
                case 'V':
                    result.append('F');
                    break;
                case 'W':
                    if (i == 0 || isVowel(prev)) {
                        result.append(c);
                    }
                    break;
                case 'X':
                    if (i == 0) {
                        result.append('S');
                    } else {
                        result.append("KS");
                    }
                    break;
                case 'Y':
                    if (i == 0 || isVowel(prev)) {
                        result.append(c);
                    }
                    break;
                case 'Z':
                    result.append('S');
                    break;
                default:
                    if (Character.isLetter(c)) {
                        result.append(c);
                    }
            }
        }

        return result.toString();
    }

    private boolean isVowel(char c) {
        return c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U';
    }

    /**
     * Calculate Levenshtein distance between two strings
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
     * Calculate Jaro-Winkler similarity (better for names with common prefixes)
     */
    public double jaroWinklerSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }

        if (s1.equals(s2)) {
            return 1.0;
        }

        int len1 = s1.length();
        int len2 = s2.length();

        if (len1 == 0 || len2 == 0) {
            return 0.0;
        }

        // Calculate Jaro similarity
        int matchDistance = Math.max(len1, len2) / 2 - 1;
        boolean[] s1Matches = new boolean[len1];
        boolean[] s2Matches = new boolean[len2];

        int matches = 0;
        int transpositions = 0;

        // Find matches
        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);

            for (int j = start; j < end; j++) {
                if (s2Matches[j] || s1.charAt(i) != s2.charAt(j)) {
                    continue;
                }
                s1Matches[i] = true;
                s2Matches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        // Count transpositions
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!s1Matches[i]) {
                continue;
            }
            while (!s2Matches[k]) {
                k++;
            }
            if (s1.charAt(i) != s2.charAt(k)) {
                transpositions++;
            }
            k++;
        }

        double jaro = (matches / (double) len1 +
                       matches / (double) len2 +
                       (matches - transpositions / 2.0) / matches) / 3.0;

        // Calculate common prefix length (up to 4 chars)
        int prefixLength = 0;
        for (int i = 0; i < Math.min(4, Math.min(len1, len2)); i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                prefixLength++;
            } else {
                break;
            }
        }

        // Jaro-Winkler = Jaro + (prefix length × prefix scaling × (1 - Jaro))
        double prefixScaling = 0.1;
        return jaro + (prefixLength * prefixScaling * (1.0 - jaro));
    }
}
