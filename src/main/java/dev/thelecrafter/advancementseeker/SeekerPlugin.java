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
        getConfig().setInlineComments("start-unix-timestamp", List.of("Edit this when server is stopped"));
        getConfig().setInlineComments("data", List.of("Do not edit!"));
        saveConfig();
        getLogger().info("Loading start date...");
        startDate = new Date(getConfig().getLong("start-unix-timestamp")).toInstant().atZone(ZoneId.of("Europe/Berlin")).toLocalDateTime();
        getLogger().info("Start date is " + startDate.toString() + " [Past: " + (LocalDateTime.now().isAfter(startDate) ? "Yes" : "No") + "]");
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPreJoin(AsyncPlayerPreLoginEvent event) {
        if (startDate.isAfter(LocalDateTime.now())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component
                            .text("Hey! Nicht so schnell! :)")
                            .color(NamedTextColor.RED)
                    );
        }
    }

    @EventHandler
    public void formatChat(AsyncChatEvent event) {
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            if (advancements.size() == advancements.stream().filter(advancement -> source.getAdvancementProgress(advancement).isDone()).count()) {
                return sourceDisplayName.append(Component.text(": ").color(NamedTextColor.DARK_GRAY)).append(message.colorIfAbsent(NamedTextColor.GOLD));
            } else return sourceDisplayName.append(Component.text(": ").color(NamedTextColor.DARK_GRAY)).append(message.colorIfAbsent(NamedTextColor.WHITE));
        });
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerAdvancementSync(event.getPlayer());
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        playerAdvancementSync(event.getPlayer());
    }

    void playerAdvancementSync(Player player) {
        long advancementsDone =  advancements.stream().filter(advancement -> player.getAdvancementProgress(advancement).isDone()).count();
        TextColor color;
        if (advancements.size() == advancementsDone) {
            color = NamedTextColor.GOLD;
            if (getConfig().getInt("data." + player.getUniqueId() + ".lastLevelMessage") < advancementsDone) {
                getConfig().set("data." + player.getUniqueId() + ".lastLevelMessage", advancementsDone);
                saveConfig();
                player.sendMessage(Component.text("Du hast das maximale Level erreicht und neue Verzauberungen freigeschaltet! Glückwunsch!")
                        .append(Component.newline())
                        .append(Component.text("Verzauberungen:"))
                        .append(Component.newline())
                        .append(Component.text("    Protection VI"))
                        .append(Component.newline())
                        .append(Component.text("    Sharpness VII"))
                        .append(Component.newline())
                        .append(Component.text("    Efficiency VII"))
                        .append(Component.newline())
                        .append(Component.text("    Fortune IV"))
                        .color(NamedTextColor.GOLD)
                );
                Bukkit.broadcast(player.displayName().append(Component.text(" hat das maximale Level erreicht!").color(NamedTextColor.GOLD)));
            }
        }
        else {
            color = colorList.get((int) Math.floorDiv(advancementsDone, 250L));
            for (int i = 1000; i >= 250; i = i-250) {
                if (advancementsDone >= i && getConfig().getInt("data." + player.getUniqueId() + ".lastLevelMessage") < i) {
                    getConfig().set("data." + player.getUniqueId() + ".lastLevelMessage", i);
                    saveConfig();
                    String enchantment;
                    switch (i) {
                        case 1000 -> enchantment = "Protection V";
                        case 750 -> enchantment = "Sharpness VI";
                        case 500 -> enchantment = "Efficiency VI";
                        case 250 -> enchantment = "Fortune III";
                        default -> enchantment = "Invalid Level. Report to TheLeCrafter";
                    }
                    player.sendMessage(Component.text("Du hast Level " + i + " erreicht und eine neue Verzauberung freigeschaltet!")
                            .color(NamedTextColor.GOLD)
                            .append(Component.newline())
                            .append(Component.text("    Verzauberung: " + enchantment))
                    );
                }
            }
        }
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
                        .text("                       ᴄʀᴀꜰᴛᴇʀ ꜱᴍᴘ")
                        .color(NamedTextColor.GOLD)
        );
        if (LocalDateTime.now().isBefore(startDate)) {
            long difference = ChronoUnit.MINUTES.between(LocalDateTime.now(), startDate);
            long daysUntil = Math.floorDiv(difference, 60*24);
            long hoursUntil = Math.floorDiv(difference - (60*24*daysUntil), 60);
            long minutesUntil = difference - ((hoursUntil)*60) - (daysUntil*60*24);
            event.setVersion("ѕᴏᴏɴᴛᴍ");
            event.setProtocolVersion(Integer.MAX_VALUE);
            event.motd(
                    event.motd()
                            .append(Component.newline())
                            .append(Component
                                    .text("        ɴᴏᴄʜ " + daysUntil + " ᴛᴀɢᴇ " + hoursUntil + " ѕᴛᴜɴᴅᴇɴ " + minutesUntil + " ᴍɪɴᴜᴛᴇɴ")
                                    .color(NamedTextColor.RED)
                            )
            );
        } else {
            event.motd(
                    event.motd()
                            .append(Component.newline())
                            .append(Component
                                    .text("                      ɴᴏᴡ ʀᴇʟᴇᴀѕᴇᴅ")
                                    .color(NamedTextColor.YELLOW)

                            )
            );
        }
    }
}
