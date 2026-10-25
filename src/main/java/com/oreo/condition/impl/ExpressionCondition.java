package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public class ExpressionCondition implements Condition {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");

    private final SmartMenus plugin;
    private final String expression;

    public ExpressionCondition(SmartMenus plugin, String expression) {
        this.plugin = plugin;
        this.expression = expression;
    }

    @Override
    public boolean check(Player player) {
        if (expression == null || expression.isEmpty()) return false;
        try {
            String resolved = resolvePlaceholders(player, expression);
            return evalBoolean(resolved);
        } catch (Exception e) {
            plugin.getLogger().warning("ExpressionCondition failed for expression '" + expression + "': " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean take(Player player) {
        return check(player);
    }

    @Override
    public String getErrorMessage(Player player) {
        return com.oreo.util.ColorUtil.color("&cCondition not met: " + expression);
    }

    @Override
    public ConditionType getType() {
        return ConditionType.EXPRESSION;
    }

    private String resolvePlaceholders(Player player, String text) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return text;
        try {
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return (String) papiClass.getMethod("setPlaceholders", Player.class, String.class)
                    .invoke(null, player, text);
        } catch (Exception e) {
            return text;
        }
    }

    private boolean evalBoolean(String expr) {
        expr = expr.trim();

        String[] cmpOps = {">=", "<=", "==", "!=", ">", "<"};
        for (String op : cmpOps) {
            int idx = findOperatorIndex(expr, op);
            if (idx >= 0) {
                String left = expr.substring(0, idx).trim();
                String right = expr.substring(idx + op.length()).trim();
                double lv = evalArithmetic(left);
                double rv = evalArithmetic(right);
                switch (op) {
                    case ">=": return lv >= rv;
                    case "<=": return lv <= rv;
                    case "==": return lv == rv;
                    case "!=": return lv != rv;
                    case ">":  return lv > rv;
                    case "<":  return lv < rv;
                }
            }
        }

        return evalArithmetic(expr) != 0;
    }

    private int findOperatorIndex(String expr, String op) {
        int i = 0;
        while (i <= expr.length() - op.length()) {
            if (expr.startsWith(op, i)) {

                boolean isSingleChar = op.length() == 1;
                if (isSingleChar) {
                    char next = (i + 1 < expr.length()) ? expr.charAt(i + 1) : '\0';
                    if (next == '=' || next == '>') { i++; continue; }
                }
                return i;
            }
            i++;
        }
        return -1;
    }

    private int pos;
    private String src;

    private double evalArithmetic(String expr) {
        this.src = expr.trim();
        this.pos = 0;
        double result = parseAddSub();
        return result;
    }

    private double parseAddSub() {
        double left = parseMulDiv();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '+') { pos++; left += parseMulDiv(); }
            else if (c == '-') { pos++; left -= parseMulDiv(); }
            else break;
        }
        return left;
    }

    private double parseMulDiv() {
        double left = parseUnary();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '*') { pos++; left *= parseUnary(); }
            else if (c == '/') {
                pos++;
                double right = parseUnary();
                if (right == 0) throw new ArithmeticException("Division by zero");
                left /= right;
            }
            else break;
        }
        return left;
    }

    private double parseUnary() {
        skipSpaces();
        if (pos < src.length() && src.charAt(pos) == '-') {
            pos++;
            return -parseUnary();
        }
        if (pos < src.length() && src.charAt(pos) == '+') {
            pos++;
            return parseUnary();
        }
        return parseAtom();
    }

    private double parseAtom() {
        skipSpaces();
        if (pos < src.length() && src.charAt(pos) == '(') {
            pos++;
            double val = parseAddSub();
            skipSpaces();
            if (pos < src.length() && src.charAt(pos) == ')') pos++;
            return val;
        }
        return parseNumber();
    }

    private double parseNumber() {
        skipSpaces();
        int start = pos;
        if (pos < src.length() && src.charAt(pos) == '-') pos++;
        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
            pos++;
        }
        String numStr = src.substring(start, pos).trim();
        if (numStr.isEmpty()) throw new NumberFormatException("Expected number at position " + start + " in: " + src);
        return Double.parseDouble(numStr);
    }

    private void skipSpaces() {
        while (pos < src.length() && src.charAt(pos) == ' ') pos++;
    }
}
