package com.cactusupgrade;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.cactusupgrade.listeners.CactusGrowListener;
import com.cactusupgrade.listeners.IslandDeleteListener;
import com.cactusupgrade.managers.CactusUpgradeManager;
import com.cactusupgrade.commands.CactusUpgradeCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class CactusUpgrade extends JavaPlugin {

    private static CactusUpgrade instance;
    private CactusUpgradeManager upgradeManager;

    @Override
    public void onEnable() {
        instance = this;

        // Check SuperiorSkyblock2 hook
        if (getServer().getPluginManager().getPlugin("SuperiorSkyblock2") == null) {
            getLogger().severe("SuperiorSkyblock2 not found! Disabling CactusUpgrade.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Save default config
        saveDefaultConfig();

        // Initialize manager
        upgradeManager = new CactusUpgradeManager(this);

        // Register listeners
        getServer().getPluginManager().registerEvents(new CactusGrowListener(this), this);
        getServer().getPluginManager().registerEvents(new IslandDeleteListener(this), this);

        // Register commands
        getCommand("cactusupgrade").setExecutor(new CactusUpgradeCommand(this));
        getCommand("cactusupgrade").setTabCompleter(new CactusUpgradeCommand(this));

        getLogger().info("CactusUpgrade enabled! Hooked into SuperiorSkyblock2.");
    }

    @Override
    public void onDisable() {
        if (upgradeManager != null) {
            upgradeManager.shutdown();
        }
        getLogger().info("CactusUpgrade disabled.");
    }

    public static CactusUpgrade getInstance() {
        return instance;
    }

    public CactusUpgradeManager getUpgradeManager() {
        return upgradeManager;
    }
}
