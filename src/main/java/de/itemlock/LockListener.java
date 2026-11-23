package de.itemlock;

import org.bukkit.Keyed;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.BlockDispenseLootEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.List;

/** Entfernt gesperrte Items ueberall, wo sie entstehen koennen. */
public final class LockListener implements Listener {
    private final ItemLockPlugin plugin;
    private final LockManager locks;

    public LockListener(ItemLockPlugin plugin, LockManager locks) {
        this.plugin = plugin;
        this.locks = locks;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent e) {
        Item item = e.getEntity();
        if (item.getThrower() != null) return;
        if (locks.isLocked(item.getItemStack())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobDeath(EntityDeathEvent e) {
        e.getDrops().removeIf(locks::isLocked);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent e) {
        e.getItems().removeIf(i -> locks.isLocked(i.getItemStack()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLoot(LootGenerateEvent e) {
        List<ItemStack> filtered = new ArrayList<>(e.getLoot());
        filtered.removeIf(locks::isLocked);
        e.setLoot(filtered);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVaultLoot(BlockDispenseLootEvent e) {
        List<ItemStack> filtered = new ArrayList<>(e.getDispensedLoot());
        filtered.removeIf(locks::isLocked);
        e.setDispensedLoot(filtered);
    }

    private boolean isVanilla(Recipe r) {
        return !(r instanceof Keyed k) || k.getKey().getNamespace().equals("minecraft");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        Recipe r = e.getRecipe();
        if (r == null || !isVanilla(r)) return;
        if (locks.isLocked(r.getResult())) e.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {
        Recipe r = e.getRecipe();
        if (isVanilla(r) && locks.isLocked(r.getResult())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmelt(FurnaceSmeltEvent e) {
        if (locks.isLocked(e.getResult())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCook(BlockCookEvent e) {
        if (locks.isLocked(e.getResult())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onResultClick(InventoryClickEvent e) {
        if (e.getSlotType() != InventoryType.SlotType.RESULT) return;
        InventoryType t = e.getInventory().getType();
        if (t == InventoryType.WORKBENCH || t == InventoryType.CRAFTING) return;
        if (locks.isLocked(e.getCurrentItem())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTrade(VillagerAcquireTradeEvent e) {
        if (locks.isLocked(e.getRecipe().getResult())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        Item item = e.getItem();
        if (locks.isLocked(item.getItemStack())) {
            e.setCancelled(true);
            if (plugin.getConfig().getBoolean("remove-from-inventories")) item.remove();
        }
    }
}
