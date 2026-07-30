package com.github.yourname.basesplugin.command;

import com.github.yourname.basesplugin.BasesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {
    private final BasesPlugin plugin;

    public AdminCommand(BasesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "grantbase" -> handleGrant(sender, args);
            case "delbase"    -> handleDelete(sender, args);
            case "upgradebase"-> handleUpgrade(sender, args);
        }
        return true;
    }

    private void handleGrant(CommandSender sender, String[] args) {
        if (!sender.hasPermission("basesplugin.admin.grant")) {
            sender.sendMessage(ChatColor.RED + "Нет прав!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Использование: /grantbase <игрок> <уровень>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Игрок не найден!");
            return;
        }
        int level;
        try {
            level = Integer.parseInt(args[1]);
            if (level < 1 || level > plugin.getBaseManager().getMaxLevel()) {
                sender.sendMessage(ChatColor.RED + "Уровень от 1 до " + plugin.getBaseManager().getMaxLevel());
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Уровень должен быть числом!");
            return;
        }

        plugin.getBaseManager().grantBase(target.getUniqueId(), level);
        sender.sendMessage(ChatColor.GREEN + "База уровня " + level + " выдана игроку " + target.getName());
        target.sendMessage(ChatColor.GREEN + "Администратор выдал вам базу " + level + " уровня!");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("basesplugin.admin.del")) {
            sender.sendMessage(ChatColor.RED + "Нет прав!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Использование: /delbase <игрок> <причина>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Игрок не найден!");
            return;
        }
        if (!plugin.getBaseManager().hasBase(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "У этого игрока нет базы!");
            return;
        }

        StringBuilder reason = new StringBuilder();
        for (int i = 1; i < args.length; i++) reason.append(args[i]).append(" ");

        plugin.getBaseManager().deleteBase(target.getUniqueId());
        sender.sendMessage(ChatColor.GREEN + "База игрока " + target.getName() + " удалена. Причина: " + reason.toString().trim());
        target.sendMessage(ChatColor.RED + "Ваша база удалена администратором. Причина: " + ChatColor.YELLOW + reason.toString().trim());
    }

    private void handleUpgrade(CommandSender sender, String[] args) {
        if (!sender.hasPermission("basesplugin.admin.upgrade")) {
            sender.sendMessage(ChatColor.RED + "Нет прав!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Использование: /upgradebase <игрок> <уровень>");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Игрок не найден!");
            return;
        }
        if (!plugin.getBaseManager().hasBase(target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "У этого игрока нет базы!");
            return;
        }
        int level;
        try {
            level = Integer.parseInt(args[1]);
            if (level < 1 || level > plugin.getBaseManager().getMaxLevel()) {
                sender.sendMessage(ChatColor.RED + "Уровень от 1 до " + plugin.getBaseManager().getMaxLevel());
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Уровень должен быть числом!");
            return;
        }

        int current = plugin.getBaseManager().getBase(target.getUniqueId()).getLevel();
        if (current > level) {
            sender.sendMessage(ChatColor.RED + "У игрока база получше твой рухляди хахах");
            return;
        }
        if (current == level) {
            sender.sendMessage(ChatColor.YELLOW + "У игрока уже такой уровень базы!");
            return;
        }

        plugin.getBaseManager().setBaseLevel(target.getUniqueId(), level);
        sender.sendMessage(ChatColor.GREEN + "База игрока " + target.getName() + " установлена на уровень " + level);
        target.sendMessage(ChatColor.GREEN + "Администратор изменил уровень вашей базы на " + level);
    }
}
