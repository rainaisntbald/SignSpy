package net.iridiummc.signSpy;

import org.bukkit.plugin.java.JavaPlugin;

public class SignSpy extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("SignSpy has been enabled!");

        getServer().getPluginManager().registerEvents(new SignListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("SignSpy has been disabled!");
    }
}
