package com.cactusupgrade.commands;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.cactusupgrade.CactusUpgrade;
import com.cactusupgrade.managers.CactusUpgradeManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CactusUpgradeCommand implements CommandExecutor, TabCompleter {

    private final CactusUpgrade plugin;
    private final CactusUpgradeManager manager;

    public CactusUpgradeCommand(CactusUpgrade plugin) {
        this.plugin = plugin;
        this.manager = plugin.getUpgradeManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("cactusupgrade.admin")) {
            sender.sendMessage(color(plugin.getConfig().getString("messages.no-permission")));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(color(plugin.getConfig().getString("messages.usage")));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String targetName = args[1];

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(target.getUniqueId());
        Island island = superiorPlayer != null ? superiorPlayer.getIsland() : null;

        if (island == null) {
            sender.sendMessage(color(plugin.getConfig().getString("messages.no-island")
                    .replace("{player}", targetName)));
            return true;
        }

        switch (subCommand) {
            case "set": {
                if (args.length < 3) {
                    sender.sendMessage(color(plugin.getConfig().getString("messages.usage")));
                    return true;
                }
                int level;
                try {
                    level = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(color(plugin.getConfig().getString("messages.invalid-level")
                            .replace("{max}", String.valueOf(manager.getMaxLevel()))));
                    return true;
                }

                if (level < 0 || level > manager.getMaxLevel()) {
                    sender.sendMessage(color(plugin.getConfig().getString("messages.invalid-level")
                            .replace("{max}", String.valueOf(manager.getMaxLevel()))));
                    return true;
                }

                manager.setIslandLevel(island, level);
                sender.sendMessage(color(plugin.getConfig().getString("messages.upgrade-set")
                        .replace("{level}", String.valueOf(level))
                        .replace("{player}", targetName)));
                break;
            }

            case "reset": {
                manager.resetIsland(island);
                sender.sendMessage(color(plugin.getConfig().getString("messages.upgrade-reset")
                        .replace("{player}", targetName)));
                break;
            }

            case "info": {
                int level = manager.getIslandLevel(island);
                double multiplier = manager.getMultiplierForLevel(level);
                sender.sendMessage(color(plugin.getConfig().getString("messages.upgrade-info")
                        .replace("{player}", targetName)
                        .replace("{level}", String.valueOf(level))
                        .replace("{multiplier}", String.valueOf(multiplier))));
                break;
            }

            default:
                sender.sendMessage(color(plugin.getConfig().getString("messages.usage")));
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("cactusupgrade.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return Arrays.asList("set", "reset", "info").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(p -> p.getName())
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            List<String> levels = new ArrayList<>();
            for (int i = 0; i <= manager.getMaxLevel(); i++) {
                levels.add(String.valueOf(i));
            }
            return levels;
        }

        return new ArrayList<>();
    }

    private String color(String msg) {
        if (msg == null) return "";
        return msg.replace("&", "§");
    }
}
