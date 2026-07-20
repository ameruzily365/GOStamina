package org.go.gostamina.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.go.gostamina.GOStaminaPlugin;
import org.go.gostamina.data.StaminaData;

public final class StaminaExpansion extends PlaceholderExpansion {
    private final GOStaminaPlugin plugin;
    public StaminaExpansion(GOStaminaPlugin plugin) { this.plugin = plugin; }
    @Override public String getIdentifier() { return "stamina"; }
    @Override public String getAuthor() { return "GOStamina"; }
    @Override public String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";
        StaminaData data = plugin.staminaManager().get(player);
        if (data == null) return "0";
        return switch (params.toLowerCase()) {
            case "current" -> Integer.toString(data.currentStamina());
            case "max" -> Integer.toString(data.maximumStamina());
            case "percentage_round" -> Integer.toString(data.maximumStamina() == 0 ? 0 : Math.round(data.currentStamina() * 100f / data.maximumStamina()));
            case "percentage" -> String.format(java.util.Locale.US, "%.1f", data.maximumStamina() == 0 ? 0d : data.currentStamina() * 100d / data.maximumStamina());
            case "caffeine" -> Boolean.toString(plugin.staminaManager().hasCaffeine(player));
            case "caffeine_timer" -> formatDuration(plugin.staminaManager().caffeineRemainingMillis(player));
            case "caffeine_level" -> Integer.toString(plugin.staminaManager().caffeineLevel(player));
            default -> null;
        };
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return hours + ":" + minutes + ":" + seconds;
    }
}
