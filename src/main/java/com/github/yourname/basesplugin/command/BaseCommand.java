package com.github.yourname.basesplugin.command;

import com.github.yourname.basesplugin.BasesPlugin;
import com.github.yourname.basesplugin.gui.BaseGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public class BaseCommand implements CommandExecutor {
    private final BasesPlugin plugin;
    private final NamespacedKey botKey;

    public BaseCommand(BasesPlugin plugin) {
        this.plugin = plugin;
        this.botKey = new NamespacedKey(plugin, "basebot");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }

        if (args.length == 0) {
            new BaseGUI(plugin).open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("botspawn")) {
            if (!player.hasPermission("basesplugin.admin.bot")) {
                player.sendMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Использование: /base botspawn <mob/ник>");
                return true;
            }
            spawnBot(player, args[1]);
            player.sendMessage(ChatColor.GREEN + "Бот создан!");
            return true;
        }

        return true;
    }

    private void spawnBot(Player player, String arg) {
        try {
            EntityType type = EntityType.valueOf(arg.toUpperCase());
            LivingEntity ent = (LivingEntity) player.getWorld().spawnEntity(player.getLocation(), type);
            ent.setAI(false);
            ent.setInvulnerable(true);
            ent.setSilent(true);
            ent.setCustomNameVisible(true);
            ent.setCustomName(ChatColor.GOLD + "База ▶ Нажмите ПКМ");
            ent.getPersistentDataContainer().set(botKey, PersistentDataType.BYTE, (byte) 1);
            if (ent instanceof org.bukkit.entity.Ageable ageable) ageable.setAdult();
        } catch (IllegalArgumentException ex) {
            ArmorStand stand = player.getWorld().spawn(player.getLocation(), ArmorStand.class, as -> {
                as.setVisible(false);
                as.setGravity(false);
                as.setInvulnerable(true);
                as.setSmall(true);
                as.setMarker(true);
                as.setCustomNameVisible(true);
                as.setCustomName(ChatColor.GOLD + "База ▶ Нажмите ПКМ");
                as.getPersistentDataContainer().set(botKey, PersistentDataType.BYTE, (byte) 1);
            });

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwner(arg);
            head.setItemMeta(meta);
            stand.getEquipment().setHelmet(head);
        }
    }
}
