package com.oreo.action;

import com.oreo.SmartMenus;
import org.bukkit.entity.Player;
import java.util.Map;

public interface Action {

    void execute(SmartMenus plugin, Player player, Map<String, String> vars);
}
