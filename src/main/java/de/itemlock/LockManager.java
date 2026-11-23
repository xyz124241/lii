package de.itemlock;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class LockManager {
    private final ItemLockPlugin plugin;
    private final File file;
    private final Set<Material> locked = EnumSet.noneOf(Material.class);

    public LockManager(ItemLockPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "locked.yml");
        load();
    }

    public void load() {
        locked.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String name : yml.getStringList("locked")) {
            Material m = Material.matchMaterial(name);
            if (m != null) locked.add(m);
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        List<String> names = new ArrayList<>();
        for (Material m : locked) names.add(m.name());
        Collections.sort(names);
        yml.set("locked", names);
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("locked.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    public boolean toggle(Material m) {
        boolean now;
        if (locked.contains(m)) { locked.remove(m); now = false; }
        else { locked.add(m); now = true; }
        save();
        return now;
    }

    public boolean isLocked(Material m) {
        return locked.contains(m);
    }

    public Set<Material> all() {
        return Collections.unmodifiableSet(locked);
    }

    public boolean isLocked(ItemStack stack) {
        if (stack == null) return false;
        Material type = stack.getType();
        if (type.isAir() || !locked.contains(type)) return false;
        return !(plugin.getConfig().getBoolean("protect-custom-items", true) && isCustom(stack));
    }

    private boolean isCustom(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        if (!meta.getPersistentDataContainer().isEmpty()) return true;
        return meta.hasItemModel();
    }
}
