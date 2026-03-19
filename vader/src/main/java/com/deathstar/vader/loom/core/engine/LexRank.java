package com.deathstar.vader.loom.core.engine;

/**
 * Infinite split-point sorting logic (Lexical Ranking) to support custom ordering without
 * re-indexing entire lists. Provides string ranks like 'a', 'b', and midpoints like 'am'.
 */
public class LexRank {

    private static final int DEFAULT_RADIX = 36; // 0-9a-z

    /**
     * Calculates the midpoint between two lexical ranks.
     *
     * @param prev The lower rank (can be null for top-of-list)
     * @param next The higher rank (can be null for bottom-of-list)
     * @return The middle string rank.
     */
    public static String getMiddle(String prev, String next) {
        if (prev == null && next == null) {
            return "m"; // Default middle 'm' (base 36)
        }

        if (prev == null) {
            return getMiddleOfStr(null, next);
        }

        if (next == null) {
            return getMiddleOfStr(prev, null);
        }

        return getMiddleOfStr(prev, next);
    }

    // A simplified illustrative core logic for LexRank midpoint calculation:
    private static String getMiddleOfStr(String prev, String next) {
        if (prev == null) {
            StringBuilder zeroes = new StringBuilder();
            for (int i = 0; i < next.length(); i++) zeroes.append('0');
            prev = zeroes.toString();
        }
        if (next == null) return prev + "z";

        int pLen = prev.length();
        int nLen = next.length();
        int maxLen = Math.max(pLen, nLen);

        StringBuilder result = new StringBuilder();
        boolean passedDiff = false;

        for (int i = 0; i < maxLen || !passedDiff; i++) {
            char pChar = i < pLen ? prev.charAt(i) : '0';
            char nChar = i < nLen ? next.charAt(i) : 'z';

            if (pChar == nChar) {
                result.append(pChar);
            } else {
                int midValue =
                        (Character.digit(pChar, DEFAULT_RADIX)
                                        + Character.digit(nChar, DEFAULT_RADIX))
                                / 2;
                result.append(Character.forDigit(midValue, DEFAULT_RADIX));
                passedDiff = true;
                break;
            }
        }

        if (result.toString().equals(prev)) {
            result.append("m"); // Appending to split further down
        }

        return result.toString();
    }
}
