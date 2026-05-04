package com.cactusupgrade.listeners;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.cactusupgrade.CactusUpgrade;
import com.cactusupgrade.managers.CactusUpgradeManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;

public class CactusGrowListener implements Listener {

    private final CactusUpgradeManager manager;

    public CactusGrowListener(CactusUpgrade plugin) {
        this.manager = plugin.getUpgradeManager();
    }

    /**
     * Intercepts vanilla cactus growth.
     * If the cactus is on an island with an upgrade level > 0,
     * we cancel the vanilla event — the global scheduler handles growth instead.
     * Islands with level 0 are untouched and use vanilla timing.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCactusGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CACTUS) return;

        Island island = SuperiorSkyblockAPI.getIslandAt(block.getLocation());
        if (island == null) return;

        if (manager.getIslandLevel(island) > 0) {
            event.setCancelled(true); // Global scheduler takes over
        }
    }
}
