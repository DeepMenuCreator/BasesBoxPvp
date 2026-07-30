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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

public class BaseCommand implements CommandExecutor {
    private final BasesPlugin plugin;
    private final NamespacedKey botKey;
    private final Map<Integer, Entity> spawnedBots = new HashMap<>();
    private int nextBotId = 1;

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
            int id = spawnBot(player, args[1]);
            player.sendMessage(ChatColor.GREEN + "Бот создан! ID: " + ChatColor.YELLOW + id);
            return true;
        }

        if (args[0].equalsIgnoreCase("botremove")) {
            if (!player.hasPermission("basesplugin.admin.bot")) {
                player.sendMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Использование: /base botremove <id>");
                return true;
            }
            try {
                int id = Integer.parseInt(args[1]);
                Entity ent = spawnedBots.remove(id);
                if (ent != null && !ent.isDead()) {
                    ent.remove();
                    player.sendMessage(ChatColor.GREEN + "Бот " + ChatColor.YELLOW + id + ChatColor.GREEN + " удалён!");
                } else {
                    player.sendMessage(ChatColor.RED + "Бот с ID " + id + " не найден!");
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "ID должен быть числом!");
            }
            return true;
        }

        return true;
    }

    private int spawnBot(Player player, String arg) {
        int id = nextBotId++;
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
            spawnedBots.put(id, ent);
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
            spawnedBots.put(id, stand);
        }
        return id;
    }
                    }
