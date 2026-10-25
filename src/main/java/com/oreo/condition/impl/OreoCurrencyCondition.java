package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import fr.elias.oreoEssentials.OreoEssentials;
import fr.elias.oreoEssentials.modules.currency.Currency;
import fr.elias.oreoEssentials.modules.currency.CurrencyService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class OreoCurrencyCondition implements Condition {
    private final SmartMenus plugin;
    private final String currencyId;
    private final double amount;

    public OreoCurrencyCondition(SmartMenus plugin, String currencyId, double amount) {
        this.plugin = plugin;
        this.currencyId = currencyId;
        this.amount = amount;
    }

    @Override
    public boolean check(Player player) {
        CurrencyService service = getCurrencyService();
        if (service == null) return false;
        try {
            double balance = service.getBalance(player.getUniqueId(), currencyId).get();
            return balance >= amount;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean take(Player player) {
        CurrencyService service = getCurrencyService();
        if (service == null) return false;
        try {
            return service.withdraw(player.getUniqueId(), currencyId, amount).get();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getErrorMessage(Player player) {
        CurrencyService service = getCurrencyService();

        if (service == null) {
            return plugin.getMessageManager().getMessage("conditions.oreo_currency.unavailable", player);
        }

        Currency currency = service.getCurrency(currencyId);
        if (currency == null) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("currency", currencyId);
            return plugin.getMessageManager().getMessage("conditions.oreo_currency.currency_not_found", player, replacements);
        }

        try {
            double balance = service.getBalance(player.getUniqueId(), currencyId).get();

            Map<String, String> replacements = new HashMap<>();
            replacements.put("currency", currency.getName());
            replacements.put("amount", currency.format(amount));
            replacements.put("balance", currency.format(balance));

            return plugin.getMessageManager().getMessage("conditions.oreo_currency.insufficient", player, replacements);
        } catch (Exception e) {
            return plugin.getMessageManager().getMessage("conditions.oreo_currency.failed_check", player);
        }
    }

    @Override
    public ConditionType getType() {
        return ConditionType.OREO_CURRENCY;
    }

    private CurrencyService getCurrencyService() {
        try {
            OreoEssentials oreo = (OreoEssentials) Bukkit.getPluginManager().getPlugin("OreoEssentials");
            return oreo != null && oreo.isEnabled() ? oreo.getCurrencyService() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
