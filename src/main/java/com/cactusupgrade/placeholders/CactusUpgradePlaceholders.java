package com.cactusupgrade.placeholders;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.cactusupgrade.CactusUpgrade;
import com.cactusupgrade.managers.CactusUpgradeManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CactusUpgradePlaceholders extends PlaceholderExpansion {

    private final CactusUpgrade plugin;
    private final CactusUpgradeManager manager;

    public CactusUpgradePlaceholders(CactusUpgrade plugin) {
        this.plugin = plugin;
        this.manager = plugin.getUpgradeManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "cactusupgrade";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // Stay registered on PlaceholderAPI reload
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        // Get the island of the player
        SuperiorPlayer superiorPlayer = SuperiorSkyblockAPI.getPlayer(player.getUniqueId());
        if (superiorPlayer == null) return "N/A";

        Island island = superiorPlayer.getIsland();
        if (island == null) return "N/A";

        int level = manager.getIslandLevel(island);
        int maxLevel = manager.getMaxLevel();

        switch (params.toLowerCase()) {

            // Current upgrade level (e.g. 3)
            case "level":
                return String.valueOf(level);

            // Current multiplier (e.g. 1.3)
            case "multiplier":
                return formatDouble(manager.getMultiplierForLevel(level));

            // Multiplier of the next level, or "MAX" if already at max
            case "next_multiplier":
                if (level >= maxLevel) return "MAX";
                return formatDouble(manager.getMultiplierForLevel(level + 1));

            // Maximum level allowed by config
            case "max_level":
                return String.valueOf(maxLevel);

            // Current grow interval in ticks
            case "grow_ticks":
                return String.valueOf(manager.getGrowTicksForIsland(island));

            // Current grow interval in seconds (rounded)
            case "grow_seconds":
                long ticks = manager.getGrowTicksForIsland(island);
                return String.valueOf(ticks / 20);

            // Whether the island has any upgrade active (true/false)
            case "has_upgrade":
                return level > 0 ? "true" : "false";

            // Remaining levels until max
            case "levels_remaining":
                return String.valueOf(Math.max(0, maxLevel - level));

            default:
                return null;
        }
    }

    private String formatDouble(double value) {
        // Show as integer if whole number (e.g. 2.0 -> "2"), otherwise keep decimal
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }
}
