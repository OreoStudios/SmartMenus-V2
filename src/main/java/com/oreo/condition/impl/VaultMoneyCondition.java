package com.oreo.condition.impl;

import com.oreo.SmartMenus;
import com.oreo.condition.Condition;
import com.oreo.condition.ConditionType;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;

public class VaultMoneyCondition implements Condition {
    private final SmartMenus plugin;
    private final double amount;
    private Economy economy;

    public VaultMoneyCondition(SmartMenus plugin, double amount) {
        this.plugin = plugin;
        this.amount = amount;
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) economy = rsp.getProvider();
    }

    @Override
    public boolean check(Player player) {
        return economy != null && economy.has(player, amount);
    }

    @Override
    public boolean take(Player player) {
        if (economy == null || !check(player)) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @Override
    public String getErrorMessage(Player player) {
        if (economy == null) {
            return plugin.getMessageManager().getMessage("conditions.vault_money.economy_unavailable", player);
        }

        double balance = economy.getBalance(player);
        Map<String, String> replacements = new HashMap<>();
        replacements.put("amount", String.format("%.2f", amount));
        replacements.put("balance", String.format("%.2f", balance));

        return plugin.getMessageManager().getMessage("conditions.vault_money.insufficient", player, replacements);
    }

    @Override
    public ConditionType getType() {
        return ConditionType.VAULT_MONEY;
    }
}
