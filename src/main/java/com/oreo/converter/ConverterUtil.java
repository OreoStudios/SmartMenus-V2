package com.oreo.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ConverterUtil {

    private static final Pattern MINI_HEX = Pattern.compile("<#([0-9a-fA-F]{6})>");

    public static String convertColors(String input) {
        if (input == null) return null;

        StringBuilder sb = new StringBuilder();
        int i = 0, len = input.length();
        while (i < len) {
            char c = input.charAt(i);

            if ((c == '§' || c == '&') && i + 1 < len
                    && (input.charAt(i + 1) == 'x' || input.charAt(i + 1) == 'X')) {
                char prefix = c;
                int j = i + 2;
                char[] hex = new char[6];
                boolean valid = true;
                for (int k = 0; k < 6; k++) {
                    if (j + 1 >= len || input.charAt(j) != prefix || !isHexChar(input.charAt(j + 1))) {
                        valid = false;
                        break;
                    }
                    hex[k] = input.charAt(j + 1);
                    j += 2;
                }
                if (valid) {
                    sb.append("&#").append(hex);
                    i = j;
                    continue;
                }
            }

            if (c == '§') { sb.append('&'); i++; continue; }
            sb.append(c);
            i++;
        }

        return MINI_HEX.matcher(sb.toString()).replaceAll(m -> "&#" + m.group(1));
    }

    public static List<String> convertColors(List<String> lines) {
        List<String> out = new ArrayList<>(lines.size());
        for (String l : lines) out.add(convertColors(l));
        return out;
    }

    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    public static List<Integer> expandSlots(List<?> slotList) {
        List<Integer> result = new ArrayList<>();
        for (Object entry : slotList) {
            String s = String.valueOf(entry).trim();
            if (s.contains("-")) {
                String[] parts = s.split("-", 2);
                try {
                    int from = Integer.parseInt(parts[0].trim());
                    int to   = Integer.parseInt(parts[1].trim());
                    for (int n = from; n <= to; n++) result.add(n);
                } catch (NumberFormatException ignored) {}
            } else {
                try { result.add(Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }

    public static String normalizeMaterial(String mat) {
        if (mat == null) return "STONE";
        String m = mat.trim();
        if (m.toLowerCase().startsWith("head-"))     return "PLAYER_HEAD";
        if (m.toLowerCase().startsWith("basehead-")) return "PLAYER_HEAD";
        if (m.toLowerCase().startsWith("hdb-"))      return "PLAYER_HEAD";
        return m.toUpperCase();
    }
}
