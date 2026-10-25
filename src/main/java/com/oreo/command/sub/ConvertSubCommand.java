package com.oreo.command.sub;

import com.oreo.SmartMenus;
import com.oreo.converter.CommandPanelConverter;
import com.oreo.converter.DeluxeMenusConverter;
import com.oreo.converter.ZMenusConverter;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConvertSubCommand extends SubCommand {

    public ConvertSubCommand(SmartMenus plugin) {
        super(plugin);
    }

    @Override
    public String name() {
        return "convert";
    }

    @Override
    public String primaryPermission() {
        return "smartmenus.convert";
    }

    @Override
    public String legacyPermission() {
        return "ogui.convert";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            msg(sender, "&e&lSmartMenus Converter");
            msg(sender, "&7Usage: &f/smartmenus convert <type> <file>");
            msg(sender, "&7Types: &fcp &8(CommandPanel) &7| &fdm &8(DeluxeMenus) &7| &fzm &8(ZMenus)");
            msg(sender, "&7Example: &f/smartmenus convert dm guide");
            msg(sender, "&7Inputs:  &fplugins/SmartMenus/converter/commandpanel/");
            msg(sender, "&7         &fplugins/SmartMenus/converter/deluxemenus/");
            msg(sender, "&7         &fplugins/SmartMenus/converter/zmenus/");
            msg(sender, "&7Outputs: &fplugins/SmartMenus/guis/converted/");
            return;
        }

        String convertType = args[1].toLowerCase(Locale.ENGLISH);
        String inputSubDir = switch (convertType) {
            case "cp", "commandpanel" -> "converter/commandpanel";
            case "dm", "deluxemenus" -> "converter/deluxemenus";
            case "zm", "zmenus" -> "converter/zmenus";
            default -> null;
        };
        if (inputSubDir == null) {
            msg(sender, "&cUnknown type: &f" + convertType + " &c— use &fcp&c, &fdm&c, or &fzm");
            return;
        }

        String fileName = args[2];
        if (!fileName.endsWith(".yml")) fileName += ".yml";

        File inputDir = new File(plugin.getDataFolder(), inputSubDir);
        File inputFile = new File(inputDir, fileName);
        if (!inputFile.exists()) {
            msg(sender, "&c✗ File not found: &f" + inputFile.getAbsolutePath());
            msg(sender, "&7Place your YAML files in: &f" + inputDir.getPath());
            return;
        }

        File outputDir = new File(plugin.getDataFolder(), "guis/converted");
        try {
            File outputFile = switch (convertType) {
                case "cp", "commandpanel" -> CommandPanelConverter.convert(inputFile, outputDir, null);
                case "dm", "deluxemenus" -> DeluxeMenusConverter.convert(inputFile, outputDir, null);
                case "zm", "zmenus" -> ZMenusConverter.convert(inputFile, outputDir, null);
                default -> throw new IOException("Unknown type");
            };
            plugin.reloadGuis();
            msg(sender, "&a✔ Converted successfully!");
            msg(sender, "&7Output: &fplugins/SmartMenus/guis/converted/" + outputFile.getName());
            msg(sender, "&7Use &f/smartmenus open " + outputFile.getName().replace(".yml", "") + " &7to test.");
        } catch (IOException e) {
            msg(sender, "&c✗ Conversion failed: " + e.getMessage());
            plugin.getLogger().severe("[Converter] Failed to convert " + fileName + ": " + e.getMessage());
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return filter(List.of("cp", "dm", "zm"), args[1]);
        }
        if (args.length == 3) {
            String subDir = switch (args[1].toLowerCase(Locale.ENGLISH)) {
                case "dm", "deluxemenus" -> "converter/deluxemenus";
                case "zm", "zmenus" -> "converter/zmenus";
                default -> "converter/commandpanel";
            };
            File inputDir = new File(plugin.getDataFolder(), subDir);
            List<String> files = new ArrayList<>();
            if (inputDir.exists() && inputDir.isDirectory()) {
                File[] ymlFiles = inputDir.listFiles((d, n) -> n.endsWith(".yml"));
                if (ymlFiles != null) {
                    for (File f : ymlFiles) files.add(f.getName().replace(".yml", ""));
                }
            }
            return filter(files, args[2]);
        }
        return List.of();
    }
}
