package de.itemlock;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ItemLockPlugin extends JavaPlugin implements Listener {

    private LockManager locks;
    private SenseItem sense;
    private final Map<UUID, LockMenu> awaitingSearch = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        locks = new LockManager(this);
        sense = new SenseItem(this);
        sense.registerRecipe();

        Bukkit.getPluginManager().registerEvents(new LockListener(this, locks), this);
        Bukkit.getPluginManager().registerEvents(new SenseListener(this, sense), this);
        Bukkit.getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!getConfig().getBoolean("remove-from-inventories")) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                purge(p.getInventory().getContents(), p.getInventory());
                purge(p.getEnderChest().getContents(), p.getEnderChest());
            }
        }, 40L, 40L);

        getLogger().info("ItemLock aktiv - " + locks.all().size() + " Items gesperrt.");
    }

    @Override
    public void onDisable() {
        if (sense != null) sense.unregisterRecipe();
    }

    private void purge(ItemStack[] contents, org.bukkit.inventory.Inventory inv) {
        for (int i = 0; i < contents.length; i++) {
            if (locks.isLocked(contents[i])) inv.setItem(i, null);
        }
    }

    public void awaitSearch(Player p, LockMenu menu) {
        awaitingSearch.put(p.getUniqueId(), menu);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof LockMenu menu)) return;
        e.setCancelled(true);
        menu.click(e);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof LockMenu) e.setCancelled(true);
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        LockMenu menu = awaitingSearch.remove(e.getPlayer().getUniqueId());
        if (menu == null) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        Bukkit.getScheduler().runTask(this, () -> {
            menu.setSearch(text.equals("-") ? "" : text);
            menu.open();
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        awaitingSearch.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!getConfig().getBoolean("resource-pack.send-on-join")) return;
        String url = getConfig().getString("resource-pack.url", "");
        if (url == null || url.isBlank()) return;
        byte[] hash = null;
        String sha1 = getConfig().getString("resource-pack.sha1", "");
        if (sha1 != null && sha1.length() == 40) {
            hash = new byte[20];
            for (int i = 0; i < 20; i++) hash[i] = (byte) Integer.parseInt(sha1.substring(i * 2, i * 2 + 2), 16);
        }
        Component prompt = Ui.c(getConfig().getString("resource-pack.prompt", ""));
        e.getPlayer().setResourcePack(UUID.nameUUIDFromBytes(("itemlock:" + url).getBytes()), url, hash, prompt, false);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (cmd.getName().equalsIgnoreCase("itemlock")) {
            if (args.length == 0) {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Nur im Spiel nutzbar. Konsole: /itemlock list | reload");
                    return true;
                }
                new LockMenu(this, locks, p).open();
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "list" -> {
                    List<String> names = new ArrayList<>();
                    for (Material m : locks.all()) names.add(m.name());
                    sender.sendMessage(Ui.c("&8[&bItemLock&8] &fGesperrt (" + names.size() + "): &c" + (names.isEmpty() ? "-" : String.join(", ", names))));
                }
                case "reload" -> {
                    reloadConfig();
                    locks.load();
                    sense.registerRecipe();
                    sender.sendMessage(Ui.c("&8[&bItemLock&8] &aNeu geladen."));
                }
                default -> sender.sendMessage(Ui.c("&cNutzung: /itemlock [list|reload]"));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("sense")) {
            if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
                Player target = null;
                if (args.length >= 2) {
                    target = Bukkit.getPlayerExact(args[1]);
                } else if (sender instanceof Player p) {
                    target = p;
                }

                if (target == null) {
                    sender.sendMessage(Ui.c("&cSpieler nicht gefunden."));
                    return true;
                }

                // Effektiv finale Referenz fuer die Lambda-Funktion.
                final Player recipient = target;
                recipient.getInventory().addItem(sense.create()).values()
                        .forEach(rest -> recipient.getWorld()
                                .dropItemNaturally(recipient.getLocation(), rest));

                sender.sendMessage(Ui.c("&8[&bItemLock&8] &aSense an &f"
                        + recipient.getName() + " &agegeben."));
            } else {
                sender.sendMessage(Ui.c("&cNutzung: /sense give [spieler]"));
            }
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, @NotNull String[] args) {
        if (cmd.getName().equalsIgnoreCase("itemlock") && args.length == 1) return List.of("list", "reload");
        if (cmd.getName().equalsIgnoreCase("sense")) {
            if (args.length == 1) return List.of("give");
            if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
