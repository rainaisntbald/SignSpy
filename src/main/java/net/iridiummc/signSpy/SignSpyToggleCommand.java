package net.iridiummc.signSpy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SignSpyToggleCommand implements CommandExecutor {

    private final SignSpy plugin;

    public SignSpyToggleCommand(SignSpy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Command can only be executed by a player.");
            return true;
        }
        if (!player.hasPermission("signspy.mod")) {
            player.sendMessage("You do not have permission to execute this command.");
            return true;
        }
        boolean nowDisabled = plugin.toggleNotifications(player);
        if (nowDisabled) {
            player.sendMessage("SignSpy notifications disabled.");
        } else {
            player.sendMessage("SignSpy notifications enabled.");
        }
        return true;
    }
}