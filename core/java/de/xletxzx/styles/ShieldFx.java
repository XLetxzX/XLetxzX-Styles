package de.xletxzx.styles;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Computes the tint of the own shield from its state. */
public final class ShieldFx {
    private ShieldFx() {}

    private static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** Returns the ARGB tint to use, or {@code orig} when nothing should change. */
    public static int tint(int orig) {
        try {
            Config.Shield c = Config.get().shield;
            if (!c.on) return orig;
            Minecraft mc = Minecraft.getInstance();
            Player p = mc.player;
            if (p == null || !mc.options.getCameraType().isFirstPerson()) return orig;
            ItemStack st = p.getMainHandItem().is(Items.SHIELD) ? p.getMainHandItem() : p.getOffhandItem();
            if (!st.is(Items.SHIELD)) return orig;
            float cd = p.getCooldowns().getCooldownPercent(st, 0f);
            int col;
            if (cd > 0f) {
                if (!c.tintDisabled) return orig;
                col = c.fade ? mix(0xFFFFFFFF, c.disabled, 0.25f + 0.75f * cd) : c.disabled;
                return mix(0xFFFFFFFF, col, 0.7f);
            }
            if (p.isBlocking()) {
                if (!c.tintBlock) return orig;
                col = c.block;
            } else if (p.isUsingItem() && p.getUseItem().is(Items.SHIELD)) {
                if (!c.tintRaise) return orig;
                col = c.raise;
            } else {
                if (!c.tintReady) return orig;
                col = c.ready;
            }
            return mix(0xFFFFFFFF, col, 0.7f);
        } catch (Throwable t) {
            return orig;
        }
    }
}
