package com.cactusupgrade.listeners;

import com.bgsoftware.superiorskyblock.api.events.IslandDisbandEvent;
import com.cactusupgrade.CactusUpgrade;
import com.cactusupgrade.managers.CactusUpgradeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class IslandDeleteListener implements Listener {

    private final CactusUpgradeManager manager;

    public IslandDeleteListener(CactusUpgrade plugin) {
        this.manager = plugin.getUpgradeManager();
    }

    /**
     * When an island is disbanded/deleted, automatically reset its cactus upgrade.
     * This clears the cache and removes the persisted data so if the same player
     * creates a new island, they start at level 0.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIslandDisband(IslandDisbandEvent event) {
        manager.resetIsland(event.getIsland());
    }
}
