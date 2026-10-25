package com.oreo.util;

import net.md_5.bungee.api.ChatColor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:((?:#[A-Fa-f0-9]{6}:?)+)>([^<]+)</gradient>");
    private static final Pattern RAINBOW_PATTERN = Pattern.compile("<rainbow>([^<]+)</rainbow>");
    private static final Pattern LEGACY_AMP_HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String color(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        message = processGradients(message);

        message = processRainbow(message);

        message = processHex(message);

        message = ChatColor.translateAlternateColorCodes('&', message);

        return message;
    }

    private static String processHex(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            try {
                ChatColor hexColor = ChatColor.of("#" + hexCode);
                matcher.appendReplacement(buffer, hexColor.toString());
            } catch (Exception e) {
                matcher.appendReplacement(buffer, matcher.group(0));
            }
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String processGradients(String message) {
        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String colorsStr = matcher.group(1);
            String text = matcher.group(2);

            String[] hexColors = colorsStr.split(":");
            List<String> colorList = new ArrayList<>();
            for (String hex : hexColors) {
                if (hex != null && !hex.trim().isEmpty()) {
                    colorList.add(hex.trim());
                }
            }

            String gradientText;
            if (colorList.size() >= 2) {
                gradientText = applyMultiGradient(text, colorList);
            } else if (colorList.size() == 1) {
                try {
                    ChatColor color = ChatColor.of(colorList.get(0));
                    gradientText = color + text;
                } catch (Exception e) {
                    gradientText = text;
                }
            } else {
                gradientText = text;
            }

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(gradientText));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String processRainbow(String message) {
        Matcher matcher = RAINBOW_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String text = matcher.group(1);
            String rainbowText = applyRainbow(text);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(rainbowText));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String applyMultiGradient(String text, List<String> hexColors) {

        List<Color> colors = new ArrayList<>();
        for (String hex : hexColors) {
            Color color = hexToColor(hex);
            if (color != null) {
                colors.add(color);
            }
        }

        if (colors.isEmpty()) {
            return text;
        }

        if (colors.size() == 1) {
            try {
                ChatColor chatColor = ChatColor.of(colors.get(0));
                return chatColor + text;
            } catch (Exception e) {
                return text;
            }
        }

        StringBuilder result = new StringBuilder();
        int length = text.length();
        int numColors = colors.size();

        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);

            double position = (double) i / Math.max(1, length - 1);

            double segmentSize = 1.0 / (numColors - 1);
            int segmentIndex = (int) (position / segmentSize);

            if (segmentIndex >= numColors - 1) {
                segmentIndex = numColors - 2;
            }

            double segmentPosition = (position - (segmentIndex * segmentSize)) / segmentSize;

            Color startColor = colors.get(segmentIndex);
            Color endColor = colors.get(segmentIndex + 1);
            Color interpolated = interpolateColor(startColor, endColor, segmentPosition);

            try {
                ChatColor chatColor = ChatColor.of(interpolated);
                result.append(chatColor).append(c);
            } catch (Exception e) {
                result.append(c);
