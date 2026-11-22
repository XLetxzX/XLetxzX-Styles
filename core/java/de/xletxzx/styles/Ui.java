package de.xletxzx.styles;

import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/** Kleine Immediate-Mode-UI. Eingabe wird direkt per GLFW abgefragt (versionsunabhaengig). */
final class Ui {
    static GuiGraphics g;
    static Font font;
    static int mx, my;
    static boolean pressed, down, released, prev, rpressed, rprev;
    static String active;
    private static int cx0, cy0, cx1 = Integer.MAX_VALUE, cy1 = Integer.MAX_VALUE;

    static final int[] PALETTE = {
        0xFF8B3DFF, 0xFF3D7BFF, 0xFF22C3E6, 0xFF2ECC71, 0xFFF1C40F, 0xFFFF8A3D,
        0xFFFF3D5A, 0xFFFF5FB4, 0xFFFFFFFF, 0xFFB0B6C8, 0xFF3A4060, 0xFF0B0D18
    };

    static void begin(GuiGraphics gg, Font f, int x, int y) {
        g = gg; font = f; mx = x; my = y;
        long w = GLFW.glfwGetCurrentContext();
        boolean d = w != 0 && GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        pressed = d && !prev;
        released = !d && prev;
        down = d;
        prev = d;
        if (!d) active = null;
        boolean rd = w != 0 && GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        rpressed = rd && !rprev;
        rprev = rd;
        cx0 = 0; cy0 = 0; cx1 = Integer.MAX_VALUE; cy1 = Integer.MAX_VALUE;
    }

    static void clip(int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
        cx0 = x; cy0 = y; cx1 = x + w; cy1 = y + h;
    }

    static void unclip() {
        g.disableScissor();
        cx0 = 0; cy0 = 0; cx1 = Integer.MAX_VALUE; cy1 = Integer.MAX_VALUE;
    }

    static boolean hover(int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h
            && mx >= cx0 && mx < cx1 && my >= cy0 && my < cy1;
    }

    static Config c() { return Config.get(); }
    static int accent() { return c().accent; }
    static int text() { return c().text; }
    static int muted() { return mix(c().text, c().bg, 0.45f); }
    static int panel() { return alpha(c().panel, Math.min(255, c().bgAlpha)); }
    static int bg() { return c().bg; }

    static int alpha(int argb, int a) { return (Math.max(0, Math.min(255, a)) << 24) | (argb & 0xFFFFFF); }

