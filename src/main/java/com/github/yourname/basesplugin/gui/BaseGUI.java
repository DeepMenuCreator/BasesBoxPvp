package com.github.yourname.basesplugin.gui;

import com.github.yourname.basesplugin.BasesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.UUID;

public class BaseGUI implements Listener {
    private final BasesPlugin plugin;
    private final String title = ChatColor.DARK_GRAY + "Меню базы";

    public BaseGUI(BasesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, title);

        ItemStack glass = item(Material.BLACK_STAINED_GLASS_PANE, " ", 0);
        for (int i = 0; i < 27; i++) inv.setItem(i, glass);

        UUID uuid = player.getUniqueId();
        boolean has = plugin.getBaseManager().hasBase(uuid);

        if (!has) {
            inv.setItem(11, item(Material.LIME_WOOL,
                    ChatColor.GREEN + "Купить",
                    1001,
                    ChatColor.GRAY + "Цена: " + ChatColor.YELLOW + "1000 DragonCoins",
                    ChatColor.GRAY + "Нажмите, чтобы купить"));

            inv.setItem(15, item(Material.TRIPWIRE_HOOK,
                    ChatColor.RED + "Заблокировано",
                    1002,
                    ChatColor.GRAY + "Купите базу для доступа"));
        } else {
            int lvl = plugin.getBaseManager().getBase(uuid).getLevel();
            int price = plugin.getBaseManager().getUpgradePrice(lvl);

            inv.setItem(11, item(Material.GOLD_BLOCK,
                    ChatColor.GOLD + "Улучшить базу",
                    1003,
                    ChatColor.GRAY + "Уровень: " + ChatColor.YELLOW + lvl,
                    ChatColor.GRAY + "Цена: " + ChatColor.YELLOW + price + " DragonCoins",
                    ChatColor.GRAY + "Нажмите для улучшения"));

            inv.setItem(15, item(Material.ENDER_PEARL,
                    ChatColor.AQUA + "Телепорт",
                    1004,
                    ChatColor.GRAY + "Телепортируйтесь на базу"));
        }

        player.openInventory(inv);
    }

    private ItemStack item(Material mat, String name, int cmd, String... lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(name);
        if (lore.length > 0) m.setLore(Arrays.asList(lore));
        if (cmd > 0) m.setCustomModelData(cmd);
        i.setItemMeta(m);
        return i;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(title)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta()) return;

        String name = ChatColor.stripColor(it.getItemMeta().getDisplayName());
        p.closeInventory();

        switch (name) {
            case "Купить" -> plugin.getBaseManager().createBase(p);
            case "Улучшить базу" -> plugin.getBaseManager().upgradeBase(p);
            case "Телепорт" -> plugin.getBaseManager().teleportToBase(p);
        }
    }
}
