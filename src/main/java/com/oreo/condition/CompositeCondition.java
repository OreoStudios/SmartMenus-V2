package com.oreo.condition;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CompositeCondition implements Condition {

    private final List<Condition> conditions;
    private final boolean isOr;
    private final int minimum;

    public CompositeCondition(List<Condition> conditions, boolean isOr, int minimum) {
        this.conditions = conditions != null ? conditions : new ArrayList<>();
        this.isOr = isOr;
        this.minimum = minimum;
    }

    @Override
    public boolean check(Player player) {
        if (isOr) {
            int passed = 0;
            for (Condition c : conditions) {
                if (c.check(player)) passed++;
                if (passed >= minimum) return true;
            }
            return false;
        } else {
            for (Condition c : conditions) {
                if (!c.check(player)) return false;
            }
            return true;
        }
    }

    @Override
    public boolean take(Player player) {
        if (isOr) {

            for (Condition c : conditions) {
                if (c.check(player)) {
                    c.take(player);
                }
            }
        } else {
            for (Condition c : conditions) {
                if (!c.take(player)) return false;
            }
        }
        return true;
    }

    @Override
    public String getErrorMessage(Player player) {
        List<String> errors = new ArrayList<>();
        for (Condition c : conditions) {
            if (!c.check(player)) {
                String msg = c.getErrorMessage(player);
                if (msg != null && !msg.isEmpty()) {
                    errors.add(msg);
                }
            }
        }
        if (isOr) {
            return "You need at least " + minimum + " of the following: " + String.join(", ", errors);
        } else {
            return String.join(" | ", errors);
        }
    }

    @Override
    public ConditionType getType() {
        return null;
    }
}
