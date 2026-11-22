package de.xletxzx.styles;

import de.xletxzx.styles.Config;
import net.minecraft.world.item.ItemStack;

/** Size of the item that is being drawn in a hand seen from outside. */
public final class ThirdItem {
    public static float scale = 1f;
    /** Opacity (percent) of the item that is being drawn right now. */
    public static int alpha = 100;
    public static final java.util.Map<int[], Boolean> EXT = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    /** Tint arrays of item layers that must be drawn translucent -> percent. */
    public static final java.util.Map<int[], Integer> ALPHA = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private ThirdItem() {}

    public static float of(ItemStack st) {
        Config.Cpvp c = Config.get().cpvp;
        if (!c.sizeThird || st == null || c.itemSizes == null || c.itemSizes.isEmpty()) return 1f;
        Integer v = c.itemSizes.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()).getPath());
        return v == null ? 1f : v / 100f;
    }

    public static int alphaOf(ItemStack st) {
        try {
            Config.Opac o = Config.get().opac;
            if (st == null || o.items == null || o.items.isEmpty()) return 100;
            Integer v = o.items.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()).getPath());
            return v == null ? 100 : Math.max(0, Math.min(100, v));
        } catch (Throwable t) { return 100; }
    }
}
