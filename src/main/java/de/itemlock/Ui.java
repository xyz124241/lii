package de.itemlock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

final class Ui {
    private Ui() {}

    static Component c(String legacy) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy)
                .decoration(TextDecoration.ITALIC, false);
    }

    static List<Component> lore(String... lines) {
        List<Component> out = new ArrayList<>();
        for (String l : lines) out.add(c(l));
        return out;
    }
}
