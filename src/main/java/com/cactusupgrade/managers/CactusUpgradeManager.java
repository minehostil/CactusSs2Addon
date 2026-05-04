package com.cactusupgrade.managers;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.cactusupgrade.CactusUpgrade;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class CactusUpgradeManager {

    private final CactusUpgrade plugin;

    // islandUUID -> current upgrade level (in-memory cache)
    private final Map<UUID, Integer> islandUpgradeLevels = new HashMap<>();

    // islandUUID -> last globalTick when this island was processed
    private final Map<UUID, Long> islandTickCounters = new HashMap<>();

    // Upgrade level -> multiplier from config
    private final Map<Integer, Double> levelMultipliers = new LinkedHashMap<>();
    private final Map<Integer, String> levelNames = new LinkedHashMap<>();

    private int maxLevel;
    private long baseGrowTime;

    // Max cactus columns to grow per scheduler run to avoid TPS spikes
    private int maxGrowthsPerTick;

    // Single global scheduler task
    private BukkitTask globalTask;

    // Monotonic counter incremented every scheduler run
    private long globalTick = 0;

    // Key used in SuperiorSkyblock2 island data to persist the upgrade level
    private static final String DATA_KEY = "cactus_upgrade_level";

    public CactusUpgradeManager(CactusUpgrade plugin) {
        this.plugin = plugin;
        loadConfig();
        startGlobalScheduler();
    }

    // -------------------------------------------------------------------------
    // Config
    // -------------------------------------------------------------------------

    public void loadConfig() {
        plugin.reloadConfig();
        levelMultipliers.clear();
        levelNames.clear();

        baseGrowTime = plugin.getConfig().getLong("base-grow-time", 21600L);
        maxLevel = plugin.getConfig().getInt("upgrade.max-level", 10);
        maxGrowthsPerTick = plugin.getConfig().getInt("max-growths-per-tick", 50);

        ConfigurationSection levels = plugin.getConfig().getConfigurationSection("upgrade.levels");
        if (levels != null) {
            for (String key : levels.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    double multiplier = levels.getDouble(key + ".multiplier", 1.0);
                    String name = levels.getString(key + ".name", "Level " + level);
                    levelMultipliers.put(level, multiplier);
                    levelNames.put(level, name);
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    // -------------------------------------------------------------------------
    // Global scheduler — ONE task for the entire plugin
    // -------------------------------------------------------------------------

    private void startGlobalScheduler() {
        // Runs every tick. We use per-island counters to space out actual growths,
        // so no thousands of separate BukkitRunnables are created.
        globalTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processCactusGrowth, 1L, 1L);
    }

    /**
     * Called every server tick.
     * Checks each SS2 island, and if its growth interval has elapsed,
     * grows eligible cactus columns within loaded chunks only.
     * A growth budget caps how many columns grow per tick to prevent spikes.
     */
    private void processCactusGrowth() {
        globalTick++;
        int growthBudget = maxGrowthsPerTick;

        List<Island> islands = SuperiorSkyblockAPI.getGrid().getIslands();

        for (Island island : islands) {
            if (growthBudget <= 0) break;

            int level = getIslandLevel(island);
            if (level <= 0) continue; // No upgrade — vanilla handles this island

            UUID uuid = island.getUniqueId();
            long growInterval = getGrowTicksForLevel(level);

            long lastTick = islandTickCounters.getOrDefault(uuid, 0L);
            if ((globalTick - lastTick) < growInterval) continue;

            islandTickCounters.put(uuid, globalTick);
            growthBudget = growCactusInIsland(island, growthBudget);
        }
    }

    /**
     * Scans loaded chunks within the island's bounding box and grows cactus.
     * Never force-loads chunks.
     */
    private int growCactusInIsland(Island island, int budget) {
        World world = Bukkit.getWorld(island.getCenter().getWorldName());
        if (world == null) return budget;

        int minChunkX = (int) Math.floor(island.getMinimum().getX()) >> 4;
        int maxChunkX = (int) Math.floor(island.getMaximum().getX()) >> 4;
        int minChunkZ = (int) Math.floor(island.getMinimum().getZ()) >> 4;
        int maxChunkZ = (int) Math.floor(island.getMaximum().getZ()) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX && budget > 0; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ && budget > 0; cz++) {
                if (!world.isChunkLoaded(cx, cz)) continue;
                budget = growCactusInChunk(world.getChunkAt(cx, cz), budget);
            }
        }

        return budget;
    }

    /**
     * Iterates a chunk looking for base cactus blocks (cactus sitting on non-cactus)
     * and grows the top of each column if the space above is free.
     */
    private int growCactusInChunk(Chunk chunk, int budget) {
        int maxHeight = chunk.getWorld().getMaxHeight();
        int minHeight = chunk.getWorld().getMinHeight();

        for (int x = 0; x < 16 && budget > 0; x++) {
            for (int z = 0; z < 16 && budget > 0; z++) {
                for (int y = minHeight; y < maxHeight - 1; y++) {
                    Block block = chunk.getBlock(x, y, z);

                    // Only interested in the base of a cactus column
                    if (block.getType() != Material.CACTUS) continue;
                    if (block.getRelative(BlockFace.DOWN).getType() == Material.CACTUS) continue;

                    Block top = getTopCactus(block);
                    Block above = top.getRelative(BlockFace.UP);

                    if (above.getType() == Material.AIR && canCactusGrow(above)) {
                        above.setType(Material.CACTUS);
                        budget--;
                    }
                }
            }
        }

        return budget;
    }

    /**
     * Vanilla cactus adjacency rule: all four horizontal neighbours must be air.
     */
    private boolean canCactusGrow(Block block) {
        return block.getRelative(BlockFace.NORTH).getType() == Material.AIR
                && block.getRelative(BlockFace.SOUTH).getType() == Material.AIR
                && block.getRelative(BlockFace.EAST).getType()  == Material.AIR
                && block.getRelative(BlockFace.WEST).getType()  == Material.AIR;
    }

    /** Walks up a cactus column to find the topmost block. */
    private Block getTopCactus(Block base) {
        Block current = base;
        while (current.getRelative(BlockFace.UP).getType() == Material.CACTUS) {
            current = current.getRelative(BlockFace.UP);
        }
        return current;
    }

    // -------------------------------------------------------------------------
    // Island upgrade level
    // -------------------------------------------------------------------------

    public int getIslandLevel(Island island) {
        UUID uuid = island.getUniqueId();
        if (islandUpgradeLevels.containsKey(uuid)) {
            return islandUpgradeLevels.get(uuid);
        }
        String stored = island.getIslandData(DATA_KEY);
        int level = 0;
        if (stored != null && !stored.isEmpty()) {
            try { level = Integer.parseInt(stored); } catch (NumberFormatException ignored) {}
        }
        islandUpgradeLevels.put(uuid, level);
        return level;
    }

    public void setIslandLevel(Island island, int level) {
        if (level < 0) level = 0;
        if (level > maxLevel) level = maxLevel;

        UUID uuid = island.getUniqueId();
        islandUpgradeLevels.put(uuid, level);
        islandTickCounters.put(uuid, globalTick); // reset interval on change
        island.setIslandData(DATA_KEY, String.valueOf(level));
    }

    /**
     * Resets the island upgrade to level 0 and clears all cached data.
     * Called by command or automatically when the island is deleted.
     */
    public void resetIsland(Island island) {
        UUID uuid = island.getUniqueId();
        islandUpgradeLevels.remove(uuid);
        islandTickCounters.remove(uuid);
        island.setIslandData(DATA_KEY, "0");
    }

    // -------------------------------------------------------------------------
    // Multiplier / timing helpers
    // -------------------------------------------------------------------------

    public double getMultiplierForLevel(int level) {
        if (level <= 0) return 1.0;
        return levelMultipliers.getOrDefault(level, 1.0);
    }

    public long getGrowTicksForLevel(int level) {
        double multiplier = getMultiplierForLevel(level);
        return Math.max(1L, (long) (baseGrowTime / multiplier));
    }

    public long getGrowTicksForIsland(Island island) {
        return getGrowTicksForLevel(getIslandLevel(island));
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int getMaxLevel()                          { return maxLevel; }
    public long getBaseGrowTime()                     { return baseGrowTime; }
    public Map<Integer, Double> getLevelMultipliers() { return Collections.unmodifiableMap(levelMultipliers); }
    public String getLevelName(int level)             { return levelNames.getOrDefault(level, "Level " + level); }

    // -------------------------------------------------------------------------
    // Shutdown
    // -------------------------------------------------------------------------

    public void shutdown() {
        if (globalTask != null) {
            globalTask.cancel();
            globalTask = null;
        }
        islandUpgradeLevels.clear();
        islandTickCounters.clear();
    }
}
