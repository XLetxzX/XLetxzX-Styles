package de.xletxzx.styles;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;

/** While the chat is open, HUD modules can be dragged with the mouse. */
final class ChatMove {
    private ChatMove() {}

    private static String dragging;
    private static int offX, offY;

    static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof ChatScreen) {
                dragging = null;
                ScreenEvents.afterRender(screen).register((sc, g, mx, my, dt) -> draw(g, mx, my, sc.width, sc.height));
            }
        });
    }

    private static void draw(GuiGraphics g, int mx, int my, int sw, int sh) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Ui.begin(g, mc.font, mx, my);
            Config c = Config.get();
            String hoverKey = null;
            for (String k : HudRenderer.KEYS) {
                if (k.equals("glow")) continue;
                Config.Hud h = c.hud.get(k);
                if (!h.on) continue;
                int[] sz = HudRenderer.sizeS(k);
                boolean hv = Ui.hover(h.x, h.y, sz[0], sz[1]);
                if (hv) hoverKey = k;
                if (Ui.pressed && hv && dragging == null) {
                    dragging = k; offX = mx - h.x; offY = my - h.y;
                }
                if (k.equals(dragging) && Ui.down) {
                    h.x = Math.max(0, Math.min(sw - sz[0], mx - offX));
                    h.y = Math.max(0, Math.min(sh - sz[1], my - offY));
                }
                int oc = k.equals(dragging) ? 0xFFFFFFFF : (hv ? Ui.accent() : Ui.alpha(Ui.accent(), 110));
                g.fill(h.x - 1, h.y - 1, h.x + sz[0] + 1, h.y, oc);
                g.fill(h.x - 1, h.y + sz[1], h.x + sz[0] + 1, h.y + sz[1] + 1, oc);
                g.fill(h.x - 1, h.y, h.x, h.y + sz[1], oc);
                g.fill(h.x + sz[0], h.y, h.x + sz[0] + 1, h.y + sz[1], oc);
            }
            if (hoverKey != null && dragging == null) {
                Config.Hud h = c.hud.get(hoverKey);
                Ui.text(HudRenderer.LABELS[HudRenderer.indexOf(hoverKey)], h.x, Math.max(0, h.y - 11), 0xFFFFFFFF);
            }
            if (Ui.released && dragging != null) { dragging = null; Config.save(); }
        } catch (Throwable t) {
            dragging = null;
        }
    }
}
