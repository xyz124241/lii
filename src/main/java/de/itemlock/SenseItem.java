package de.itemlock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class SenseItem {
    private final ItemLockPlugin plugin;
    private final NamespacedKey idKey;
    private final NamespacedKey recipeKey;

    public SenseItem(ItemLockPlugin plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "sense");
        this.recipeKey = new NamespacedKey(plugin, "sense");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Ui.c("&5&lSense"));
        meta.lore(Ui.lore(
                "&7Schwert, Axt und Mace in einer Waffe.",
                "",
                "&8- &fNormaler Schwert-Schaden",
                "&8- &fBricht Schilde (Axt-Effekt)",
                "&8- &fMehr Schaden je hoeher du faellst (Mace)"));
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.BYTE, (byte) 1);
        meta.setEnchantmentGlintOverride(true);
        if (plugin.getConfig().getBoolean("sense.use-custom-model", true)) {
            meta.setItemModel(NamespacedKey.fromString("sensepack:sense"));
        }
        item.setItemMeta(meta);
        return item;
    }

    public boolean isSense(ItemStack stack) {
        if (stack == null || stack.getType() != Material.NETHERITE_SWORD) return false;
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(idKey, PersistentDataType.BYTE);
    }

    public void registerRecipe() {
        Bukkit.removeRecipe(recipeKey);
        if (!plugin.getConfig().getBoolean("sense.recipe-enabled", true)) return;
        ShapelessRecipe r = new ShapelessRecipe(recipeKey, create());
        r.addIngredient(Material.NETHERITE_AXE);
        r.addIngredient(Material.NETHERITE_SWORD);
        r.addIngredient(Material.MACE);
        r.addIngredient(Material.NETHER_STAR);
        Bukkit.addRecipe(r);
    }

    public void unregisterRecipe() {
        Bukkit.removeRecipe(recipeKey);
    }
}
