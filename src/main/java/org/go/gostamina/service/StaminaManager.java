package org.go.gostamina.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.go.gostamina.GOStaminaPlugin;
import org.go.gostamina.data.StaminaData;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class StaminaManager {
    private final GOStaminaPlugin plugin;
    private final Map<UUID, StaminaData> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> regenLockedUntil = new ConcurrentHashMap<>();

    public StaminaManager(GOStaminaPlugin plugin) { this.plugin = plugin; }

    public StaminaData get(UUID uuid) { return cache.get(uuid); }
    public StaminaData get(Player player) { return cache.get(player.getUniqueId()); }

    public void load(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                StaminaData data = plugin.storage().load(player.getUniqueId());
                Bukkit.getScheduler().runTask(plugin, () -> cache.put(player.getUniqueId(), data));
            } catch (SQLException e) { plugin.getLogger().severe("Failed to load stamina for " + player.getName() + ": " + e.getMessage()); }
        });
    }

    public void unload(Player player) { StaminaData data = cache.remove(player.getUniqueId()); if (data != null) save(data); }
    public void saveDirty() { cache.values().stream().filter(StaminaData::dirty).forEach(this::save); }
    public void saveAll() { cache.values().forEach(this::saveSync); }
    private void save(StaminaData data) { Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveSync(data)); }
    public void saveSync(StaminaData data) { try { plugin.storage().save(data); } catch (SQLException e) { plugin.getLogger().severe("Failed to save stamina: " + e.getMessage()); } }

    public boolean consume(Player player, int amount) {
        StaminaData data = get(player);
        if (data == null || amount <= 0) return true;
        lockRegeneration(player, plugin.getConfig().getInt("regeneration.delay-seconds", 5));
        if (data.currentStamina() < amount) { data.setCurrentStamina(0); return false; }
        data.addCurrentStamina(-amount);
        return true;
    }

    public void restore(Player player, int amount) { StaminaData data = get(player); if (data != null && amount > 0) data.addCurrentStamina(amount); }
    public boolean hasStamina(Player player) { StaminaData data = get(player); return data == null || data.currentStamina() > 0; }
    public void lockRegeneration(Player player, int seconds) { regenLockedUntil.put(player.getUniqueId(), System.currentTimeMillis() + seconds * 1000L); }
    public boolean canRegenerate(Player player) { return System.currentTimeMillis() >= regenLockedUntil.getOrDefault(player.getUniqueId(), 0L); }

    public void startTasks() {
        int regenInterval = Math.max(1, plugin.getConfig().getInt("regeneration.interval-ticks", 20));
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!plugin.getConfig().getBoolean("regeneration.enabled", true)) return;
            int amount = plugin.getConfig().getInt("regeneration.amount-per-tick", 1);
            for (Player player : Bukkit.getOnlinePlayers()) if (canRegenerate(player)) restore(player, amount);
        }, regenInterval, regenInterval);
        int saveTicks = Math.max(20, plugin.getConfig().getInt("storage.save-interval-seconds", 120) * 20);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveDirty, saveTicks, saveTicks);
    }
}
