package com.github.yourname.basesplugin.listener;

import com.github.yourname.basesplugin.BasesPlugin;
import com.github.yourname.basesplugin.gui.BaseGUI;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

public class BotListener implements Listener {
    private final BasesPlugin plugin;
    private final NamespacedKey key;

    public BotListener(BasesPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "basebot");
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        Entity ent = e.getRightClicked();
        if (ent.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            e.setCancelled(true);
            if (e.getPlayer() instanceof Player p) {
                new BaseGUI(plugin).open(p);
            }
        }
    }
}
