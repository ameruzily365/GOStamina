package org.go.gostamina;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.go.gostamina.command.StaminaCommand;
import org.go.gostamina.config.Settings;
import org.go.gostamina.listener.StaminaListener;
import org.go.gostamina.papi.StaminaExpansion;
import org.go.gostamina.service.StaminaManager;
import org.go.gostamina.storage.SQLiteStorage;

import java.sql.SQLException;

public final class GOStaminaPlugin extends JavaPlugin {
    private Settings settings;
    private SQLiteStorage storage;
    private StaminaManager staminaManager;

    @Override public void onEnable() {
        settings = new Settings(this);
        settings.reload();
        storage = new SQLiteStorage(this);
        try { storage.open(); } catch (SQLException e) { getLogger().severe("Unable to open SQLite storage: " + e.getMessage()); getServer().getPluginManager().disablePlugin(this); return; }
        staminaManager = new StaminaManager(this);
        StaminaListener listener = new StaminaListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        listener.startContinuousTasks();
        staminaManager.startTasks();
        StaminaCommand command = new StaminaCommand(this);
        getCommand("stamina").setExecutor(command);
        getCommand("stamina").setTabCompleter(command);
        Bukkit.getOnlinePlayers().forEach(staminaManager::load);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) new StaminaExpansion(this).register();
    }

    @Override public void onDisable() {
        if (staminaManager != null) staminaManager.saveAll();
        if (storage != null) try { storage.close(); } catch (SQLException e) { getLogger().warning("Failed to close SQLite storage: " + e.getMessage()); }
    }

    public Settings settings() { return settings; }
    public SQLiteStorage storage() { return storage; }
    public StaminaManager staminaManager() { return staminaManager; }
}
