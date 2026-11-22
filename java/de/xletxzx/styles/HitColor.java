package de.xletxzx.styles;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;

import java.lang.reflect.Field;

/** Changes the color of the flash that entities get when they are hit (the vanilla red hurt overlay). */
final class HitColor {
    private HitColor() {}

    private static String applied = "";
    private static Field texField;

    static void tick(Minecraft mc) {
        Config.Hit h = Config.get().hit;
        String sig = h.on ? h.color + "/" + h.opacity : "off";
        if (sig.equals(applied)) return;
        try {
            OverlayTexture ot = mc.gameRenderer.overlayTexture();
            if (ot == null) return;
            if (texField == null) {
                for (Field f : OverlayTexture.class.getDeclaredFields()) {
                    if (f.getType() == DynamicTexture.class) { f.setAccessible(true); texField = f; break; }
                }
            }
            if (texField == null) { applied = sig; return; }
            DynamicTexture tex = (DynamicTexture) texField.get(ot);
            NativeImage img = tex.getPixels();
            if (img == null) return;
            int col;
            if (h.on) {
                int a = Math.max(0, Math.min(255, h.opacity * 255 / 100));
                col = ARGB.color(a, (h.color >> 16) & 255, (h.color >> 8) & 255, h.color & 255);
            } else {
                col = ARGB.color(178, 255, 0, 0);
            }
            for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setPixel(x, y, col);
            tex.upload();
            applied = sig;
        } catch (Throwable t) {
            System.err.println("[XLetxzX] Hit color failed: " + t);
            applied = sig;
        }
    }
}
