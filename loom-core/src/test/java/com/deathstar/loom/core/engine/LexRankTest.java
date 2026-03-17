package com.deathstar.loom.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LexRankTest {

    @Test
    void testMiddleBetweenTwoValues() {
        String mid = LexRank.getMiddle("a", "c");
        assertTrue(mid.compareTo("a") > 0, "mid should be > a");
        assertTrue(mid.compareTo("c") < 0, "mid should be < c");

        String midAZ = LexRank.getMiddle("a", "z");
        assertTrue(midAZ.compareTo("a") > 0, "mid should be > a");
        assertTrue(midAZ.compareTo("z") < 0, "mid should be < z");
    }

    @Test
    void testMiddleWithNullPrev_InsertsAtTop() {
        String next = "m";
        String mid = LexRank.getMiddle(null, next);

        assertNotNull(mid);
        assertFalse(mid.isEmpty());
        // Must be lexicographically less than "m"
        assertTrue(mid.compareTo(next) < 0, "mid '" + mid + "' should be < '" + next + "'");
    }

    @Test
    void testMiddleWithNullNext_InsertsAtBottom() {
        String prev = "m";
        String mid = LexRank.getMiddle(prev, null);

        assertNotNull(mid);
        assertFalse(mid.isEmpty());
        // Must be lexicographically greater than "m"
        assertTrue(mid.compareTo(prev) > 0, "mid '" + mid + "' should be > '" + prev + "'");
    }

    @Test
    void testMiddleWithBothNull_InitList() {
        String mid = LexRank.getMiddle(null, null);
        assertNotNull(mid);
        assertFalse(mid.isEmpty());
        assertEquals("m", mid);
    }

    @Test
    void testMiddleGeneratingLongerFraction() {
        String prev = "a";
        String next = "b";
        String mid = LexRank.getMiddle(prev, next);

        assertTrue(mid.compareTo(prev) > 0, "mid '" + mid + "' should be > '" + prev + "'");
        assertTrue(mid.compareTo(next) < 0, "mid '" + mid + "' should be < '" + next + "'");

        String mid2 = LexRank.getMiddle("a", "am");
        assertTrue(mid2.compareTo("a") > 0);
        assertTrue(mid2.compareTo("am") < 0);
    }
}
