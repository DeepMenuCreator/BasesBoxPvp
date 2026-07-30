package com.github.yourname.basesplugin;

import com.github.yourname.basesplugin.command.BaseCommand;
import com.github.yourname.basesplugin.gui.BaseGUI;
import com.github.yourname.basesplugin.listener.BotListener;
import com.github.yourname.basesplugin.listener.ProtectionListener;
import com.github.yourname.basesplugin.manager.BaseManager;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BasesPlugin extends JavaPlugin {
    private static BasesPlugin instance;
    private PlayerPointsAPI pointsAPI;
    private BaseManager baseManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
            pointsAPI = PlayerPoints.getInstance().getAPI();
        } else {
            getLogger().severe("PlayerPoints не найден! Плагин отключается.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        baseManager = new BaseManager(this);

        getCommand("base").setExecutor(new BaseCommand(this));
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BotListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BaseGUI(this), this);

        getLogger().info("BasesPlugin включен!");
    }

    @Override
    public void onDisable() {
        if (baseManager != null) baseManager.save();
    }

    public static BasesPlugin getInstance() {
        return instance;
    }

    public PlayerPointsAPI getPointsAPI() {
        return pointsAPI;
    }

    public BaseManager getBaseManager() {
        return baseManager;
    }
}
