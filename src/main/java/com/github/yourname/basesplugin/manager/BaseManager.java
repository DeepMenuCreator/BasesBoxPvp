package com.github.yourname.basesplugin.manager;

import com.github.yourname.basesplugin.BasesPlugin;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BaseManager {
    private final BasesPlugin plugin;
    private final File file;
    private FileConfiguration data;
    private final Map<UUID, Base> bases = new HashMap<>();
    private final Map<Integer, LevelData> levels = new LinkedHashMap<>();
    private final Set<String> protectedBlocks = new HashSet<>();
    private World baseWorld;
    private int nextIndex = 0;

    public BaseManager(BasesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        initLevels();
        setupWorld();
        load();
        startAutoSave();
    }

    private void initLevels() {
        levels.put(1, new LevelData(500, 5));
        levels.put(2, new LevelData(900, 7));
        levels.put(3, new LevelData(1200, 9));
        levels.put(4, new LevelData(1400, 11));
        levels.put(5, new LevelData(1600, 13));
        levels.put(6, new LevelData(1800, 15));
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
            var section = data.getConfigurationSection("bases");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        String path = "bases." + key;
                        int level = data.getInt(path + ".level", 1);

                        // Ручная загрузка location на случай, если getLocation не сработал
                        Location loc = data.getLocation(path + ".location");
                        if (loc == null && data.contains(path + ".location")) {
                            String worldName = data.getString(path + ".location.world", "bases");
                            double x = data.getDouble(path + ".location.x");
                            double y = data.getDouble(path + ".location.y");
                            double z = data.getDouble(path + ".location.z");
                            float yaw = (float) data.getDouble(path + ".location.yaw", 0);
                            float pitch = (float) data.getDouble(path + ".location.pitch", 0);
                            World w = Bukkit.getWorld(worldName);
                            if (w != null) loc = new Location(w, x, y, z, yaw, pitch);
                        }

                        if (loc != null) {
                            bases.put(uuid, new Base(uuid, level, loc));
                            buildPlatform(loc, level);
                            // Корректируем nextIndex, если база стоит дальше
                            int idx = (int) Math.round(loc.getX() / getSpacing());
                            if (idx >= nextIndex) nextIndex = idx + 1;
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Некорректный UUID в data.yml: " + key);
                    }
                }
            }
        }

        plugin.getLogger().info("Загружено баз: " + bases.size() + ", nextIndex: " + nextIndex);
    }

    public void save() {
        data.set("next-index", nextIndex);
        // Очищаем старые данные, чтобы не оставалось удалённых баз
        data.set("bases", null);
        for (Map.Entry<UUID, Base> entry : bases.entrySet()) {
            String path = "bases." + entry.getKey().toString();
            data.set(path + ".level", entry.getValue().getLevel());
            data.set(path + ".location", entry.getValue().getLocation());
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить data.yml!");
            e.printStackTrace();
        }
    }

    private void startAutoSave() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!bases.isEmpty()) save();
            }
        }.runTaskTimer(plugin, 6000L, 6000L); // Автосохранение каждые 5 минут
    }

    private int getSpacing() {
        return plugin.getConfig().getInt("base-spacing", 50);
    }

    public boolean hasBase(UUID uuid) {
        return bases.containsKey(uuid);
    }

    public Base getBase(UUID uuid) {
        return bases.get(uuid);
    }

    public int getMaxLevel() {
        return 6;
    }

    public int getPriceForLevel(int level) {
        LevelData ld = levels.get(level);
        return ld != null ? ld.price : 0;
    }

    public int getSizeForLevel(int level) {
        LevelData ld = levels.get(level);
        return ld != null ? ld.size : 5;
    }

    public void createBase(Player player) {
        UUID uuid = player.getUniqueId();
        int price = getPriceForLevel(1);
        if (plugin.getPointsAPI().look(uuid) < price) {
            player.sendMessage(ChatColor.RED + "Недостаточно DragonCoins! Нужно: " + price);
            return;
        }
        plugin.getPointsAPI().take(uuid, price);
        Location loc = new Location(baseWorld, nextIndex * getSpacing(), 64, 0);
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
        int current = base.getLevel();
        if (current >= getMaxLevel()) {
            player.sendMessage(ChatColor.RED + "Достигнут максимальный уровень базы!");
            return;
        }
        int next = current + 1;
        int price = getPriceForLevel(next);
        if (plugin.getPointsAPI().look(uuid) < price) {
            player.sendMessage(ChatColor.RED + "Недостаточно DragonCoins! Нужно: " + price);
            return;
        }
        plugin.getPointsAPI().take(uuid, price);
        clearOldPlatform(base.getLocation(), current);
        base.setLevel(next);
        buildPlatform(base.getLocation(), next);
        save();
        player.sendMessage(ChatColor.GREEN + "База улучшена до уровня " + next + "!");
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

    public void grantBase(UUID uuid, int level) {
        Base existing = bases.get(uuid);
        Location loc;
        if (existing != null) {
            clearOldPlatform(existing.getLocation(), existing.getLevel());
            loc = existing.getLocation();
        } else {
            loc = new Location(baseWorld, nextIndex * getSpacing(), 64, 0);
            nextIndex++;
        }
        Base base = new Base(uuid, level, loc);
        bases.put(uuid, base);
        buildPlatform(loc, level);
        save();
    }

    public void deleteBase(UUID uuid) {
        Base base = bases.remove(uuid);
        if (base != null) {
            clearOldPlatform(base.getLocation(), base.getLevel());
            save();
        }
    }

    public void setBaseLevel(UUID uuid, int newLevel) {
        Base base = bases.get(uuid);
        if (base == null) return;
        if (newLevel < 1 || newLevel > getMaxLevel()) return;
        clearOldPlatform(base.getLocation(), base.getLevel());
        base.setLevel(newLevel);
        buildPlatform(base.getLocation(), newLevel);
        save();
    }

    private String locKey(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    private void setProtected(World w, int x, int y, int z, Material mat) {
        Location loc = new Location(w, x, y, z);
        protectedBlocks.add(locKey(loc));
        Block b = w.getBlockAt(x, y, z);
        b.setType(mat);
    }

    private void removeProtection(World w, int x, int y, int z) {
        Location loc = new Location(w, x, y, z);
        protectedBlocks.remove(locKey(loc));
    }

    public void buildPlatform(Location center, int level) {
        LevelData ld = levels.get(level);
        int size = ld.size;
        double radius = size / 2.0;
        World w = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                double dist = Math.sqrt(x * x + z * z);

                if (dist <= radius) {
                    setProtected(w, cx + x, cy, cz + z, Material.GRASS_BLOCK);
                    setProtected(w, cx + x, cy - 1, cz + z, Material.DIRT);
                }

                for (int down = 1; down <= 4; down++) {
                    double rDown = radius * (1.0 - down * 0.22);
                    if (dist <= rDown) {
                        Material mat = (down <= 2) ? Material.DIRT : Material.STONE;
                        setProtected(w, cx + x, cy - 1 - down, cz + z, mat);
                    }
                }

                if (dist >= radius - 1.0 && dist <= radius + 0.8) {
                    for (int h = -1; h <= 8; h++) {
                        setProtected(w, cx + x, cy + h, cz + z, Material.BARRIER);
                    }
                }

                if (dist <= radius + 0.8) {
                    setProtected(w, cx + x, cy + 9, cz + z, Material.BARRIER);
                }
            }
        }

        setProtected(w, cx, cy, cz, Material.BEACON);
    }

    public void clearOldPlatform(Location center, int oldLevel) {
        int size = getSizeForLevel(oldLevel);
        double radius = size / 2.0 + 1.5;
        World w = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -size - 2; x <= size + 2; x++) {
            for (int z = -size - 2; z <= size + 2; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist > radius) continue;
                for (int y = cy - 6; y <= cy + 10; y++) {
                    Location loc = new Location(w, cx + x, y, cz + z);
                    if (protectedBlocks.contains(locKey(loc))) {
                        removeProtection(w, cx + x, y, cz + z);
                        w.getBlockAt(cx + x, y, cz + z).setType(Material.AIR);
                    }
                }
            }
        }
    }

    public boolean isProtectedBlock(Location loc) {
        return protectedBlocks.contains(locKey(loc));
    }

    public World getBaseWorld() {
        return baseWorld;
    }

    private static class LevelData {
        final int price;
        final int size;
        LevelData(int price, int size) {
            this.price = price;
            this.size = size;
        }
    }
    }
                         
