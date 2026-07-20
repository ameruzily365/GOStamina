package org.go.gostamina.storage;

import org.go.gostamina.GOStaminaPlugin;
import org.go.gostamina.data.StaminaData;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public final class SQLiteStorage implements AutoCloseable {
    private final GOStaminaPlugin plugin;
    private Connection connection;

    public SQLiteStorage(GOStaminaPlugin plugin) { this.plugin = plugin; }

    public void open() throws SQLException {
        plugin.getDataFolder().mkdirs();
        connection = DriverManager.getConnection("jdbc:sqlite:" + new File(plugin.getDataFolder(), "stamina.db"));
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_stamina (player_uuid TEXT PRIMARY KEY, current_stamina INTEGER NOT NULL, base_max_stamina INTEGER NOT NULL, bonus_max_stamina INTEGER NOT NULL)");
        }
    }

    public synchronized StaminaData load(UUID uuid) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT current_stamina, base_max_stamina, bonus_max_stamina FROM player_stamina WHERE player_uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new StaminaData(uuid, rs.getInt(1), rs.getInt(2), rs.getInt(3));
            }
        }
        int base = plugin.getConfig().getInt("defaults.base-max-stamina", 20);
        int bonus = plugin.getConfig().getInt("defaults.bonus-max-stamina", 0);
        int current = plugin.getConfig().getInt("defaults.current-stamina", base + bonus);
        return new StaminaData(uuid, current, base, bonus);
    }

    public synchronized void save(StaminaData data) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO player_stamina(player_uuid,current_stamina,base_max_stamina,bonus_max_stamina) VALUES(?,?,?,?) ON CONFLICT(player_uuid) DO UPDATE SET current_stamina=excluded.current_stamina, base_max_stamina=excluded.base_max_stamina, bonus_max_stamina=excluded.bonus_max_stamina")) {
            ps.setString(1, data.uuid().toString());
            ps.setInt(2, data.currentStamina());
            ps.setInt(3, data.baseMaxStamina());
            ps.setInt(4, data.bonusMaxStamina());
            ps.executeUpdate();
            data.clearDirty();
        }
    }

    @Override public synchronized void close() throws SQLException { if (connection != null) connection.close(); }
}
