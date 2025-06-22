package net.iridiummc.signSpy;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.Location;

public class SignListener implements Listener {

    private final SignSpy plugin;

    /**
     * Constructor for the SignListener.
     * @param plugin The main plugin instance.
     */
    public SignListener(SignSpy plugin) {
        this.plugin = plugin;
    }

    /**
     * This method listens for the SignChangeEvent, which is fired
     * whenever a sign is placed or edited by a player.
     *
     * @param event The SignChangeEvent instance.
     */
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        StringBuilder signContentBuilder = new StringBuilder();
        boolean firstLine = true;
        for (String line : event.getLines()) {
            if (!firstLine) {
                signContentBuilder.append(" | ");
            }
            signContentBuilder.append(line);
            firstLine = false;
        }
        String signContent = signContentBuilder.toString();

        TextComponent message = Component.text()
                .append(Component.text("[SignSpy] ", NamedTextColor.GOLD))
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" placed/edited a sign at ", NamedTextColor.WHITE))
                .append(Component.text()
                        .append(Component.text(loc.getWorld().getName() + ", X:", NamedTextColor.AQUA))
                        .append(Component.text(loc.getBlockX(), NamedTextColor.AQUA))
                        .append(Component.text(" Y:", NamedTextColor.AQUA))
                        .append(Component.text(loc.getBlockY(), NamedTextColor.AQUA))
                        .append(Component.text(" Z:", NamedTextColor.AQUA))
                        .append(Component.text(loc.getBlockZ(), NamedTextColor.AQUA))
                        .clickEvent(ClickEvent.runCommand("/tp " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to teleport to sign", NamedTextColor.GRAY)))
                        .build())
                .append(Component.text(" with content: ", NamedTextColor.WHITE))
                .append(Component.text("\"", NamedTextColor.GREEN))
                .append(Component.text(signContent, NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(Component.text(signContent, NamedTextColor.LIGHT_PURPLE))))
                .append(Component.text("\"", NamedTextColor.GREEN))
                .build();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("signspy.mod")) {
                onlinePlayer.sendMessage(message);
            }
        }

        plugin.getLogger().info(player.getName() + " placed/edited a sign at " +
                loc.getWorld().getName() + " X:" + loc.getBlockX() +
                " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ() +
                " with content: \"" + signContent + "\"");

        if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
            String channelId = DiscordSRV.config().getString("Channels.global");
            TextChannel chatChannel = DiscordSRV.getPlugin().getJda().getTextChannelById(channelId);

            if (chatChannel != null) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("Player " + player.getName() + "'s sign: " + signContent
                        .replace("|", "\\|")
                        .replace("*", "\\*")
                        .replace("_", "\\_")
                        .replace("`", "\\`")
                );
                embed.setColor(0x00AAFF);
                chatChannel.sendMessageEmbeds(embed.build()).queue();
            }
        }
    }
}