    static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gr = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    /** Rainbow color for the chroma option. offset shifts the hue. */
    static int rainbow(int offset) {
        float hue = (((System.currentTimeMillis() / 18) + offset) % 360) / 60f;
        int i = (int) hue;
        float f = hue - i, q = 1 - f;
        float r, g, b;
        switch (i % 6) {
            case 0: r = 1; g = f; b = 0; break;
            case 1: r = q; g = 1; b = 0; break;
            case 2: r = 0; g = 1; b = f; break;
            case 3: r = 0; g = q; b = 1; break;
            case 4: r = f; g = 0; b = 1; break;
            default: r = 1; g = 0; b = q; break;
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    static void fill(int x, int y, int w, int h, int color) { g.fill(x, y, x + w, y + h, color); }

    static void rrect(int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { fill(x, y, w, h, color); return; }
        fill(x, y + r, w, h - 2 * r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt((double) r * r - dy * dy));
            fill(x + inset, y + i, w - 2 * inset, 1, color);
            fill(x + inset, y + h - 1 - i, w - 2 * inset, 1, color);
        }
    }

    static final String[] FONTS = {"Minecraft", "Minecraft Uniform", "Galactic", "Illager", "Unifont", "Poppins", "Poppins Bold", "Fira Sans", "Comic Relief", "Anonymous Pro", "Anton", "Archivo Black", "Crimson Text"};
    private static final String[] FONT_IDS = {null, "uniform", "alt", "illageralt", "include/unifont", "xletxzx:poppins", "xletxzx:poppinsb", "xletxzx:fira", "xletxzx:comic", "xletxzx:mono", "xletxzx:anton", "xletxzx:archivo", "xletxzx:crimson"};
    private static final net.minecraft.network.chat.Style[] FONT_STYLES = new net.minecraft.network.chat.Style[FONTS.length * 8];

    /** Style for the chosen menu font, or null for the plain default. */
    private static net.minecraft.network.chat.Style fontStyle() {
        Config c = Config.get();
        int i = Math.max(0, Math.min(FONTS.length - 1, c.font));
        if (i == 0 && !c.fontBold && !c.fontItalic && !c.fontUnderline) return null;
        int k = i * 8 + (c.fontBold ? 1 : 0) + (c.fontItalic ? 2 : 0) + (c.fontUnderline ? 4 : 0);
        if (FONT_STYLES[k] == null) {
            net.minecraft.network.chat.Style st = net.minecraft.network.chat.Style.EMPTY;
            if (FONT_IDS[i] != null) {
                st = st.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                    net.minecraft.resources.Identifier.parse(FONT_IDS[i])));
            }
            if (c.fontBold) st = st.withBold(true);
            if (c.fontItalic) st = st.withItalic(true);
            if (c.fontUnderline) st = st.withUnderlined(true);
            FONT_STYLES[k] = st;
        }
        return FONT_STYLES[k];
    }

    /** Draws text in font number i (for the font picker previews). */
    static void sample(GuiGraphics gg, int i, String s, int x, int y, int color) {
        i = Math.max(0, Math.min(FONTS.length - 1, i));
        net.minecraft.network.chat.Style st = net.minecraft.network.chat.Style.EMPTY;
        if (FONT_IDS[i] != null) st = st.withFont(new net.minecraft.network.chat.FontDescription.Resource(net.minecraft.resources.Identifier.parse(FONT_IDS[i])));
        gg.drawString(Minecraft.getInstance().font, net.minecraft.network.chat.Component.literal(s).withStyle(st), x, y, color, false);
    }

    static int w(String s) {
        s = Tr.t(s);
        net.minecraft.network.chat.Style st = fontStyle();
        return st == null ? font.width(s) : font.width(net.minecraft.network.chat.Component.literal(s).withStyle(st));
    }

    static void text(String s, int x, int y, int color) {
        s = Tr.t(s);
        net.minecraft.network.chat.Style st = fontStyle();
        if (st == null) g.drawString(font, s, x, y, color, false);
        else g.drawString(font, net.minecraft.network.chat.Component.literal(s).withStyle(st), x, y, color, false);
    }
    static void textR(String s, int right, int y, int color) { text(s, right - w(s), y, color); }
    static void textC(String s, int cx, int y, int color) { text(s, cx - w(s) / 2, y, color); }

    static String fit(String s, int maxW) {
        if (w(s) <= maxW) return s;
        while (s.length() > 1 && w(s + "...") > maxW) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    // ---------------------------------------------------------------- look
    static int surface() { return alpha(mix(0xFF0C0E17, accent(), 0.05f), Math.min(255, c().bgAlpha + 30)); }
    static int surface2() { return alpha(mix(0xFF171A28, accent(), 0.10f), Math.min(255, c().bgAlpha + 30)); }
    static int line() { return mix(0xFF2A2E44, accent(), 0.22f); }
    static int dim() { return 0xFF9A9CB8; }

    static void grad(int x, int y, int w, int h, int top, int bottom) { g.fillGradient(x, y, x + w, y + h, top, bottom); }

    static void border(int x, int y, int w, int h, int col) {
        fill(x, y, w, 1, col);
        fill(x, y + h - 1, w, 1, col);
        fill(x, y + 1, 1, h - 2, col);
        fill(x + w - 1, y + 1, 1, h - 2, col);
    }

    /** Panel / tile with gradient, border, highlight line and an accent glow on hover or selection. */
    static void card(int x, int y, int w, int h, boolean hover, boolean selected) {
        if (selected || hover) border(x - 1, y - 1, w + 2, h + 2, alpha(accent(), selected ? 90 : 45));
        int top = hover ? mix(surface2(), accent(), 0.10f) : surface2();
        grad(x, y, w, h, alpha(top, Math.min(240, c().bgAlpha + 30)), alpha(surface(), Math.min(240, c().bgAlpha + 30)));
        border(x, y, w, h, selected ? accent() : hover ? mix(line(), accent(), 0.55f) : line());
        fill(x + 1, y + 1, w - 2, 1, 0x18FFFFFF);
    }

    static boolean button(String label, int x, int y, int w, int h, boolean selected) {
        boolean hv = hover(x, y, w, h);
        if (selected) {
            int a = hv ? mix(accent(), 0xFFFFFFFF, 0.12f) : accent();
            grad(x, y, w, h, a, mix(a, 0xFF000000, 0.30f));
            border(x, y, w, h, mix(a, 0xFFFFFFFF, 0.25f));
            fill(x + 1, y + 1, w - 2, 1, 0x30FFFFFF);
        } else {
            card(x, y, w, h, hv, false);
        }
        textC(label, x + w / 2, y + (h - 8) / 2, 0xFFFFFFFF);
        if (hv && pressed) { pressed = false; return true; }
        return false;
    }

    /** Gibt true zurueck, wenn angeklickt (Aufrufer dreht den Wert um). */
    static boolean toggle(int x, int y, boolean on) {
        int w = 26, h = 13;
        boolean hv = hover(x, y, w, h);
        if (on) grad(x, y, w, h, mix(accent(), 0xFFFFFFFF, hv ? 0.15f : 0f), mix(accent(), 0xFF000000, 0.25f));
        else grad(x, y, w, h, mix(surface2(), 0xFFFFFFFF, hv ? 0.10f : 0.04f), surface());
        border(x, y, w, h, on ? mix(accent(), 0xFFFFFFFF, 0.25f) : line());
        int kx = on ? x + w - h + 2 : x + 2;
        fill(kx, y + 3, h - 4, h - 4, 0x55000000);
        fill(kx, y + 2, h - 4, h - 4, 0xFFFFFFFF);
        if (hv && pressed) { pressed = false; return true; }
        return false;
    }

    static int slider(String id, String label, String valueText, int x, int y, int w, int v, int min, int max) {
        text(label, x, y, text());
        textR(valueText, x + w, y, muted());
        int ty = y + 13;
        fill(x, ty, w, 5, surface());
        border(x, ty, w, 5, line());
        float f = (v - min) / (float) (max - min);
        f = Math.max(0f, Math.min(1f, f));
        int fw = Math.max(3, (int) (w * f));
        grad(x, ty, fw, 5, mix(accent(), 0xFFFFFFFF, 0.2f), accent());
        fill(x + fw - 3, ty - 2, 6, 9, 0xFFFFFFFF);
        border(x + fw - 3, ty - 2, 6, 9, accent());
        if (pressed && hover(x - 3, ty - 4, w + 6, 13)) { active = id; pressed = false; }
        if (id.equals(active) && down) {
            float nf = (mx - x) / (float) w;
            nf = Math.max(0f, Math.min(1f, nf));
            v = min + Math.round(nf * (max - min));
        }
        return v;
    }

    private static final java.util.Map<String, float[]> PICK = new java.util.HashMap<>();

    static int hsv(float h, float s, float v) {
        h = ((h % 1f) + 1f) % 1f * 6f;
        int i = (int) h;
        float f = h - i, p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return 0xFF000000 | ((int) (r * 255 + 0.5f) << 16) | ((int) (g * 255 + 0.5f) << 8) | (int) (b * 255 + 0.5f);
    }

    private static float[] toHsv(int argb) {
        float r = ((argb >> 16) & 255) / 255f, g = ((argb >> 8) & 255) / 255f, b = (argb & 255) / 255f;
        float mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b)), d = mx - mn, h = 0;
        if (d > 0) {
            if (mx == r) h = ((g - b) / d) % 6f;
            else if (mx == g) h = (b - r) / d + 2f;
            else h = (r - g) / d + 4f;
            h /= 6f;
            if (h < 0) h += 1f;
        }
        return new float[]{h, mx == 0 ? 0 : d / mx, mx};
    }

    /** Colour picker (square, hue bar, New/Before, hex, palette dots). Returns the (new) colour. Height ~ 120. */
    static int colorEditor(String id, int x, int y, int w, int argb) {
        float[] st = PICK.get(id);
        if (st == null) { st = new float[8]; PICK.put(id, st); }
        int cur = argb & 0xFFFFFF;
        if (st[7] == 0f || (int) st[3] != cur) {
            // external change (or first use): resync HSV and remember "before"
            if (st[7] == 0f) st[4] = cur;
            float[] n = toHsv(argb);
            if (st[7] == 0f || (int) st[3] != cur) { st[0] = n[0]; st[1] = n[1]; st[2] = n[2]; }
            st[3] = cur; st[7] = 1f;
        }
        int panelW = 64, barW = 10, gap = 6;
        int sw = Math.max(40, w - panelW - barW - gap * 2);
        int sh = 96;
        // saturation / value square
        int step = 2;
        for (int cx = 0; cx < sw; cx += step) {
            float s = Math.min(1f, (cx + step / 2f) / sw);
            grad(x + cx, y, Math.min(step, sw - cx), sh, hsv(st[0], s, 1f), 0xFF000000);
        }
        border(x - 1, y - 1, sw + 2, sh + 2, 0xFF000000);
        int hx = x + sw + gap;
        for (int i = 0; i < 6; i++) {
            int y0 = y + i * sh / 6, y1 = y + (i + 1) * sh / 6;
            grad(hx, y0, barW, y1 - y0, hsv(i / 6f, 1, 1), hsv((i + 1) / 6f, 1, 1));
        }
        border(hx - 1, y - 1, barW + 2, sh + 2, 0xFF000000);
        boolean ch = false;
        if (pressed && hover(x, y, sw, sh)) active = id + "sv";
        if (pressed && hover(hx - 2, y, barW + 4, sh)) active = id + "hu";
        if (down && (id + "sv").equals(active)) {
            st[1] = Math.max(0f, Math.min(1f, (mx - x) / (float) (sw - 1)));
            st[2] = 1f - Math.max(0f, Math.min(1f, (my - y) / (float) (sh - 1)));
            ch = true;
        }
        if (down && (id + "hu").equals(active)) {
            st[0] = Math.max(0f, Math.min(0.9999f, (my - y) / (float) (sh - 1)));
            ch = true;
        }
        // markers
        int mxp = x + (int) (st[1] * (sw - 1)), myp = y + (int) ((1f - st[2]) * (sh - 1));
        border(mxp - 3, myp - 3, 7, 7, 0xFF000000);
        border(mxp - 2, myp - 2, 5, 5, 0xFFFFFFFF);
        int hy = y + (int) (st[0] * (sh - 1));
        fill(hx - 2, hy - 1, barW + 4, 3, 0xFF000000);
        fill(hx - 1, hy, barW + 2, 1, 0xFFFFFFFF);
        // palette dots: click -> take that colour
        int out = ch ? hsv(st[0], st[1], st[2]) : argb;
        int px = hx + barW + gap, pw = x + w - px;
        text("New", px, y, muted());
        rrect(px, y + 10, pw, 14, 3, 0xFF000000 | (out & 0xFFFFFF));
        text("Before", px, y + 30, muted());
        rrect(px, y + 40, pw, 14, 3, 0xFF000000 | ((int) st[4] & 0xFFFFFF));
        if (hover(px, y + 40, pw, 14) && pressed) { out = 0xFF000000 | ((int) st[4] & 0xFFFFFF); float[] n = toHsv(out); st[0] = n[0]; st[1] = n[1]; st[2] = n[2]; pressed = false; }
        rrect(px, y + 62, pw, 16, 3, 0xFF0B0D18);
        text(String.format("#%06X", out & 0xFFFFFF), px + 4, y + 66, 0xFFFFFFFF);
        int cols = Math.max(6, Math.min(PALETTE.length, w / 14));
        int dx = (w - 10) / Math.max(1, PALETTE.length - 1);
        dx = Math.min(dx, 16);
        for (int i = 0; i < PALETTE.length; i++) {
            int sx = x + i * dx, sy = y + sh + 8;
            boolean sel = (PALETTE[i] & 0xFFFFFF) == (out & 0xFFFFFF);
            if (sel) rrect(sx - 1, sy - 1, 12, 12, 6, 0xFFFFFFFF);
            rrect(sx, sy, 10, 10, 5, PALETTE[i]);
            if (hover(sx, sy, 10, 10) && pressed) {
                out = PALETTE[i]; float[] n = toHsv(out); st[0] = n[0]; st[1] = n[1]; st[2] = n[2]; pressed = false;
            }
        }
        out = 0xFF000000 | (out & 0xFFFFFF);
        st[3] = out & 0xFFFFFF;
        return out;
    }
}
