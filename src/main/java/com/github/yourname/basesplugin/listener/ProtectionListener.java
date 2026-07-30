package com.github.yourname.basesplugin.listener;

import com.github.yourname.basesplugin.BasesPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class ProtectionListener implements Listener {
    private final BasesPlugin plugin;

    public ProtectionListener(BasesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!e.getBlock().getWorld().getName().equals("bases")) return;
        if (plugin.getBaseManager().isProtectedBlock(e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cНельзя ломать блоки платформы!");
        }
        // Свои блоки (не защищённые) ломать можно
    }
}
