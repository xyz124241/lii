package de.itemlock;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class LockMenu implements InventoryHolder {
    private enum View { BROWSE, CONFIRM }
    private static final int PER_PAGE = 45;
    private static List<Material> ALL;

    private static List<Material> allItems() {
        if (ALL == null) {
            List<Material> list = new ArrayList<>();
            for (Material m : Material.values()) {
                if (!m.isLegacy() && m.isItem() && !m.isAir()) list.add(m);
            }
            list.sort(Comparator.comparing(Enum::name));
            ALL = list;
        }
        return ALL;
    }

    private final ItemLockPlugin plugin;
    private final LockManager locks;
    private final Player player;
    private View view = View.BROWSE;
    private int page = 0;
    private boolean onlyLocked = false;
    private String search = "";
    private Material pending;
    private Inventory inv;
    private List<Material> shown = List.of();

    public LockMenu(ItemLockPlugin plugin, LockManager locks, Player player) {
        this.plugin = plugin;
        this.locks = locks;
        this.player = player;
    }

    @Override
    public @NotNull Inventory getInventory() { return inv; }

    public void setSearch(String s) {
        this.search = s == null ? "" : s.trim();
        this.page = 0;
    }

    public void open() {
        if (view == View.BROWSE) buildBrowse(); else buildConfirm();
        player.openInventory(inv);
    }

    private void buildBrowse() {
        inv = Bukkit.createInventory(this, 54, Component.text("ItemLock - Items sperren"));
        String q = search.toLowerCase(Locale.ROOT).replace(' ', '_');
        List<Material> list = new ArrayList<>();
        for (Material m : allItems()) {
            if (onlyLocked && !locks.isLocked(m)) continue;
            if (!q.isEmpty() && !m.name().toLowerCase(Locale.ROOT).contains(q)) continue;
            list.add(m);
        }
        shown = list;
        int pages = Math.max(1, (int) Math.ceil(list.size() / (double) PER_PAGE));
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = page * PER_PAGE + i;
            if (idx >= list.size()) break;
            inv.setItem(i, icon(list.get(idx)));
        }
        inv.setItem(45, button(Material.ARROW, "&e<< Zurueck"));
        inv.setItem(46, button(onlyLocked ? Material.RED_DYE : Material.LIME_DYE,
                onlyLocked ? "&cFilter: nur gesperrte" : "&aFilter: alle Items", "&7Klicken zum Umschalten"));
        inv.setItem(48, button(Material.BARRIER, "&cSchliessen"));
        inv.setItem(49, button(Material.PAPER, "&fSeite " + (page + 1) + " / " + pages,
                "&7" + list.size() + " Items", "&7Gesperrt gesamt: &c" + locks.all().size()));
        inv.setItem(50, button(Material.OAK_SIGN, "&bSuchen",
                search.isEmpty() ? "&7Klicken, dann Name in den Chat schreiben" : "&7Suche: &f" + search,
                "&7Rechtsklick: Suche loeschen"));
        inv.setItem(53, button(Material.ARROW, "&eWeiter >>"));
    }

    private ItemStack icon(Material m) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        boolean isLocked = locks.isLocked(m);
        meta.lore(Ui.lore(isLocked ? "&c&lGESPERRT" : "&a&lErlaubt",
                "&7Klicken zum " + (isLocked ? "Freigeben" : "Sperren")));
        if (isLocked) meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack button(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Ui.c(name));
        if (lore.length > 0) meta.lore(Ui.lore(lore));
        it.setItemMeta(meta);
        return it;
    }

    private void buildConfirm() {
        boolean isLocked = locks.isLocked(pending);
        inv = Bukkit.createInventory(this, 27, Component.text(isLocked ? "Freigeben bestaetigen" : "Sperren bestaetigen"));
        ItemStack show = new ItemStack(pending);
        ItemMeta sm = show.getItemMeta();
        if (sm != null) {
            sm.lore(Ui.lore(isLocked ? "&cAktuell: GESPERRT" : "&aAktuell: erlaubt"));
            show.setItemMeta(sm);
        }
        inv.setItem(13, show);
        inv.setItem(11, button(Material.LIME_CONCRETE, isLocked ? "&aJA, wieder freigeben" : "&cJA, jetzt sperren",
                isLocked ? new String[]{"&7Item kann wieder gedroppt, gefunden", "&7und gecraftet werden."}
                         : new String[]{"&7Weg aus Mob-Drops, Loot, Vaults,", "&7Spawnern und Vanilla-Crafting.",
                                        "&7Custom-Rezepte bleiben erlaubt."}));
        inv.setItem(15, button(Material.RED_CONCRETE, "&7Abbrechen"));
    }

    public void click(InventoryClickEvent e) {
        if (e.getClickedInventory() != inv) return;
        int slot = e.getRawSlot();
        if (view == View.CONFIRM) {
            if (slot == 11) {
                boolean now = locks.toggle(pending);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                player.sendMessage(Ui.c("&8[&bItemLock&8] &f" + pending.name() + (now ? " &cwurde gesperrt." : " &awurde freigegeben.")));
                view = View.BROWSE;
                open();
            } else if (slot == 15) {
                view = View.BROWSE;
                open();
            }
            return;
        }
        if (slot < PER_PAGE) {
            int idx = page * PER_PAGE + slot;
            if (idx < shown.size()) {
                pending = shown.get(idx);
                view = View.CONFIRM;
                open();
            }
            return;
        }
        switch (slot) {
            case 45 -> { page--; buildBrowse(); player.openInventory(inv); }
            case 53 -> { page++; buildBrowse(); player.openInventory(inv); }
            case 46 -> { onlyLocked = !onlyLocked; page = 0; buildBrowse(); player.openInventory(inv); }
            case 48 -> player.closeInventory();
            case 50 -> {
                if (e.isRightClick()) {
                    setSearch("");
                    buildBrowse();
                    player.openInventory(inv);
                } else {
                    player.closeInventory();
                    plugin.awaitSearch(player, this);
                    player.sendMessage(Ui.c("&8[&bItemLock&8] &7Schreibe den Item-Namen in den Chat (z.B. &fender&7). &8'-' = Suche loeschen"));
                }
            }
            default -> { }
        }
    }
}
