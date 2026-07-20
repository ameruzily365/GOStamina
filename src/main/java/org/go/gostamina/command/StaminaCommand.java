package org.go.gostamina.command;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.go.gostamina.GOStaminaPlugin;
import org.go.gostamina.data.StaminaData;

import java.util.*;

public final class StaminaCommand implements CommandExecutor, TabCompleter {
    private final GOStaminaPlugin plugin;
    public StaminaCommand(GOStaminaPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("stamina.admin")) { sender.sendMessage("§cNo permission."); return true; }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            sender.sendMessage("§aGOStamina reloaded.");
            return true;
        }
        if (args.length < 3) return usage(sender);
        Player target = Bukkit.getPlayerExact(args[0].equalsIgnoreCase("max") ? args[2] : args[1]);
        if (target == null) { sender.sendMessage("§cPlayer not found or data not loaded."); return true; }
        StaminaData data = plugin.staminaManager().get(target);
        if (data == null) { sender.sendMessage("§cPlayer stamina data is still loading."); return true; }
        try {
            if (args[0].equalsIgnoreCase("set") && args.length == 3) data.setCurrentStamina(parse(args[2]));
            else if (args[0].equalsIgnoreCase("add") && args.length == 3) data.addCurrentStamina(parse(args[2]));
            else if (args[0].equalsIgnoreCase("remove") && args.length == 3) data.addCurrentStamina(-parse(args[2]));
            else if (args[0].equalsIgnoreCase("delay") && args.length == 3) plugin.staminaManager().lockRegeneration(target, parse(args[2]));
            else if (args[0].equalsIgnoreCase("max") && args.length == 4) {
                int amount = parse(args[3]);
                if (args[1].equalsIgnoreCase("set")) data.setBaseMaxStamina(amount);
                else if (args[1].equalsIgnoreCase("add")) data.addBonusMaxStamina(amount);
                else if (args[1].equalsIgnoreCase("remove")) data.addBonusMaxStamina(-amount);
                else return usage(sender);
            } else return usage(sender);
            sender.sendMessage("§aUpdated " + target.getName() + ": " + data.currentStamina() + "/" + data.maximumStamina());
        } catch (NumberFormatException e) { sender.sendMessage("§cAmount must be an integer."); }
        return true;
    }

    private int parse(String value) { return Math.max(0, Integer.parseInt(value)); }
    private boolean usage(CommandSender sender) { sender.sendMessage("§e/stamina set|add|remove <player> <amount>\n/stamina max set|add|remove <player> <amount>\n/stamina delay <player> <seconds>\n/stamina reload"); return true; }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) { return List.of(); }
}
