package de.xletxzx.styles;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Custom crosshair that replaces the vanilla one while enabled. */
final class Crosshair {
    private Crosshair() {}

    static void register() {
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, orig -> (ctx, tc) -> {
            Config.Cross c = Config.get().cross;
            Minecraft mc = Minecraft.getInstance();
            if (!c.on) {
                orig.render(ctx, tc);
                return;
            }
            // custom crosshair only while actually playing: not in F5, not in inventories or menus
            if (mc.options.hideGui || !mc.options.getCameraType().isFirstPerson()
                || mc.screen != null && !(mc.screen instanceof HudEditorScreen)) return;
            draw(ctx, c);
        });
    }

    private static void draw(GuiGraphics g, Config.Cross c) {
        int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
        int a = Math.max(0, Math.min(255, (int) (c.opacity * 2.55)));
        int col;
        if (c.rainbow) {
            double h = (System.currentTimeMillis() % 4000) / 4000.0;
            float r = cl(Math.abs(h * 6 - 3) - 1), gr = cl(2 - Math.abs(h * 6 - 2)), b = cl(2 - Math.abs(h * 6 - 4));
            col = (a << 24) | ((int) (r * 255) << 16) | ((int) (gr * 255) << 8) | (int) (b * 255);
        } else {
            col = (a << 24) | (c.color & 0xFFFFFF);
        }
        if (c.custom && c.cust != null && c.cust.length == 225) {
            int sc = Math.max(1, c.cscale);
            int x0 = cx - (15 * sc) / 2, y0 = cy - (15 * sc) / 2;
            for (int i = 0; i < 225; i++) {
                if (c.cust[i] == 0) continue;
                int px = x0 + (i % 15) * sc, py = y0 + (i / 15) * sc;
                g.fill(px, py, px + sc, py + sc, col);
            }
            return;
        }
        int t = Math.max(1, c.thick), s = Math.max(1, c.size), gp = Math.max(0, c.gap);
        int half = t / 2;
        // arms: left, right, up, down (up is skipped for the T shape)
        int[][] arms = {
            {cx - gp - s, cy - half, s, t},
            {cx + gp + (t % 2 == 0 ? 0 : 1) , cy - half, s, t},
            {cx - half, cy - gp - s, t, s},
            {cx - half, cy + gp + (t % 2 == 0 ? 0 : 1), t, s}
        };
        for (int i = 0; i < arms.length; i++) {
            if (c.tShape && i == 2) continue;
            int[] r = arms[i];
            if (c.outline) g.fill(r[0] - 1, r[1] - 1, r[0] + r[2] + 1, r[1] + r[3] + 1, (a << 24));
        }
        for (int i = 0; i < arms.length; i++) {
            if (c.tShape && i == 2) continue;
            int[] r = arms[i];
            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], col);
        }
        if (c.dot) {
            if (c.outline) g.fill(cx - half - 1, cy - half - 1, cx - half + t + 1, cy - half + t + 1, (a << 24));
            g.fill(cx - half, cy - half, cx - half + t, cy - half + t, col);
        }
    }

    private static float cl(double v) { return (float) Math.max(0, Math.min(1, v)); }
}
