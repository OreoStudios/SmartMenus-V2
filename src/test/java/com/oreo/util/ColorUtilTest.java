package com.oreo.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ColorUtilTest {

    @Test
    void colorIsNullAndEmptySafe() {
        assertNull(ColorUtil.color((String) null));
        assertEquals("", ColorUtil.color(""));
    }

    @Test
    void colorTranslatesAmpersandCodes() {
        assertEquals("§aHello", ColorUtil.color("&aHello"));
    }

    @Test
    void stripColorRemovesTranslatedCodes() {
        assertEquals("Hello", ColorUtil.stripColor(ColorUtil.color("&aHello")));
    }

    @Test
    void stripColorRemovesHexCodes() {
        assertEquals("Red", ColorUtil.stripColor("&#ff0000Red"));
    }

    @Test
    void stripColorRemovesGradientTags() {
        assertEquals("Hi", ColorUtil.stripColor("<gradient:#ff0000:#00ff00>Hi</gradient>"));
    }

    @Test
    void stripColorRemovesRainbowTags() {
        assertEquals("Yo", ColorUtil.stripColor("<rainbow>Yo</rainbow>"));
    }

    @Test
    void colorListColorsEveryLine() {
        List<String> input = Arrays.asList("&aOne", "&bTwo");
        List<String> result = ColorUtil.colorList(input);
        assertEquals(Arrays.asList("§aOne", "§bTwo"), result);
    }

    @Test
    void stripColorLeavesPlainTextUnchanged() {
        assertEquals("just text", ColorUtil.stripColor("just text"));
        assertFalse(ColorUtil.stripColor("&#123456abc").contains("&#"));
    }
}
