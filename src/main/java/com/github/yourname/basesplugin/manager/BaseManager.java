package com.github.yourname.basesplugin.manager;

import com.github.yourname.basesplugin.BasesPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BaseManager {
    private final BasesPlugin plugin;
    private final File file;
    private FileConfiguration data;
    private final Map<UUID, Base> bases = new HashMap<>();
    private World baseWorld;
    private int nextIndex = 0;

    public BaseManager(BasesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        load();
        setupWorld();
    }

    private void setupWorld() {
        baseWorld = Bukkit.getWorld("bases");
        if (baseWorld == null) {
            WorldCreator wc = new WorldCreator("bases");
            wc.type(WorldType.FLAT);
            wc.generatorSettings("{\"layers\":[]}");
            baseWorld = Bukkit.createWorld(wc);
        }
        if (baseWorld != null) {
            baseWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            baseWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            baseWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            baseWorld.setTime(6000);
            baseWorld.setDifficulty(Difficulty.PEACEFUL);
        }
    }

    private void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        data = YamlConfiguration.loadConfiguration(file);
        nextIndex = data.getInt("next-index", 0);

        if (data.contains("bases")) {
            for (String key : data.getConfigurationSection("bases").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                String path = "bases." + key;
                int level = data.getInt(path + ".level", 1);
                Location loc = data.getLocation(path + ".location");
                if (loc != null) {
                    bases.put(uuid, new Base(uuid, level, loc));
                    buildPlatform(loc, level);
                }
            }
        }
    }

    public void save() {
        data.set("next-index", nextIndex);
        for (Map.Entry<UUID, Base> entry : bases.entrySet()) {
            String path = "bases." + entry.getKey().toString();
            data.set(path + ".level", entry.getValue().getLevel());
            data.set(path + ".location", entry.getValue().getLocation());
        }
        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasBase(UUID uuid) {
        return bases.containsKey(uuid);
    }

    public Base getBase(UUID uuid) {
        return bases.get(uuid);
    }

    public int getUpgradePrice(int level) {
        return plugin.getConfig().getInt("base-price", 1000) * level;
    }

    public void createBase(Player player) {
        UUID uuid = player.getUniqueId();
        int price = plugin.getConfig().getInt("base-price", 1000);

        if (plugin.getPointsAPI().look(uuid) < price) {
            player.sendMessage(ChatColor.RED + "Недостаточно DragonCoins!");
            return;
        }

        plugin.getPointsAPI().take(uuid, price);

        int spacing = plugin.getConfig().getInt("base-spacing", 50);
        Location loc = new Location(baseWorld, nextIndex * spacing, 64, 0);
        nextIndex++;

        Base base = new Base(uuid, 1, loc);
        bases.put(uuid, base);
        buildPlatform(loc, 1);
        save();

        player.sendMessage(ChatColor.GREEN + "База куплена! Телепортируйтесь через /base");
    }

    public void upgradeBase(Player player) {
        UUID uuid = player.getUniqueId();
        Base base = bases.get(uuid);
        if (base == null) return;

        int price = getUpgradePrice(base.getLevel());
        if (plugin.getPointsAPI().look(uuid) < price) {
            player.sendMessage(ChatColor.RED + "Недостаточно DragonCoins! Нужно: " + price);
            return;
        }

        plugin.getPointsAPI().take(uuid, price);
        base.setLevel(base.getLevel() + 1);
        buildPlatform(base.getLocation(), base.getLevel());
        save();

        player.sendMessage(ChatColor.GREEN + "База улучшена до уровня " + base.getLevel() + "!");
    }

    public void teleportToBase(Player player) {
        Base base = bases.get(player.getUniqueId());
        if (base == null) {
            player.sendMessage(ChatColor.RED + "У вас нет базы!");
            return;
        }
        Location tp = base.getLocation().clone().add(0.5, 1, 0.5);
        player.teleport(tp);
        player.sendMessage(ChatColor.GREEN + "Телепортация на базу...");
    }

    public void buildPlatform(Location center, int level) {
        int size = 5 + (level - 1) * 2;
        int half = size / 2;
        World w = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -half; x <= half; x++) {
            for (int z = -half; z <= half; z++) {
                boolean edge = Math.abs(x) == half || Math.abs(z) == half;
                Block b = w.getBlockAt(cx + x, cy, cz + z);
                if (edge) {
                    b.setType(Material.BARRIER);
                } else {
                    b.setType(Material.GRASS_BLOCK);
                    w.getBlockAt(cx + x, cy - 1, cz + z).setType(Material.STONE);
                    w.getBlockAt(cx + x, cy - 2, cz + z).setType(Material.STONE);
                }
            }
        }
        w.getBlockAt(cx, cy, cz).setType(Material.BEACON);
    }

    public boolean isPlatformBlock(Location loc) {
        if (!loc.getWorld().equals(baseWorld)) return false;
        for (Base base : bases.values()) {
            Location c = base.getLocation();
            int size = 5 + (base.getLevel() - 1) * 2;
            int half = size / 2;
            int dx = Math.abs(loc.getBlockX() - c.getBlockX());
            int dz = Math.abs(loc.getBlockZ() - c.getBlockZ());
            int dy = c.getBlockY() - loc.getBlockY();

            if (dx <= half && dz <= half && dy >= 0 && dy <= 2) {
                Material m = loc.getBlock().getType();
                if (m == Material.GRASS_BLOCK || m == Material.STONE ||
                    m == Material.BARRIER || m == Material.BEACON || m == Material.DIRT) {
                    return true;
                }
            }
        }
        return false;
    }

    public World getBaseWorld() {
        return baseWorld;
    }
}
