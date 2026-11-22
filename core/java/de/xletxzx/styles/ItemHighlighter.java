package de.xletxzx.styles;

import de.xletxzx.styles.mixin.ContainerScreenAccess;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Highlights chosen items in every inventory window. */
final class ItemHighlighter {
    private ItemHighlighter() {}

    static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof AbstractContainerScreen<?> cs) {
                ScreenEvents.afterRender(screen).register((sc, g, mx, my, dt) -> draw(cs, g));
            }
        });
    }

    static String key(ItemStack st) {
        return BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
    }

    private static void draw(AbstractContainerScreen<?> cs, GuiGraphics g) {
        Config.ItemHl c = Config.get().ihl;
        if (!c.on || c.items.isEmpty()) return;
        int left, top;
        try {
            ContainerScreenAccess a = (ContainerScreenAccess) cs;
            left = a.xletxzx$left();
            top = a.xletxzx$top();
        } catch (Throwable t) {
            left = (cs.width - 176) / 2;
            top = (cs.height - 166) / 2;
        }
        int a = Math.max(0, Math.min(255, c.opacity * 255 / 100));
        int fill = (a << 24) | (c.color & 0xFFFFFF);
        int edge = 0xFF000000 | (c.color & 0xFFFFFF);
        for (Slot s : cs.getMenu().slots) {
            ItemStack st = s.getItem();
            if (st.isEmpty()) continue;
            Boolean on = c.items.get(key(st));
            if (on == null || !on) continue;
            int x = left + s.x, y = top + s.y;
            g.fill(x, y, x + 16, y + 16, fill);
            g.fill(x, y, x + 16, y + 1, edge);
            g.fill(x, y + 15, x + 16, y + 16, edge);
            g.fill(x, y, x + 1, y + 16, edge);
            g.fill(x + 15, y, x + 16, y + 16, edge);
        }
    }
}
