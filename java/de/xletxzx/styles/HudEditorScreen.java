package de.xletxzx.styles;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class HudEditorScreen extends Screen {
    private final Screen parent;
    private String dragging, hoverKey;
    private int offX, offY;
    private boolean prevKey = true;

    public HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    /** Mouse wheel over a module = change its size. */
    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (hoverKey != null) {
            Config.Hud h = Config.get().hud.get(hoverKey);
            h.scale = Math.max(0.5f, Math.min(3f, Math.round((h.scale + (sy > 0 ? 0.1f : -0.1f)) * 10f) / 10f));
            Config.save();
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        Ui.begin(g, this.font, mx, my);
        g.fill(0, 0, this.width, this.height, 0x66000000);
        Config c = Config.get();
        for (int x = 0; x < width; x += 20) g.fill(x, 0, x + 1, height, 0x14FFFFFF);
        for (int y = 0; y < height; y += 20) g.fill(0, y, width, y + 1, 0x14FFFFFF);

        hoverKey = null;
        for (String k : HudRenderer.KEYS) {
            if (k.equals("glow")) continue;
            Config.Hud h = c.hud.get(k);
            if (!h.on) continue;
            int[] sz = HudRenderer.sizeS(k);
            boolean hv = Ui.hover(h.x, h.y, sz[0], sz[1]);
            if (hv) hoverKey = k;
            if (Ui.pressed && hv && dragging == null) {
                dragging = k; offX = mx - h.x; offY = my - h.y; Ui.pressed = false;
            }
            if (k.equals(dragging) && Ui.down) {
                h.x = Math.max(0, Math.min(width - sz[0], mx - offX));
                h.y = Math.max(0, Math.min(height - sz[1], my - offY));
            }
            HudRenderer.draw(g, this.font, k, h.x, h.y);
            int oc = k.equals(dragging) ? 0xFFFFFFFF : (hv ? Ui.accent() : Ui.alpha(Ui.accent(), 140));
            g.fill(h.x - 1, h.y - 1, h.x + sz[0] + 1, h.y, oc);
            g.fill(h.x - 1, h.y + sz[1], h.x + sz[0] + 1, h.y + sz[1] + 1, oc);
            g.fill(h.x - 1, h.y, h.x, h.y + sz[1], oc);
            g.fill(h.x + sz[0], h.y, h.x + sz[0] + 1, h.y + sz[1], oc);
            if (hv) Ui.text(HudRenderer.LABELS[HudRenderer.indexOf(k)] + "  " + Math.round(h.scale * 100) + "%",
                h.x, Math.max(0, h.y - 11), 0xFFFFFFFF);
        }
        if (Ui.released && dragging != null) { dragging = null; Config.save(); }

        int pw = 120, ph = 36, px = (width - pw) / 2, py = height / 2 - 20;
        Ui.rrect(px, py, pw, ph, c.radius + 2, Ui.alpha(c.panel, 235));
        Ui.textC("HUD Editor", width / 2, py + 7, Ui.text());
        if (Ui.button("Done", width / 2 - 30, py + 19, 60, 13, true)) onClose();

        long w = GLFW.glfwGetCurrentContext();
        boolean k = w != 0 && XLetxzXStyles.menuKeyDown(w);
        if (k && !prevKey) onClose();
        prevKey = k;
    }

    @Override
    public void onClose() {
        Config.save();
        this.minecraft.setScreen(parent);
    }
}
