package com.oreo.gui;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotParserTest {

    @Test
    void parsesSingleSlot() {
        assertEquals(List.of(13), SlotParser.parse("13"));
    }

    @Test
    void parsesAscendingRangeInclusive() {
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8), SlotParser.parse("0-8"));
    }

    @Test
    void parsesOffsetRange() {
        assertEquals(Arrays.asList(18, 19, 20, 21, 22, 23, 24, 25, 26), SlotParser.parse("18-26"));
    }

    @Test
    void parsesDescendingRange() {
        assertEquals(Arrays.asList(8, 7, 6, 5, 4, 3, 2, 1, 0), SlotParser.parse("8-0"));
    }

    @Test
    void parsesCommaSeparatedList() {
        assertEquals(Arrays.asList(0, 2, 4), SlotParser.parse("0,2,4"));
    }

    @Test
    void parsesMixedListAndRange() {
        assertEquals(Arrays.asList(1, 2, 3, 5), SlotParser.parse("1-3,5"));
    }

    @Test
    void toleratesWhitespaceAroundRange() {
        assertEquals(Arrays.asList(1, 2, 3), SlotParser.parse("1 - 3"));
    }

    @Test
    void nullEmptyAndInvalidYieldEmpty() {
        assertTrue(SlotParser.parse(null).isEmpty());
        assertTrue(SlotParser.parse("").isEmpty());
        assertTrue(SlotParser.parse("   ").isEmpty());
        assertTrue(SlotParser.parse("abc").isEmpty());
    }

    @Test
    void skipsInvalidTokensButKeepsValidOnes() {
        assertEquals(Arrays.asList(1, 3), SlotParser.parse("1,x,3"));
    }

    @Test
    void acceptsNumericValueObject() {
        assertEquals(List.of(7), SlotParser.parse(7));
    }
}
