package dev.thelecrafter.advancementseeker;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
    public void formatChat(@NotNull AsyncChatEvent event) {
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            if (advancements.size() == advancements.stream().filter(advancement -> source.getAdvancementProgress(advancement).isDone()).count()) {
                return sourceDisplayName.append(Component.text(": ").color(NamedTextColor.DARK_GRAY)).append(message.colorIfAbsent(NamedTextColor.GOLD));
            } else return sourceDisplayName.append(Component.text(": ").color(NamedTextColor.DARK_GRAY)).append(message.colorIfAbsent(NamedTextColor.WHITE));
        });
    }
    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        playerAdvancementSync(event.getPlayer());
        event.getPlayer().sendPlayerListHeaderAndFooter(Component.text("ᴄʀᴀꜰᴛᴇʀ ꜱᴍᴘ").color(NamedTextColor.GOLD), Component.text("ᴘʟᴀʏ.ᴛʜᴇʟᴇᴄʀᴀꜰᴛᴇʀ.ᴅᴇᴠ").color(NamedTextColor.YELLOW));
    }

    @EventHandler
    public void onAdvancementDone(@NotNull PlayerAdvancementDoneEvent event) {
        playerAdvancementSync(event.getPlayer());
    }

    void playerAdvancementSync(Player player) {
        long advancementsDone = advancements.stream().filter(advancement -> player.getAdvancementProgress(advancement).isDone()).count();
        TextColor color;
        boolean broadcast = false;
        if (advancements.size() == advancementsDone) {
            color = NamedTextColor.GOLD;
            if (getConfig().getInt("data." + player.getUniqueId() + ".lastLevelMessage") < advancementsDone) {
                getConfig().set("data." + player.getUniqueId() + ".lastLevelMessage", advancementsDone);
                saveConfig();
                player.sendMessage(Component.text("Du hast das maximale Level erreicht und neue Verzauberungen freigeschaltet! Glückwunsch!")
                        .append(Component.newline())
                        .append(Component.text("Verzauberungen:"))
                        .append(Component.newline())
                        .append(MiniMessage.miniMessage().deserialize("    <rainbow>Protection VI</rainbow>"))
                        .append(Component.newline())
                        .append(MiniMessage.miniMessage().deserialize("    <rainbow>Sharpness VII</rainbow>"))
                        .append(Component.newline())
                        .append(MiniMessage.miniMessage().deserialize("    <rainbow>Efficiency VII</rainbow>"))
                        .append(Component.newline())
                        .append(MiniMessage.miniMessage().deserialize("    <rainbow>Fortune IV</rainbow>"))
                        .color(NamedTextColor.GOLD)
                );
                broadcast = true;
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
                        case 250 -> enchantment = "Fortune V";
                        default -> enchantment = "Invalid Level. Report to TheLeCrafter";
                    }
                    player.sendMessage(Component.text("Du hast Level " + i + " erreicht und eine neue Verzauberung freigeschaltet!")
                            .color(NamedTextColor.GOLD)
                            .append(Component.newline())
                            .append(Component.text("    Verzauberung: ").append(MiniMessage.miniMessage().deserialize("<rainbow>" + enchantment)))
                    );
                }
            }
        }
        Component newName = Component.text("["+advancementsDone+"] ").append(player.name()).color(color);
        player.displayName(newName);
        player.playerListName(newName);
        if (broadcast) Bukkit.broadcast(player.displayName().append(Component.text(" hat das maximale Level erreicht!").color(NamedTextColor.GOLD)));
    }

    @EventHandler
    public void onServerListPing(@NotNull PaperServerListPingEvent event) {
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
            event.setMaxPlayers(0);
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

    @EventHandler
    public void setAnvilRecipes(@NotNull PrepareAnvilEvent event) {
        if (event.getInventory().getFirstItem() == null || event.getInventory().getSecondItem() == null || !event.getInventory().getSecondItem().hasItemMeta()) return;
        if (event.getResult() == null) return;
        if (vanillaHandling(getEnchantsOfItem(event.getInventory().getFirstItem()))) {
            if (vanillaHandling(getEnchantsOfItem(event.getInventory().getSecondItem()))) return;
            AtomicBoolean isVanilla = new AtomicBoolean(true);
            getEnchantsOfItem(event.getInventory().getSecondItem()).forEach((enchantment, level) -> {
                if (enchantment.getMaxLevel() < level) isVanilla.set(false);
            });
            if (isVanilla.get()) return;
        }
        if (event.getViewers().stream().anyMatch(human -> isApplicable((Player) human, getEnchantsOfItem(event.getInventory().getSecondItem()), event.getInventory().getFirstItem()))) {
            Map<Enchantment, Integer> enchantsToAdd = new HashMap<>();
            getEnchantsOfItem(event.getInventory().getFirstItem()).forEach((enchantment, level) -> {
                if (possibleEnchantments.contains(enchantment) && Objects.equals(level, getEnchantsOfItem(event.getInventory().getSecondItem()).get(enchantment)) && level < enchantment.getMaxLevel() + 2) {
                    enchantsToAdd.put(enchantment, level+1);
                } else enchantsToAdd.put(enchantment, level);
            });
            getEnchantsOfItem(event.getInventory().getSecondItem()).forEach((enchantment, level) -> {
                if (getEnchantsOfItem(event.getInventory().getFirstItem()).containsKey(enchantment)) {
                    if (level > getEnchantsOfItem(event.getInventory().getFirstItem()).get(enchantment)) {
                        enchantsToAdd.put(enchantment, level);
                    }
                } else enchantsToAdd.put(enchantment, level);
            });
            ItemStack result = addEnchantsToItem(event.getInventory().getFirstItem(), enchantsToAdd);
            result.editMeta(meta ->{
                if (event.getInventory().getRenameText() != null && !event.getInventory().getRenameText().isBlank()) {
                    meta.displayName(Component.text(event.getInventory().getRenameText()));
                }
            });
            event.setResult(result);
            event.getInventory().setRepairCost(30);
        }
    }

    private final List<Enchantment> possibleEnchantments = List.of(Enchantment.LOOT_BONUS_BLOCKS, Enchantment.DIG_SPEED, Enchantment.DAMAGE_ALL, Enchantment.PROTECTION_ENVIRONMENTAL);

    public Map<Enchantment, Integer> getEnchantsOfItem(ItemStack item) {
        if (!item.hasItemMeta()) return Map.of();
        if (item.getItemMeta() instanceof EnchantmentStorageMeta meta) return meta.getStoredEnchants();
        else return item.getItemMeta().getEnchants();
    }

    public @NotNull ItemStack addEnchantsToItem(@NotNull ItemStack item, Map<Enchantment, Integer> enchantments) {
        ItemStack cloned = item.clone();
        if (!cloned.hasItemMeta() || !(cloned.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
            cloned.addUnsafeEnchantments(enchantments);
        } else {
            enchantments.forEach((enchantment, level) -> meta.addStoredEnchant(enchantment, level, true));
            cloned.setItemMeta(meta);
        }
        return cloned;
    }

    private boolean vanillaHandling(@NotNull Map<Enchantment, Integer> enchantmentLevels) {
        AtomicBoolean vanilla = new AtomicBoolean(true);
        enchantmentLevels.forEach((enchantment, level) -> {
            if (possibleEnchantments.contains(enchantment) && (enchantment.getMaxLevel() == level || enchantment.getMaxLevel() + 1 == level || enchantment.getMaxLevel() + 2 == level)) {
                vanilla.set(false);
            }
        });
        return vanilla.get();
    }

    private boolean isApplicable(Player player, Map<Enchantment, Integer> enchantmentLevels, ItemStack applyingTo) {
        if (applyingTo == null) return false;
        long advancementsDone = advancements.stream().filter(advancement -> player.getAdvancementProgress(advancement).isDone()).count();
        AtomicBoolean applicable = new AtomicBoolean(true);
        enchantmentLevels.forEach((enchantment, level) -> {
            if (possibleEnchantments.contains(enchantment)) {
                if (enchantment.getMaxLevel() + 1 == level) {
                    if (advancementsDone < advancements.size()) applicable.set(false);
                } else if (enchantment.getMaxLevel() == level) {
                    if (advancementsDone < (possibleEnchantments.indexOf(enchantment)+1) * 250L) applicable.set(false);
                }
            }
        });
        return applicable.get();
    }
}
