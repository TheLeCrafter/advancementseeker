package dev.thelecrafter.advancementseeker;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class SeekerPlugin extends JavaPlugin implements Listener {

    private final ArrayList<Advancement> advancements = new ArrayList<>();
    private LocalDateTime startDate = null;
    private final List<TextColor> colorList = List.of(NamedTextColor.WHITE, NamedTextColor.GREEN, NamedTextColor.BLUE, NamedTextColor.LIGHT_PURPLE, NamedTextColor.AQUA);

    @Override
    public void onEnable() {
        getLogger().info("Loading advancements...");
        Bukkit.advancementIterator().forEachRemaining(advancements::add);
        getLogger().info("Loaded " + advancements.size() + " advancements!");
        saveDefaultConfig();
        getLogger().info("Loading start date...");
        startDate = new Date(getConfig().getLong("start-unix-timestamp")).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        getLogger().info("Start date is " + startDate.toString() + " [Past: " + (LocalDateTime.now().isAfter(startDate) ? "Yes" : "No") + "]");
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPreJoin(AsyncPlayerPreLoginEvent event) {
        if (startDate.isAfter(LocalDateTime.now())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("Not yet released!").color(NamedTextColor.RED));
        }
    }

    @EventHandler
    public void formatChat(AsyncChatEvent event) {
        event.renderer((source, sourceDisplayName, message, viewer) -> sourceDisplayName.append(Component.text(": ").color(NamedTextColor.DARK_GRAY)).append(message.colorIfAbsent(NamedTextColor.WHITE)));
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        formatPlayer(event.getPlayer());
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        formatPlayer(event.getPlayer());
    }

    void formatPlayer(Player player) {
        long advancementsDone =  advancements.stream().filter(advancement -> player.getAdvancementProgress(advancement).isDone()).count();
        TextColor color;
        if (advancements.size() == advancementsDone) color = NamedTextColor.GOLD;
        else color = colorList.get((int) Math.floorDiv(advancementsDone, 250L));
        Component newName = Component.text("["+advancementsDone+"] ").append(player.name()).color(color);
        player.displayName(newName);
        player.playerListName(newName);
    }

    @EventHandler
    public void onServerListPing(PaperServerListPingEvent event) {
        if (event.getClient().isLegacy() || event.getClient().getProtocolVersion() < 393) {
            event.setMotd("\u00a74Legacy Client version!\nUpdate to " + getServer().getMinecraftVersion());
            event.setVersion("Use " + getServer().getMinecraftVersion());
            return;
        }
        if (event.getClient().getProtocolVersion() != event.getProtocolVersion()) {
            event.setVersion("Use " + getServer().getMinecraftVersion());
            event.setProtocolVersion(Integer.MAX_VALUE);
            return;
        }
        event.motd(
                Component
                        .text("                       \u1d04\u0280\u1d00\ua730\u1d1b\u1d07\u0280 \ua731\u1d0d\u1d18")
                        .color(NamedTextColor.GOLD)
        );
        if (LocalDateTime.now().isBefore(startDate)) {
            long difference = ChronoUnit.MINUTES.between(LocalDateTime.now(), startDate);
            long daysUntil = Math.floorDiv(difference, 60*24)-1;
            long hoursUntil = Math.floorDiv(difference - (60*24*daysUntil), 60)-1;
            long minutesUntil = difference - ((hoursUntil+1)*60) - (daysUntil*60*24);
            event.setVersion("\u0455\u1d0f\u1d0f\u0274\u1d1b\u1d0d");
            event.setProtocolVersion(Integer.MAX_VALUE);
            event.motd(
                    event.motd()
                            .append(Component.newline())
                            .append(Component
                                    .text("        \u0274\u1d0f\u1d04\u029c " + daysUntil + " \u1d1b\u1d00\u0262\u1d07 " + hoursUntil + " \u0455\u1d1b\u1d1c\u0274\u1d05\u1d07\u0274 " + minutesUntil + " \u1d0d\u026a\u0274\u1d1c\u1d1b\u1d07\u0274")
                                    .color(NamedTextColor.RED)
                            )
            );
        } else {
            event.motd(
                    event.motd()
                            .append(Component.newline())
                            .append(Component
                                    .text("                      \u0274\u1d0f\u1d21 \u0280\u1d07\u029f\u1d07\u1d00\u0455\u1d07\u1d05")
                                    .color(NamedTextColor.YELLOW)

                            )
            );
        }
    }
}
