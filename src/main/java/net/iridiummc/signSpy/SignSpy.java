package net.iridiummc.signSpy;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SignSpy extends JavaPlugin {

    private final Set<UUID> notificationsDisabled = new HashSet<>();

    @Override
    public void onEnable() {
        getLogger().info("SignSpy has been enabled!");
        this.saveDefaultConfig();
        FileConfiguration config = getConfig();
        String host = config.getString("redis.host");
        int port = config.getInt("redis.port");
        boolean redisEnabled = config.getBoolean("redis.enabled");
        String username = config.getString("redis.username");
        String password = config.getString("redis.password");

        getServer().getPluginManager().registerEvents(
                new SignListener(this, host, port, redisEnabled, username, password), this
        );
        // Register the toggle command
        getCommand("signspytoggle").setExecutor(new SignSpyToggleCommand(this));
    }

    @Override
    public void onDisable() {
        getLogger().info("SignSpy has been disabled!");
    }

    public boolean isNotificationsDisabled(Player player) {
        return notificationsDisabled.contains(player.getUniqueId());
    }

    /**
     * Toggles the notifications for the given player.
     *
     * @param player the player to toggle
     * @return true if notifications are now disabled, false otherwise
     */
    public boolean toggleNotifications(Player player) {
        UUID uuid = player.getUniqueId();
        if (notificationsDisabled.contains(uuid)) {
            notificationsDisabled.remove(uuid);
            return false;
        } else {
            notificationsDisabled.add(uuid);
            return true;
        }
    }
}