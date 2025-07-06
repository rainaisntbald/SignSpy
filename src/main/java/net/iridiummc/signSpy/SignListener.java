package net.iridiummc.signSpy;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.Location;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class SignListener implements Listener {

    private final SignSpy plugin;
    private static final String CHANNEL_NAME = "signspy";
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final boolean redisEnabled;
    private final boolean ignoreSignShop;

    /**
     * Constructor for the SignListener.
     * @param plugin The main plugin instance.
     */
    public SignListener(SignSpy plugin, String host, int port, boolean redisEnabled, String username, String password, boolean ignoreSignShop) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.redisEnabled = redisEnabled;
        this.ignoreSignShop = ignoreSignShop;

        if(redisEnabled) {
            new Thread(this::subscribe).start();
        }
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

        String playerName = player.getName();
        String worldName = loc.getWorld().getName();
        String x = loc.getBlockX() + "";
        String y = loc.getBlockY() + "";
        String z = loc.getBlockZ() + "";
        String signContent = signContentBuilder.toString();
        if(ignoreSignShop &&
                PlainTextComponentSerializer.plainText().serialize(event.lines().get(0)).equals(playerName) &&
                NumberUtils.isCreatable(PlainTextComponentSerializer.plainText().serialize(event.lines().get(1)))) {
            return;
        }

        if(redisEnabled) {
            publish(playerName, worldName, x, y, z, signContent);
            return;
        }
        displaySignMessage(playerName, worldName, x, y, z, signContent);
    }

    private void displaySignMessage(String playerName, String worldName, String x, String y, String z, String signContent) {
        TextComponent message = Component.text()
                .append(Component.text("[SignSpy] ", NamedTextColor.GOLD))
                .append(Component.text(playerName, NamedTextColor.YELLOW))
                .append(Component.text(" placed/edited a sign at ", NamedTextColor.WHITE))
                .append(Component.text()
                        .append(Component.text(worldName + ", X:", NamedTextColor.AQUA))
                        .append(Component.text(x, NamedTextColor.AQUA))
                        .append(Component.text(" Y:", NamedTextColor.AQUA))
                        .append(Component.text(y, NamedTextColor.AQUA))
                        .append(Component.text(" Z:", NamedTextColor.AQUA))
                        .append(Component.text(z, NamedTextColor.AQUA))
                        .clickEvent(ClickEvent.runCommand("/tp " + x + " " + y + " " + z))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to teleport to sign", NamedTextColor.GRAY)))
                        .build())
                .append(Component.text(" with content: ", NamedTextColor.WHITE))
                .append(Component.text("\"", NamedTextColor.GREEN))
                .append(Component.text(signContent, NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(Component.text(signContent, NamedTextColor.LIGHT_PURPLE))))
                .append(Component.text("\"", NamedTextColor.GREEN))
                .build();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("signspy.mod") && !plugin.isNotificationsDisabled(onlinePlayer)) {
                onlinePlayer.sendMessage(message);
            }
        }

        plugin.getLogger().info(playerName + " placed/edited a sign at " +
                worldName + " X:" + x +
                " Y:" + y + " Z:" + z +
                " with content: \"" + signContent + "\"");

        if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
            String channelId = DiscordSRV.config().getString("Channels.global");
            TextChannel chatChannel = DiscordSRV.getPlugin().getJda().getTextChannelById(channelId);

            if (chatChannel != null) {
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("Player " + playerName + "'s sign: " + signContent
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

    public void subscribe() {
        DefaultJedisClientConfig defaultJedisClientConfig = DefaultJedisClientConfig.builder().build();
        if(!username.isBlank() || !password.isBlank()) {
            defaultJedisClientConfig = DefaultJedisClientConfig.builder().user(username).password(password).build();
        }

        try (Jedis jedis = new Jedis(host, port, defaultJedisClientConfig)) {
            jedis.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    if(!channel.equals(CHANNEL_NAME)) return; // Probably useless
                    String[] split = message.split("#", 6);
                    displaySignMessage(split[0], split[1], split[2], split[3], split[4], split[5]);
                }
            }, CHANNEL_NAME);
        }
    }

    public void publish(String playerName, String worldName, String x, String y, String z, String signContent) {
        String message = playerName + "#" + worldName + "#" + x + "#" + y + "#" + z + "#" + signContent;

        DefaultJedisClientConfig defaultJedisClientConfig = DefaultJedisClientConfig.builder().build();
        if(!username.isBlank() && !password.isBlank()) {
            defaultJedisClientConfig = DefaultJedisClientConfig.builder().user(username).password(password).build();
        }

        try (Jedis jedis = new Jedis(host, port, defaultJedisClientConfig)) {
            jedis.publish(CHANNEL_NAME, message);
        }
    }
}
