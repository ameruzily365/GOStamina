package org.go.gostamina.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.go.gostamina.GOStaminaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public final class Settings {
    private final GOStaminaPlugin plugin;
    private final Map<String, ActionSettings> actions = new HashMap<>();
    private final Map<String, Integer> foods = new HashMap<>();
    private int defaultFoodRegen;

    public Settings(GOStaminaPlugin plugin) { this.plugin = plugin; }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.saveResource("actions.yml", false);
        plugin.saveResource("foods.yml", false);
        plugin.reloadConfig();
        loadActions();
        loadFoods();
    }

    private void loadActions() {
        actions.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "actions.yml"));
        ConfigurationSection section = yml.getConfigurationSection("actions");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            String path = "actions." + key + ".";
            ActionSettings.ActionType type = ActionSettings.ActionType.valueOf(yml.getString(path + "type", "instant").toUpperCase());
            actions.put(key, new ActionSettings(yml.getBoolean(path + "enabled", true), type, yml.getInt(path + "amount", 1), Math.max(1, yml.getInt(path + "interval-seconds", 1))));
        }
    }

    private void loadFoods() {
        foods.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "foods.yml"));
        defaultFoodRegen = yml.getInt("default.regen", 10);
        ConfigurationSection section = yml.getConfigurationSection("foods");
        if (section != null) for (String key : section.getKeys(false)) foods.put(key.toLowerCase(), yml.getInt("foods." + key + ".regen", defaultFoodRegen));
    }

    public ActionSettings action(String key) { return actions.getOrDefault(key, new ActionSettings(false, ActionSettings.ActionType.INSTANT, 0, 1)); }
    public int foodRegen(String key) { return foods.getOrDefault(key.toLowerCase(), defaultFoodRegen); }
}
