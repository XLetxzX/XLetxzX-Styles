package de.xletxzx.styles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** EXTRAS content of the XLetxzX menu (drawn inside the menu window). */
final class ExtrasPanel {
    static final int HUD = 1, FEATURES = 2, TWEAKS = 3, PERF = 4, PACKLIST = 5, BLOCK = 6, FONTS = 7, CROSS = 8, HIT = 9, NICK = 10, ITEMHL = 11, SCORE = 12, SHIELD = 13, PHYS = 14, BLUR = 15, SATU = 16, CPVP = 17, OPAC = 18;
    static final int[] SIDE_TABS = {HUD, FEATURES, TWEAKS, PERF, CPVP, BLOCK, CROSS, HIT, SHIELD, PHYS, BLUR, SATU, OPAC, NICK, ITEMHL, FONTS};
    private static final String[] TABS = {"", "HUD Modules", "Features", "Tweaks", "Performance", "Pack list", "Block Highlight", "Fonts", "Crosshair", "Hit Color", "Nick", "Item Highlighter", "Scoreboard", "Shield Status", "Item Physics", "Motion Blur", "Colour Saturation", "CPvP", "Opacity Editor"};
    private static final String[] GLYPH = {"","@crafting_table","@nether_star","@comparator","@firework_rocket","@bookshelf","@glowstone","@spectral_arrow","@diamond_sword","@name_tag","@spyglass","@oak_sign","@writable_book","@shield","@dropper","@ender_eye","@red_dye","@end_crystal","@glass"};
    private static final int[] GCOL = {0, 0xFF3D8BFF, 0xFFFFB02E, 0xFFFF5C5C, 0xFFFF8A3D, 0xFFB07CFF, 0xFF3DD5FF, 0xFFFF4FA3, 0xFF3DDC84, 0xFFFF5C5C, 0xFFFFB02E, 0xFF3DD5FF, 0xFFB07CFF, 0xFF3D8BFF, 0xFFFFB02E, 0xFF3DD5FF, 0xFFFF4FA3, 0xFFFF5C5C, 0xFFB07CFF};
    private static final String[] MODES = {"Text", "Border", "Background"};
    static final int SIDE = 130;

    static int tab = HUD;
    static String query = "";
    private static String filter = "All";
    private static int mode = 0;
    private static String hudSel = "fps";

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Font font = minecraft.font;
    private int scroll, maxScroll, scrollL, maxL, scrollR, maxR, splitX;
    private int rebinding; // 0 = none, 1 = zoom key
    private Runnable after;
    private String toast = "";
    private long toastUntil;
    private Set<String> sel = new HashSet<>();

    ExtrasPanel() {}

    boolean mouseScrolled(double x, double y, double sxx, double syy) {
        int d = (int) (syy * 20);
        if (x < sideX1) { sideScroll = Math.max(0, Math.min(sideMax, sideScroll - d)); return true; }
        if (tab == HUD && query.trim().isEmpty()) {
            if (x < splitX) scrollL = Math.max(0, Math.min(maxL, scrollL - d));
            else scrollR = Math.max(0, Math.min(maxR, scrollR - d));
        } else {
            scroll = Math.max(0, Math.min(maxScroll, scroll - d));
        }
        return true;
    }

    void toast(String s) { toast = s; toastUntil = System.currentTimeMillis() + 3500; }

    // ================================================================ render
    /** Draws the EXTRAS page into the given rectangle (absolute screen coordinates). */
    void render(GuiGraphics g, int x0, int y0, int W, int H, int mx, int my, boolean packList) {
        if (packList) tab = PACKLIST; else if (tab == PACKLIST) tab = HUD;
        if (tab == PACKLIST || tab == HUD) sel = PackManager.selected();
        Config c = Config.get();
        ctxOn = false;
        int cx, cy, cw, ch;
        if (packList) {
            cx = x0; cy = y0; cw = W; ch = H;
        } else {
            sidebar(x0, y0, H);
            cx = x0 + SIDE + 12; cy = y0 + 4; cw = W - SIDE - 16; ch = H - 8;
        }
        if (!packList && !query.trim().isEmpty()) {
            results(cx, cy, cw, ch);
        } else if (tab == HUD) {
            hudTab(cx, cy, cw, ch);
        } else if (tab == PACKLIST) {
            packs(cx, cy, cw, ch);
        } else {
            viewTop = cy; viewBot = cy + ch;
            Ui.clip(cx - 2, cy - 2, cw + 4, ch + 4);
            int end;
            switch (tab) {
                case FEATURES: end = features(cx, cy - scroll, cw); break;
                case TWEAKS: end = tweaks(cx, cy - scroll, cw); break;
                case BLOCK: end = block(cx, cy - scroll, cw); break;
                case FONTS: end = fonts(cx, cy - scroll, cw); break;
                case CROSS: end = cross(cx, cy - scroll, cw); break;
                case HIT: end = hit(cx, cy - scroll, cw); break;
                case NICK: end = nick(cx, cy - scroll, cw); break;
                case ITEMHL: end = items(cx, cy - scroll, cw); break;
                case SHIELD: end = shieldTab(cx, cy - scroll, cw); break;
                case PHYS: end = physTab(cx, cy - scroll, cw); break;
                case BLUR: end = blurTab(cx, cy - scroll, cw); break;
                case SATU: end = satTab(cx, cy - scroll, cw); break;
                case CPVP: end = cpvpTab(cx, cy - scroll, cw); break;
                case OPAC: end = opacTab(cx, cy - scroll, cw); break;
                default: end = perf(cx, cy - scroll, cw); break;
            }
            Ui.unclip();
            maxScroll = Math.max(0, end + scroll - (cy + ch));
            scroll = Math.max(0, Math.min(maxScroll, scroll));
            if (maxScroll > 0) {
                int bh = Math.max(14, ch * ch / (ch + maxScroll));
                Ui.rrect(cx + cw + 1, cy + (int) ((ch - bh) * (scroll / (float) maxScroll)), 2, bh, 1, Ui.alpha(Ui.accent(), 200));
            }
        }

        if (Ui.released) Config.save();
        if (System.currentTimeMillis() < toastUntil) {
            int tw = Ui.w(toast) + 16;
            int tx = x0 + (W - tw) / 2, ty = y0 + H - 18;
            Ui.rrect(tx, ty, tw, 16, c.radius, 0xEE0B0B14);
            Ui.fill(tx, ty, 2, 16, Ui.accent());
            Ui.textC(toast, tx + tw / 2, ty + 4, 0xFFFFD27A);
        }
        if (after != null) { Runnable r = after; after = null; r.run(); }
    }

    private int pScroll, pMax;

    /** Performance settings (shown on the Settings page of the menu). */
    void renderPerf(int x, int y, int w, int h) {
        Ui.clip(x - 2, y - 2, w + 4, h + 4);
        int end = perf(x, y - pScroll, w);
        Ui.unclip();
        pMax = Math.max(0, end + pScroll - (y + h));
        pScroll = Math.max(0, Math.min(pMax, pScroll));
        if (pMax > 0) {
            int bh = Math.max(14, h * h / (h + pMax));
            Ui.rrect(x + w + 1, y + (int) ((h - bh) * (pScroll / (float) pMax)), 2, bh, 1, Ui.alpha(Ui.accent(), 200));
        }
        if (System.currentTimeMillis() < toastUntil) {
            int tw = Ui.w(toast) + 16;
            int tx = x + (w - tw) / 2, ty = y + h - 18;
            Ui.rrect(tx, ty, tw, 16, 2, 0xEE0B0B14);
            Ui.textC(toast, tx + tw / 2, ty + 4, 0xFFFFD27A);
        }
        if (after != null) { Runnable r = after; after = null; r.run(); }
    }

    void scrollPerf(double amount) { pScroll = Math.max(0, Math.min(pMax, pScroll - (int) (amount * 20))); }

    private void setTab(int t) { tab = t; scroll = 0; scrollL = 0; scrollR = 0; rebinding = 0; }

    // ================================================================ sidebar
    static int sideScroll, sideMax, sideX1, viewTop, viewBot;
    static boolean ctxOn;
    static int ctxX, ctxY, ctxW;
    static String ctxId = "", ctxHint = "", itemQ = "";

    private void ctx(String id, String hint, int x, int y, int w) {
        if (y >= viewTop - 2 && y + 16 <= viewBot + 2) { ctxOn = true; ctxId = id; ctxHint = hint; ctxX = x; ctxY = y; ctxW = w; }
    }

    static String ctxText() { return "nick".equals(ctxId) ? Config.get().nick.name : "preset".equals(ctxId) ? PresetsPanel.name : itemQ; }

    static void setCtx(String t) {
        if ("nick".equals(ctxId)) Config.get().nick.name = t == null ? "" : t;
        else if ("preset".equals(ctxId)) PresetsPanel.name = t == null ? "" : t;
        else if ("items".equals(ctxId) || "glow".equals(ctxId) || "isize".equals(ctxId) || "iopac".equals(ctxId)) itemQ = t == null ? "" : t;
    }

    private void sidebar(int x0, int y0, int H) {
        Config c = Config.get();
        Ui.card(x0, y0, SIDE, H, false, false);
        Ui.text("Extras", x0 + 10, y0 + 8, 0xFFFFFFFF);
        int hudOn = 0;
        for (String k : HudRenderer.KEYS) if (c.hud.get(k).on) hudOn++;
        Ui.text(hudOn + " modules on", x0 + 10, y0 + 19, Ui.accent());
        int top = y0 + 58, bot = y0 + H - 52;
        sideX1 = x0 + SIDE;
        int total = SIDE_TABS.length * 20 + 4 * 16;
        sideMax = Math.max(0, total - (bot - top));
        sideScroll = Math.max(0, Math.min(sideMax, sideScroll));
        Ui.clip(x0, top - 2, SIDE, bot - top + 2);
        int y = top - sideScroll;
        for (int i : SIDE_TABS) {
            String head = i == HUD ? "Modules" : i == TWEAKS ? "Gameplay" : i == CPVP ? null : i == BLOCK ? "Visuals" : i == NICK ? "Extras" : null;
            if (head != null) {
                Ui.text(Tr.t(head).toUpperCase(), x0 + 10, y + 5, Ui.muted());
                Ui.fill(x0 + 8, y + 14, SIDE - 16, 1, 0x22FFFFFF);
                y += 16;
            }
            String count = i == HUD ? hudOn + "/" + HudRenderer.KEYS.length
                : i == FEATURES ? featuresOn() + "/3" : i == TWEAKS ? tweaksOn() + "/5" : i == BLOCK ? blockOn() + "/4" : i == CROSS ? (Config.get().cross.on ? "on" : "off") : i == HIT ? (Config.get().hit.on ? "on" : "off") : i == NICK ? (Config.get().nick.on ? "on" : "off") : i == ITEMHL ? (Config.get().ihl.on ? "on" : "off") : i == SHIELD ? (Config.get().shield.on ? "on" : "off") : i == PHYS ? (Config.get().phys.on ? "on" : "off") : i == BLUR ? (Config.get().post.blurOn ? "on" : "off") : i == SATU ? (Config.get().post.satOn ? "on" : "off") : i == CPVP ? cpvpOn() + "/9" : i == OPAC ? (Config.get().opac.any() ? "on" : "off") : "";
            if (y + 19 > top - 2 && y < bot && navRow(i, x0 + 4, y, SIDE - 8, 19, count)) { setTab(i); query = ""; }
            y += 20;
        }
        Ui.unclip();
        if (sideMax > 0) {
            int bh = Math.max(12, (bot - top) * (bot - top) / total);
            Ui.rrect(x0 + SIDE - 3, top + (int) ((bot - top - bh) * (sideScroll / (float) sideMax)), 2, bh, 1, Ui.alpha(Ui.accent(), 200));
        }
        y = y0 + H - 46;
        if (toolRow("@crafting_table", 0xFF3D8BFF, "HUD Editor", x0 + 4, y, SIDE - 8)) after = () -> minecraft.setScreen(new HudEditorScreen(minecraft.screen));
        y += 22;
        if (toolRow("@barrier", 0xFFFF5C5C, "Reset HUD", x0 + 4, y, SIDE - 8)) { Config.resetHudPositions(); toast("HUD positions reset."); }
    }

    private int featuresOn() {
        Config.Fx f = Config.get().fx;
        return (f.zoom ? 1 : 0) + (f.killAnim ? 1 : 0)
            + (Config.get().hud.get("spotify").on || Config.get().hud.get("lyrics").on ? 1 : 0);
    }

    private int blockOn() {
        Config.Hl h = Config.get().hl;
        return (h.o.on ? 1 : 0) + (h.o2.on ? 1 : 0) + (h.o3.on ? 1 : 0) + (h.fill ? 1 : 0);
    }

    private int tweaksOn() {
        Config.Tweaks t = Config.get().tweaks;
        boolean[] b = {t.lowFire, t.lowShield, t.lowTotem, t.noVignette, Config.get().fx.hideMyArmor};
        int n = 0;
        for (boolean v : b) if (v) n++;
        return n;
    }

    // ================================================================ helpers
    private static final java.util.Map<String, net.minecraft.world.item.ItemStack> ICON = new java.util.HashMap<>();

    private static net.minecraft.world.item.ItemStack iconStack(String id) {
        return ICON.computeIfAbsent(id, k -> {
            net.minecraft.world.item.Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(k));
            return it == null ? net.minecraft.world.item.ItemStack.EMPTY : new net.minecraft.world.item.ItemStack(it);
        });
    }

    /** Icon tile: a real item picture ("@item_id") on a dark rounded square; falls back to a glyph. */
    private void badge(String glyph, int col, int x, int y) {
        if (glyph.startsWith("@")) {
            Ui.rrect(x, y, 16, 16, 3, Ui.alpha(0xFF000000, 90));
            Ui.rrect(x, y + 15, 16, 1, 0, Ui.alpha(col, 200));
            net.minecraft.world.item.ItemStack st = iconStack(glyph.substring(1));
            if (!st.isEmpty()) {
                Ui.g.pose().pushMatrix();
                Ui.g.pose().translate(x + 1, y + 1);
                Ui.g.pose().scale(0.875f, 0.875f);
                Ui.g.renderItem(st, 0, 0);
                Ui.g.pose().popMatrix();
                return;
            }
            glyph = "?";
        }
        Ui.rrect(x, y, 16, 16, 3, Ui.alpha(col, 215));
        Ui.textC(glyph, x + 8, y + 4, 0xFFFFFFFF);
    }

    /** Sidebar row: item icon, label, counter on the right, accent highlight when selected. */
    private boolean navRow(int i, int x, int y, int w, int h, String count) {
        boolean on = tab == i, hv = Ui.hover(x, y, w, h);
        if (on) {
            Ui.grad(x, y, w, h, Ui.alpha(Ui.accent(), 120), Ui.alpha(Ui.accent(), 50));
            Ui.fill(x, y, 2, h, Ui.accent());
        } else if (hv) {
            Ui.fill(x, y, w, h, 0x1AFFFFFF);
        }
        badge(GLYPH[i], GCOL[i], x + 5, y + (h - 16) / 2);
        Ui.text(TABS[i], x + 24, y + (h - 8) / 2, on || hv ? 0xFFFFFFFF : 0xFFDDDDEE);
        if (!count.isEmpty()) Ui.textR(count, x + w - 4, y + (h - 8) / 2, on ? 0xFFFFFFFF : Ui.accent());
        if (hv && Ui.pressed) { Ui.pressed = false; return true; }
        return false;
    }

    private boolean toolRow(String glyph, int col, String label, int x, int y, int w) {
        boolean hv = Ui.hover(x, y, w, 20);
        if (hv) Ui.fill(x, y, w, 20, 0x22FFFFFF);
        badge(glyph, col, x + 5, y + 2);
        Ui.text(label, x + 24, y + 6, hv ? 0xFFFFFFFF : 0xFFBBBBCC);
        if (hv && Ui.pressed) { Ui.pressed = false; return true; }
        return false;
    }

    /** Key binding button. which: 1 = zoom key, 2 = menu key. */
    private void keyButton(int which, int x, int y, int w) {
        Config.Fx f = Config.get().fx;
        int cur = f.zoomKey;
        boolean on = rebinding == which;
        if (Ui.button(on ? "Press a key..." : keyName(cur), x, y, w, 16, on)) rebinding = on ? 0 : which;
        if (rebinding != which) return;
        long win = GLFW.glfwGetCurrentContext();
        for (int k = 32; k <= 348 && win != 0; k++) {
            if (GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS) {
                if (k != GLFW.GLFW_KEY_ESCAPE) {
                    f.zoomKey = k;
                }
                rebinding = 0;
                Config.save();
                break;
            }
        }
    }

    private static final java.util.Map<String, Object[]> EMOJI = new java.util.LinkedHashMap<>();
    static {
        Object[][] m = {
            {"fullbright", "@glowstone", 0xFFFFC83D}, {"visual", "@ender_eye", 0xFF3DD5FF}, {"zoom", "@spyglass", 0xFF3D8BFF},
            {"kill", "@diamond_sword", 0xFFFF5C5C}, {"sound", "@note_block", 0xFFB07CFF}, {"spotify", "@music_disc_cat", 0xFF3DDC84},
            {"low fire", "@fire_charge", 0xFFFF8A3D}, {"low shield", "@shield", 0xFF3D8BFF}, {"low totem", "@totem_of_undying", 0xFFFFC83D},
            {"hide armor", "@iron_chestplate", 0xFF9A9CB8}, {"pumpkin", "@carved_pumpkin", 0xFFFF8A3D}, {"vignette", "@black_dye", 0xFF9A9CB8},
            {"nausea", "@fermented_spider_eye", 0xFF3DDC84}, {"bobbing", "@feather", 0xFF3DD5FF}, {"fov", "@ender_eye", 0xFFFF8A3D},
            {"enabled", "@lime_dye", 0xFF3DDC84}, {"gradient", "@purple_dye", 0xFFB07CFF},
            {"rainbow", "@firework_star", 0xFFFF4FA3}, {"color", "@red_dye", 0xFFFF4FA3}, {"miscellaneous", "@comparator", 0xFF9A9CB8},
            {"pulse", "@heart_of_the_sea", 0xFFFF5C5C}, {"vanilla", "@grass_block", 0xFF9A9CB8}, {"extras", "@nether_star", 0xFFFFC83D},
            {"tweaks", "@comparator", 0xFFFF5C5C}, {"lyrics", "@paper", 0xFF3DDC84}, {"hud", "@crafting_table", 0xFF3D8BFF},
            {"outline", "@item_frame", 0xFFB07CFF}, {"center dot", "@ender_pearl", 0xFFFFFFFF}, {"shape", "@stick", 0xFF3DDC84},
            {"theme", "@painting", 0xFFFF4FA3}, {"custom", "@name_tag", 0xFFFF4FA3}, {"look", "@ender_eye", 0xFF3DD5FF},
            {"label", "@name_tag", 0xFF3D8BFF}, {"bold", "@writable_book", 0xFFFFC83D}, {"font", "@book", 0xFFFFC83D}, {"background", "@black_concrete", 0xFF9A9CB8}, {"shadow", "@coal", 0xFF9A9CB8},
            {"border", "@item_frame", 0xFFB07CFF}, {"progress", "@experience_bottle", 0xFF3DDC84}, {"size", "@spyglass", 0xFF3D8BFF},
            {"opacity", "@glass", 0xFFB07CFF}, {"position", "@compass", 0xFF3DD5FF}, {"reset", "@barrier", 0xFFFF5C5C},
            {"line width", "@stick", 0xFF3D8BFF}, {"speed", "@sugar", 0xFFFF8A3D}, {"distance", "@spyglass", 0xFF3D8BFF},
            {"key", "@lever", 0xFF3D8BFF}, {"effect", "@potion", 0xFFFF4FA3}, {"corner", "@stone_slab", 0xFF9A9CB8},
            {"snow", "@snowball", 0xFF3DD5FF}, {"gap", "@stick", 0xFF3D8BFF}, {"thick", "@stick", 0xFF3D8BFF}
        };
        for (Object[] r : m) EMOJI.put((String) r[0], new Object[]{r[1], r[2]});
    }

    /** Small colored badge in front of a label. Returns the x offset for the text (0 if none). */
    private int emoji(String label, int x, int y) {
        String l = label.toLowerCase();
        for (java.util.Map.Entry<String, Object[]> e : EMOJI.entrySet()) {
            if (l.contains(e.getKey())) {
                badge((String) e.getValue()[0], (Integer) e.getValue()[1], x, y);
                return 22;
            }
        }
        return 0;
    }

    private static final java.util.Set<String> OPEN = new java.util.HashSet<>();

    /** Collapsible header row. Returns true while the group is open. */
    private boolean fold(String id, String label, int x, int y, int w, int swatch) {
        boolean open = OPEN.contains(id);
        boolean hov = Ui.hover(x, y, w, 16);
        Ui.rrect(x, y, w, 16, 3, hov ? Ui.surface2() : Ui.surface());
        Ui.text((open ? "- " : "+ ") + label, x + 6, y + 4, Ui.text());
        if (swatch != 0) {
            Ui.rrect(x + w - 26, y + 3, 20, 10, 2, 0xFF000000 | swatch);
        }
        if (hov && Ui.pressed) { Ui.pressed = false; if (open) OPEN.remove(id); else OPEN.add(id); }
        return open;
    }

    private int section(String s, int x, int y, int w) {
        int o = emoji(s, x, y + 3);
        Ui.text(Tr.t(s).toUpperCase(), x + o, y + (o > 0 ? 3 : 0), Ui.muted());
        int tw = o + Ui.w(s);
        Ui.fill(x + tw + 6, y + (o > 0 ? 7 : 4), Math.max(0, w - tw - 6), 1, Ui.alpha(Ui.muted(), 70));
        return y + (o > 0 ? 20 : 14);
    }

    private int sl(String id, String label, String vt, int x, int y, int w, int v, int min, int max) {
        int o = emoji(label.trim(), x, y - 1);
        return Ui.slider(id, label, vt, x + o, y, w - o, v, min, max);
    }

    private void rowLabel(String label, int x, int y) {
        int o = emoji(label, x, y + 1);
        Ui.text(label, x + o, y + 4, Ui.text());
    }

    private boolean tog(String label, int x, int y, int w, boolean v) {
        int o = emoji(label.trim(), x, y + 1);
        Ui.text(label, x + o, y + 3, Ui.text());
        return Ui.toggle(x + w - 26, y, v) ? !v : v;
    }

    private void card(int x, int y, int w, String label, String value) {
        Ui.rrect(x, y, w, 50, Config.get().radius, Ui.panel());
        Ui.rrect(x, y + 6, 2, 38, 1, Ui.accent());
        Ui.text(label, x + 10, y + 9, Ui.muted());
        Ui.text(value, x + 10, y + 26, Ui.text());
    }

    private static String keyName(int code) {
        if (code <= 0) return "None";
        String n = GLFW.glfwGetKeyName(code, 0);
        if (n != null) return n.toUpperCase();
        switch (code) {
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT: return "LALT";
            case GLFW.GLFW_KEY_SPACE: return "SPACE";
            case GLFW.GLFW_KEY_TAB: return "TAB";
            default: return "KEY " + code;
        }
    }

    // ================================================================ HUD MODULES
    private static final String[] HUD_GLYPH = {"@redstone","@lead","@clock","@map","@compass","@sugar","@lever","@clock","@writable_book","@redstone_lamp","@torch","@ender_chest","@apple","@cooked_beef","@experience_bottle","@daylight_detector","@potion","@netherite_sword","@diamond_sword","@anvil","@diamond_chestplate","@totem_of_undying","@end_crystal","@golden_apple","@ender_pearl","@splash_potion","@arrow","@chest","@comparator","@music_disc_cat","@paper","@glowstone"};
    private static final int[] CAT_COL = {0xFF3D8BFF, 0xFFFF5C5C, 0xFFFFB02E, 0xFF3DDC84, 0xFFB07CFF};

    private void hudTab(int x, int y, int w, int h) {
        Config c = Config.get();
        Ui.text("HUD Modules", x, y, Ui.text());
        Ui.textR(HudRenderer.KEYS.length + " modules", x + w, y, Ui.muted());
        int by = y + 14, bw = (w - 8) / 3;
        if (Ui.button("HUD Editor", x, by, bw, 16, true)) after = () -> minecraft.setScreen(new HudEditorScreen(minecraft.screen));
        if (Ui.button("All on", x + bw + 4, by, bw, 16, false)) Config.setAll(true);
        if (Ui.button("All off", x + 2 * (bw + 4), by, bw, 16, false)) Config.setAll(false);

        int top = by + 22, lh = y + h - top;
        int lw = w / 2 - 14;
        int rx = x + lw + 10, rw = w - lw - 10;
        splitX = rx - 4;

        // left: module list grouped by category
        int rows = HudRenderer.KEYS.length * 22 + HudRenderer.CAT_NAMES.length * 14;
        maxL = Math.max(0, rows - lh);
        scrollL = Math.max(0, Math.min(maxL, scrollL));
        Ui.clip(x, top, lw + 4, lh);
        int ry = top - scrollL;
        for (int cat = 0; cat < HudRenderer.CAT_NAMES.length; cat++) {
            Ui.text(Tr.t(HudRenderer.CAT_NAMES[cat]).toUpperCase(), x + 2, ry + 1, Ui.muted());
            ry += 14;
            for (int i = 0; i < HudRenderer.KEYS.length; i++) {
                if (HudRenderer.CAT[i] != cat) continue;
                String k = HudRenderer.KEYS[i];
                Config.Hud hh = c.hud.get(k);
                boolean selRow = k.equals(hudSel);
                Ui.card(x, ry, lw, 20, Ui.hover(x, ry, lw, 20), selRow);
                if (Ui.hover(x, ry, lw - 36, 20) && Ui.pressed) { hudSel = k; Ui.pressed = false; scrollR = 0; }
                badge(HUD_GLYPH[i], CAT_COL[HudRenderer.CAT[i]], x + 3, ry + 2);
                Ui.text(Ui.fit(HudRenderer.LABELS[i], lw - 76), x + 23, ry + 6, Ui.text());
                Ui.rrect(x + lw - 50, ry + 5, 10, 10, 2, hh.color);
                if (Ui.toggle(x + lw - 34, ry + 3, hh.on)) hh.on = !hh.on;
                ry += 22;
            }
        }
        Ui.unclip();
        if (maxL > 0) {
            int bh = Math.max(14, lh * lh / rows);
            Ui.rrect(x + lw + 1, top + (int) ((lh - bh) * (scrollL / (float) maxL)), 2, bh, 1, Ui.alpha(Ui.accent(), 200));
        }

        // right: settings of the selected module (own scroll)
        Ui.card(rx - 5, top - 3, rw + 10, lh + 6, false, false);
        Ui.clip(rx - 5, top - 3, rw + 10, lh + 6);
        viewTop = top - 3; viewBot = top + lh + 3;
        int end = hudSettings(rx, top - scrollR, rw);
        Ui.unclip();
        maxR = Math.max(0, end + scrollR - (top + lh));
        scrollR = Math.max(0, Math.min(maxR, scrollR));
        if (maxR > 0) {
            int bh = Math.max(14, lh * lh / (end + scrollR - top));
            Ui.rrect(rx + rw + 2, top + (int) ((lh - bh) * (scrollR / (float) maxR)), 2, bh, 1, Ui.alpha(Ui.accent(), 200));
        }
    }

    private int hudSettings(int rx, int py, int rw) {
        Config.Hud sh = Config.get().hud.get(hudSel);
        int idx = HudRenderer.indexOf(hudSel);
        Ui.text(HudRenderer.LABELS[idx], rx, py, Ui.accent());
        py += 15;
        sh.on = tog("Enabled", rx, py, rw, sh.on);
        py += 20;
        if (hudSel.equals("glow")) return glowSettings(rx, py, rw);
        if (hudSel.equals("coords")) {
            sh.cx = tog("Show X", rx, py, rw, sh.cx); py += 20;
            sh.cy = tog("Show Y", rx, py, rw, sh.cy); py += 20;
            sh.cz = tog("Show Z", rx, py, rw, sh.cz); py += 20;
            py += 4;
        }

        if (hudSel.equals("keystrokes")) py = keySettings(rx, py, rw);

        int cwid = (rw - 6) / 3;
        for (int i = 0; i < MODES.length; i++) {
            if (Ui.button(MODES[i], rx + i * (cwid + 3), py, cwid, 15, mode == i)) mode = i;
        }
        py += 20;
        int cur = mode == 0 ? sh.color : mode == 1 ? sh.borderColor : sh.bgColor;
        boolean ce = fold("hudcol", "Edit color", rx, py, rw, cur);
        py += 22;
        if (ce) {
            int nc = Ui.colorEditor("hud" + mode, rx, py, rw, cur);
            if (mode == 0) sh.color = nc; else if (mode == 1) sh.borderColor = nc; else sh.bgColor = nc;
            py += 132;
        }

        sh.chroma = tog("Rainbow text", rx, py, rw, sh.chroma); py += 20;
        sh.label = tog("Show label", rx, py, rw, sh.label); py += 20;
        sh.bg = tog("Background", rx, py, rw, sh.bg); py += 20;
        sh.shadow = tog("Text shadow", rx, py, rw, sh.shadow); py += 20;
        sh.border = tog("Border", rx, py, rw, sh.border); py += 20;
        if (hudSel.equals("spotify")) { sh.showBar = tog("Progress bar", rx, py, rw, sh.showBar); py += 20; }
        py += 4;
        int pct = Math.round(sh.scale * 100);
        pct = sl("scale", "Size", pct + "%", rx, py, rw, pct, 50, 300);
        sh.scale = pct / 100f;
        py += 26;
        sh.bgAlpha = sl("bga", "Background opacity", sh.bgAlpha * 100 / 255 + "%", rx, py, rw, sh.bgAlpha, 0, 255);
        py += 26;
        sh.x = sl("posx", "Position X", String.valueOf(sh.x), rx, py, rw, sh.x, 0, Math.max(10, minecraft.getWindow().getGuiScaledWidth() - 10));
        py += 26;
        sh.y = sl("posy", "Position Y", String.valueOf(sh.y), rx, py, rw, sh.y, 0, Math.max(10, minecraft.getWindow().getGuiScaledHeight() - 10));
        py += 30;
        if (Ui.button("↺  Reset this module", rx, py, rw, 16, false)) Config.resetModule(hudSel);
        return py + 26;
    }

    private static final String[] KEY_STYLES = {"Rounded", "Square", "Outline", "Text only"};

    private int keySettings(int rx, int py, int rw) {
        Config.Keys k = Config.get().keys;
        py = section("Style", rx, py, rw);
        int sw = (rw - 9) / 4;
        for (int i = 0; i < 4; i++) {
            if (Ui.button(KEY_STYLES[i], rx + i * (sw + 3), py, sw, 15, k.style == i)) k.style = i;
        }
        py += 20;
        k.mouse = tog("Mouse buttons", rx, py, rw, k.mouse); py += 20;
        k.space = tog("Space bar", rx, py, rw, k.space); py += 20;
        k.opacity = sl("kop", "Key opacity", k.opacity * 100 / 255 + "%", rx, py, rw, k.opacity, 20, 255); py += 26;
        if (fold("kidle", "Key color", rx, py, rw, k.idle)) { py += 22; k.idle = Ui.colorEditor("kic", rx, py, rw, k.idle); py += 132; } else py += 24;
        if (fold("ktext", "Text color", rx, py, rw, k.text)) { py += 22; k.text = Ui.colorEditor("ktc", rx, py, rw, k.text); py += 132; } else py += 24;
        if (fold("kdown", "Pressed color", rx, py, rw, k.down)) { py += 22; k.down = Ui.colorEditor("kdc", rx, py, rw, k.down); py += 132; } else py += 24;
        py = section("Add keys", rx, py, rw);
        int cw = (rw - 15) / 6;
        for (int i = 0; i < HudRenderer.EXTRA_KEYS.length; i++) {
            String name = HudRenderer.EXTRA_KEYS[i];
            int cx = rx + (i % 6) * (cw + 3), cy = py + (i / 6) * 19;
            boolean on = k.extra.contains(name);
            if (Ui.button(name, cx, cy, cw, 16, on)) {
                if (on) k.extra.remove(name); else k.extra.add(name);
            }
        }
        py += ((HudRenderer.EXTRA_KEYS.length + 5) / 6) * 19 + 6;
        return py;
    }

    // ================================================================ FEATURES
    private int features(int x, int y, int w) {
        Config c = Config.get();
        Config.Fx f = c.fx;
        Ui.text("Features", x, y, Ui.text());
        y += 18;

        y = section("Visual", x, y, w);
        f.noBob = tog("No view bobbing", x, y, w, f.noBob);
        y += 20;
        f.noSprintFov = tog("No sprint FOV effect", x, y, w, f.noSprintFov);
        y += 26;

        y = section("Zoom", x, y, w);
        f.zoom = tog("Zoom (hold key)", x, y, w, f.zoom);
        y += 20;
        rowLabel("Zoom key", x, y);
        keyButton(1, x + w - 110, y, 110);
        y += 24;
        f.zoomFov = sl("zfov", "Zoom FOV", String.valueOf(f.zoomFov), x, y, w, f.zoomFov, 30, 80);
        y += 30;

        y = section("Kill animation", x, y, w);
        f.killAnim = tog("Kill animation", x, y, w, f.killAnim);
        y += 20;
        rowLabel("Effect", x, y);
        int n = XLetxzXStyles.KILL_TYPES.length;
        if (Ui.button("<", x + w - 130, y, 18, 16, false)) f.killType = (f.killType + n - 1) % n;
        Ui.textC(XLetxzXStyles.KILL_TYPES[f.killType], x + w - 65, y + 4, Ui.text());
        if (Ui.button(">", x + w - 18, y, 18, 16, false)) f.killType = (f.killType + 1) % n;
        y += 22;
        f.killSound = tog("Sound", x, y, w, f.killSound);
        y += 20;
        if (Ui.button("Preview effect", x, y, w, 16, false)) {
            after = () -> { if (minecraft.player != null) XLetxzXStyles.playKill(minecraft, minecraft.player, f.killType, f.killSound); else toast("Not in a world."); };
        }
        y += 28;

        y = section("Spotify", x, y, w);
        Config.Hud sp = c.hud.get("spotify"), ly = c.hud.get("lyrics");
        sp.on = tog("Spotify HUD", x, y, w, sp.on); y += 20;
        ly.on = tog("Spotify Lyrics (floating in the world around you)", x, y, w, ly.on); y += 22;
        SpotifyService.Info info = SpotifyService.get();
        String st = !SpotifyService.supported() ? "Windows only"
            : info == null ? (sp.on || ly.on ? "Nothing playing" : "Off")
            : (info.playing ? "Playing: " : "Paused: ") + info.artist + " - " + info.title;
        Ui.text(Ui.fit(st, w), x, y, Ui.muted());
        return y + 20;
    }

    // ================================================================ BLOCK HIGHLIGHT
    private static int hlTab = 0;
    private static final String[] HL_TABS = {"General", "Outline", "Layers", "Fill", "Shape"};
    private static final String[] O_MODES = {"All", "Edges", "Air Exposed", "Concealed Faces"};
    private static final String[] SHAPES = {"Collision Shape", "Full Block"};
    private static final String[] F_MODES = {"Fill", "Air Exposed", "Concealed Faces", "Locked Fill"};

    private int cycle(String label, String[] opts, int v, int x, int y, int w) {
        Ui.text(label, x, y + 4, Ui.text());
        int n = opts.length;
        if (Ui.button("<", x + w - 130, y, 18, 16, false)) v = (v + n - 1) % n;
        Ui.textC(opts[v], x + w - 65, y + 4, Ui.text());
        if (Ui.button(">", x + w - 18, y, 18, 16, false)) v = (v + 1) % n;
        return v;
    }

    private int layerEditor(Config.Layer l, String id, int x, int y, int w) {
        l.on = tog("Enabled", x, y, w, l.on); y += 22;
        if (fold(id + "c", "Color", x, y, w, l.c1)) {
            y += 22;
            Ui.text("Primary", x, y, Ui.muted()); y += 12;
            l.c1 = Ui.colorEditor(id + "1", x, y, w, l.c1); y += 126;
            l.grad = tog("Gradient (secondary color)", x, y, w, l.grad); y += 20;
            if (l.grad) {
                Ui.text("Secondary", x, y, Ui.muted()); y += 12;
                l.c2 = Ui.colorEditor(id + "2", x, y, w, l.c2); y += 126;
            }
            y += 4;
        } else y += 22;
        l.opacity = sl(id + "o", "Opacity", l.opacity + " %", x, y, w, l.opacity, 5, 100); y += 28;
        l.width = sl(id + "w", "Line width", String.format("%.1f px", l.width / 10f), x, y, w, l.width, 5, 100); y += 28;
        l.rainbow = tog("Rainbow", x, y, w, l.rainbow); y += 24;
        if (fold(id + "s", "Scaling", x, y, w, 0)) {
            y += 22;
            l.sizePct = sl(id + "p", "Size (percentage)", l.sizePct + " %", x, y, w, l.sizePct, 50, 200); y += 28;
            l.blocks = sl(id + "b", "Size (blocks)", String.format("%.2f", l.blocks / 100f), x, y, w, l.blocks, -50, 50); y += 30;
        } else y += 22;
        if (fold(id + "d", "Subdivision", x, y, w, 0)) {
            y += 22;
            l.cutCorner = sl(id + "cc", "Cut from corner", l.cutCorner + " %", x, y, w, l.cutCorner, 0, 45); y += 28;
            l.outerMul = sl(id + "om", "Outer thickness multiplier", l.outerMul + " %", x, y, w, l.outerMul, 25, 300); y += 28;
            l.cutCenter = sl(id + "cm", "Cut from center", l.cutCenter + " %", x, y, w, l.cutCenter, 0, 90); y += 28;
            l.innerMul = sl(id + "im", "Inner thickness multiplier", l.innerMul + " %", x, y, w, l.innerMul, 25, 300); y += 30;
        } else y += 24;
        return y;
    }

    private int block(int x, int y, int w) {
        Config.Hl h = Config.get().hl;
        Ui.text("Block Highlight", x, y, Ui.text());
        y += 16;
        int n = HL_TABS.length, bw = (w - 3 * (n - 1)) / n;
        for (int i = 0; i < n; i++) {
            if (Ui.button(HL_TABS[i], x + i * (bw + 3), y, bw, 16, hlTab == i)) hlTab = i;
        }
        y += 26;
        if (hlTab == 0) {
            y = section("General", x, y, w);
            h.renderOn = tog("Enable mod rendering", x, y, w, h.renderOn); y += 20;
            h.keepVanilla = tog("Show vanilla outline", x, y, w, h.keepVanilla); y += 20;
            h.pulse = tog("Pulse (breathing opacity)", x, y, w, h.pulse); y += 26;
            y = section("Rainbow", x, y, w);
            h.rainbowSpeed = sl("hlrs", "Speed", h.rainbowSpeed + "x", x, y, w, h.rainbowSpeed, 1, 10); y += 28;
            h.rbDelay = sl("hlrd", "Delay", h.rbDelay + " ms", x, y, w, h.rbDelay, 0, 1000); y += 28;
            h.rbSat = sl("hlrsa", "Saturation", h.rbSat + " %", x, y, w, h.rbSat, 0, 100); y += 28;
            h.rbBri = sl("hlrb", "Brightness", h.rbBri + " %", x, y, w, h.rbBri, 10, 100); y += 32;
            if (Ui.button("Reset block highlight", x, y, w, 16, false)) { Config.get().hl = new Config.Hl(); toast("Block highlight reset."); }
            y += 28;
        } else if (hlTab == 1) {
            y = section("Outline", x, y, w);
            y = layerEditor(h.o, "hl1", x, y, w);
        } else if (hlTab == 2) {
            y = section("Secondary layer", x, y, w);
            y = layerEditor(h.o2, "hl2", x, y, w);
            y = section("Tertiary layer", x, y, w);
            y = layerEditor(h.o3, "hl3", x, y, w);
        } else if (hlTab == 3) {
            h.fill = tog("Enabled", x, y, w, h.fill); y += 22;
            y = section("Color", x, y, w);
            if (fold("hlfc", "Color", x, y, w, h.fColor)) {
                y += 22;
                Ui.text("Primary", x, y, Ui.muted()); y += 12;
                h.fColor = Ui.colorEditor("hlf1", x, y, w, h.fColor); y += 126;
                h.fGradient = tog("Gradient (secondary color)", x, y, w, h.fGradient); y += 20;
                if (h.fGradient) {
                    Ui.text("Secondary", x, y, Ui.muted()); y += 12;
                    h.fColor2 = Ui.colorEditor("hlf2", x, y, w, h.fColor2); y += 126;
                }
                y += 4;
            } else y += 22;
            h.fOpacity = sl("hlfo", "Opacity", h.fOpacity + " %", x, y, w, h.fOpacity, 5, 100); y += 28;
            h.fRainbow = tog("Rainbow", x, y, w, h.fRainbow); y += 26;
            y = section("Miscellaneous", x, y, w);
            h.fMode = cycle("Mode", F_MODES, h.fMode, x, y, w); y += 28;
        } else {
            y = section("Miscellaneous", x, y, w);
            h.shape = cycle("Shape style", SHAPES, h.shape, x, y, w); y += 24;
            h.oMode = cycle("Face mode", O_MODES, h.oMode, x, y, w); y += 30;
        }
        return y;
    }

    // ================================================================ SEARCH
    private static final String[][] ITEMS = {
        {"Zoom", "2", "Features"}, {"Kill animation", "2", "Features"}, {"Spotify", "2", "Features"}, {"Lyrics", "2", "Features"},
        {"Opacity Editor", "18", "Extras"}, {"Low Fire", "3", "Tweaks"}, {"Low Shield", "3", "Tweaks"}, {"Low Totem", "3", "Tweaks"}, {"Low Totem Pop", "17", "CPvP"},
        {"No Pumpkin overlay", "3", "Tweaks"}, {"No Vignette", "3", "Tweaks"}, {"Hide my armor", "3", "Tweaks"},
        {"Render distance", "4", "Performance"}, {"Simulation distance", "4", "Performance"}, {"VSync", "4", "Performance"}, {"Unlimited FPS", "4", "Performance"},
        {"Shadows", "4", "Performance"}, {"Particles", "4", "Performance"}, {"Clouds", "4", "Performance"}, {"Cull entities", "4", "Performance"}, {"Engine boost", "4", "Performance"},
        {"Block Highlight outline", "6", "Block Highlight"}, {"Block Highlight layers", "6", "Block Highlight"}, {"Block Highlight fill", "6", "Block Highlight"},
        {"Block Highlight shape", "6", "Block Highlight"}, {"Block Highlight rainbow", "6", "Block Highlight"},
        {"Fonts", "7", "Fonts"}, {"Bold text", "7", "Fonts"}, {"Italic text", "7", "Fonts"}, {"Underlined text", "7", "Fonts"},
        {"Menu opacity", "7", "Fonts"}, {"Corner radius", "7", "Fonts"}, {"Snow in menu", "7", "Fonts"},
        {"Hit Color", "9", "Hit Color"}, {"Hit flash color", "9", "Hit Color"}, {"Hit opacity", "9", "Hit Color"},
        {"Shield Status", "13", "Shield Status"}, {"Shield tint", "13", "Shield Status"}, {"Item Physics", "14", "Item Physics"}, {"Motion Blur", "15", "Motion Blur"}, {"Colour Saturation", "16", "Colour Saturation"}, {"Color saturation", "16", "Colour Saturation"}, {"CPvP", "17", "CPvP"}, {"Totem pop size", "17", "CPvP"}, {"Small shield", "17", "CPvP"}, {"No crystal explosion", "17", "CPvP"}, {"No explosions", "17", "CPvP"}, {"No totem sounds", "17", "CPvP"}, {"No crystal sounds", "17", "CPvP"}, {"Small handheld items", "17", "CPvP"}, {"No block break particles", "17", "CPvP"}, {"Nick", "10", "Nick"}, {"Custom name", "10", "Nick"}, {"Item Highlighter", "11", "Item Highlighter"}, {"Highlight items", "11", "Item Highlighter"},
        
        {"Crosshair", "8", "Crosshair"}, {"Custom crosshair", "8", "Crosshair"}, {"Crosshair color", "8", "Crosshair"}, {"Crosshair size", "8", "Crosshair"},
        {"Center dot", "8", "Crosshair"}, {"T shape", "8", "Crosshair"}
    };

    private void results(int x, int y, int w, int h) {
        String q = query.trim().toLowerCase();
        List<String[]> hits = new ArrayList<>();
        for (int i = 0; i < HudRenderer.KEYS.length; i++) {
            String lab = HudRenderer.LABELS[i];
            if (lab.toLowerCase().contains(q)) hits.add(new String[]{lab, "1", "HUD Modules", HudRenderer.KEYS[i]});
        }
        for (String[] it : ITEMS) if (it[0].toLowerCase().contains(q) || it[2].toLowerCase().contains(q)) hits.add(it);
        Ui.text("Results for \"" + query.trim() + "\"", x, y, Ui.text());
        y += 16;
        if (hits.isEmpty()) { Ui.text("Nothing found.", x, y + 2, Ui.muted()); return; }
        Ui.clip(x - 2, y - 2, w + 4, h - 20);
        int yy = y - scroll;
        for (String[] it : hits) {
            boolean hv = Ui.hover(x, yy, w, 20) && Ui.hover(x, y, w, h - 20);
            Ui.rrect(x, yy, w, 20, 3, hv ? Ui.surface2() : Ui.surface());
            int t = Integer.parseInt(it[1]);
            badge(GLYPH[t], GCOL[t], x + 3, yy + 2);
            Ui.text(it[0], x + 24, yy + 6, Ui.text());
            Ui.textR(it[2], x + w - 6, yy + 6, Ui.accent());
            if (hv && Ui.pressed) {
                Ui.pressed = false;
                if (t == HUD && it.length > 3) hudSel = it[3];
                setTab(t);
                query = "";
            }
            yy += 22;
        }
        Ui.unclip();
        maxScroll = Math.max(0, hits.size() * 22 - (h - 20));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
    }

    // ================================================================ FONTS
    private int fonts(int x, int y, int w) {
        Config c = Config.get();
        Ui.text("Fonts", x, y, Ui.text());
        y += 16;
        y = section("Font", x, y, w);
        int fn = Ui.FONTS.length, cols = Math.max(2, Math.min(4, w / 120)), fw = (w - 4 * (cols - 1)) / cols;
        for (int i = 0; i < fn; i++) {
            int fx = x + (i % cols) * (fw + 4), fy = y + (i / cols) * 22;
            if (Ui.button("", fx, fy, fw, 18, c.font == i)) c.font = i;
            Ui.sample(Ui.g, i, Ui.fit(Ui.FONTS[i], fw - 8), fx + 4, fy + 5, 0xFFFFFFFF);
        }
        y += ((fn + cols - 1) / cols) * 22 + 6;
        c.fontBold = tog("Bold text", x, y, w, c.fontBold); y += 20;
        c.fontItalic = tog("Italic text", x, y, w, c.fontItalic); y += 20;
        c.fontUnderline = tog("Underlined text", x, y, w, c.fontUnderline); y += 20;
        Ui.text("Preview: The quick brown fox 0123456789", x, y + 2, Ui.muted()); y += 26;
        y = section("Look", x, y, w);
        c.bgAlpha = sl("cba", "Menu opacity", c.bgAlpha * 100 / 255 + " %", x, y, w, c.bgAlpha, 40, 255); y += 28;
        c.radius = sl("crd", "Corner radius", String.valueOf(c.radius), x, y, w, c.radius, 0, 8); y += 28;
        c.snow = tog("Snow in menu", x, y, w, c.snow); y += 26;
        if (Ui.button("Reset fonts", x, y, w, 16, false)) {
            c.font = 0; c.fontBold = false; c.fontItalic = false; c.fontUnderline = false; c.bgAlpha = 170;
            toast("Fonts reset.");
        }
        return y + 28;
    }

    // ================================================================ NICK
    private int nick(int x, int y, int w) {
        Config.Nick n = Config.get().nick;
        Ui.text("Nick", x, y, Ui.text());
        y += 16;
        n.on = tog("Enabled", x, y, w, n.on); y += 22;
        y = section("Name", x, y, w);
        ctx("nick", "Your nick...", x, y, w);
        y += 24;
        Ui.text("Preview", x, y, Ui.muted());
        Ui.rrect(x + 54, y - 4, Math.max(60, Ui.w(n.name) + 14), 16, 3, 0x88000000);
        Ui.text(n.name.isEmpty() ? "-" : n.name, x + 61, y, 0xFFFFFFFF);
        y += 24;
        if (Ui.button("Reset nick", x, y, w, 16, false)) { Config.get().nick = new Config.Nick(); toast("Nick reset."); }
        return y + 28;
    }

    // ================================================================ SHIELD STATUS
    private int shieldTab(int x, int y, int w) {
        Config.Shield c = Config.get().shield;
        Ui.text("Shield Status", x, y, Ui.text());
        y += 16;
        c.on = tog("Enabled", x, y, w, c.on); y += 22;
        c.fade = tog("Fade over cooldown", x, y, w, c.fade); y += 22;
        y = section("Ready", x, y, w);
        c.tintReady = tog("Tint when ready", x, y, w, c.tintReady); y += 22;
        if (fold("shr", "Ready color", x, y, w, c.ready)) { y += 22; c.ready = Ui.colorEditor("shrc", x, y, w, c.ready); y += 130; } else y += 24;
        y = section("Blocking", x, y, w);
        c.tintBlock = tog("Tint while blocking", x, y, w, c.tintBlock); y += 22;
        if (fold("shb", "Blocking color", x, y, w, c.block)) { y += 22; c.block = Ui.colorEditor("shbc", x, y, w, c.block); y += 130; } else y += 24;
        c.tintRaise = tog("Tint while raising", x, y, w, c.tintRaise); y += 22;
        if (fold("shu", "Raising color", x, y, w, c.raise)) { y += 22; c.raise = Ui.colorEditor("shuc", x, y, w, c.raise); y += 130; } else y += 24;
        y = section("Disabled", x, y, w);
        c.tintDisabled = tog("Tint when disabled", x, y, w, c.tintDisabled); y += 22;
        if (fold("shd", "Disabled color", x, y, w, c.disabled)) { y += 22; c.disabled = Ui.colorEditor("shdc", x, y, w, c.disabled); y += 130; } else y += 24;
        if (Ui.button("Reset", x, y, w, 16, false)) { Config.get().shield = new Config.Shield(); toast("Shield Status reset."); }
        return y + 28;
    }

    private int blurTab(int x, int y, int w) {
        Config.Post c = Config.get().post;
        Ui.text("Motion Blur", x, y, Ui.text());
        y += 16;
        c.blurOn = tog("Enabled", x, y, w, c.blurOn); y += 22;
        c.blur = sl("blurv", "Strength", (c.blur * 20) + " %", x, y, w, c.blur, 1, 5);
        return y + 40;
    }

    private int satTab(int x, int y, int w) {
        Config.Post c = Config.get().post;
        Ui.text("Colour Saturation", x, y, Ui.text());
        y += 16;
        c.satOn = tog("Enabled", x, y, w, c.satOn); y += 22;
        c.sat = sl("satv", "Saturation", (c.sat * 25) + " %", x, y, w, c.sat, 0, 8);
        y += 28;
        if (Ui.button("Reset", x, y, w, 16, false)) { c.sat = 4; c.satOn = false; toast("Saturation reset."); }
        return y + 28;
    }

    // ================================================================ ITEM PHYSICS
    private int physTab(int x, int y, int w) {
        Config.Phys c = Config.get().phys;
        Ui.text("Item Physics", x, y, Ui.text());
        y += 16;
        c.on = tog("Enabled", x, y, w, c.on); y += 22;
        c.flat = tog("Lay items flat on the ground", x, y, w, c.flat); y += 22;
        return y + 28;
    }

    // ================================================================ OPACITY
    private boolean opRemove;

    /** One row of the Opacity editor: item picture on a tile, name + value above the slider, optional X. */
    private int opRow(String id, String itemId, String name, int v, int x, int y, int w, boolean removable) {
        Ui.rrect(x, y, w, 34, Config.get().radius, Ui.panel());
        Ui.rrect(x + 5, y + 5, 24, 24, 4, Ui.alpha(0xFF000000, 90));
        net.minecraft.world.item.Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(itemId));
        if (it != null) Ui.g.renderItem(new net.minecraft.world.item.ItemStack(it), x + 9, y + 9);
        if (v < 100) Ui.rrect(x + 5, y + 5, 24, 24, 4, Ui.alpha(0xFF101018, (100 - v) * 235 / 100));
        int rw = removable ? 34 : 0;
        int nv = Ui.slider(id, name, v + " %", x + 38, y + 6, w - 38 - 8 - rw, v, 0, 100);
        if (removable && Ui.button("X", x + w - 36, y + 8, 30, 18, false)) opRemove = true;
        return nv;
    }

    private static String pretty(String path) {
        String n = path.replace('_', ' ');
        return n.isEmpty() ? n : Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    // ================================================================ OPACITY
    private int opacTab(int x, int y, int w) {
        Config.Opac o = Config.get().opac;
        if (o.items == null) o.items = new java.util.LinkedHashMap<>();
        y = header("totem_of_undying", "Opacity Editor", "100 % = normal, 0 % = invisible. Add any item below.", x, y, w);
        o.fire = opRow("opf", "flint_and_steel", "Fire", o.fire, x, y, w, false); y += 38;
        o.hideOwnFire = togD("No fire on my body", "Hides the flames on your own player while you burn.", x, y, w, o.hideOwnFire); y += 32;
        o.under = opRow("opu", "water_bucket", "Underwater view", o.under, x, y, w, false); y += 38;
        o.pumpkin = opRow("opp", "carved_pumpkin", "Pumpkin overlay", o.pumpkin, x, y, w, false); y += 38;
        o.hotbar = opRow("oph", "chest", "Hotbar", o.hotbar, x, y, w, false); y += 42;
        y = section("Items", x, y, w);
        ctx("iopac", "Add an item (totem, shield, ...)", x, y, w);
        y += 24;
        String q = itemQ.trim().toLowerCase().replace(' ', '_');
        if (!q.isEmpty()) {
            List<String> hits = new ArrayList<>();
            for (net.minecraft.resources.Identifier id : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
                String path = id.getPath();
                if (path.contains(q) && !path.equals("air")) hits.add(path);
            }
            java.util.Collections.sort(hits);
            int shown = 0;
            for (String path : hits) {
                if (shown++ >= 8) break;
                boolean has = o.items.containsKey(path);
                itemRow(path, x, y);
                if (Ui.button(has ? "Added" : "Add", x + w - 56, y + 1, 52, 18, has) && !has) { o.items.put(path, 50); itemQ = ""; }
                y += 24;
            }
            if (hits.isEmpty()) { Ui.text("Nothing found.", x, y + 2, Ui.muted()); y += 20; }
        } else {
            opRemove = false;
            String remove = null;
            for (java.util.Map.Entry<String, Integer> e : o.items.entrySet()) {
                int v = e.getValue() == null ? 100 : e.getValue();
                e.setValue(opRow("opi_" + e.getKey(), e.getKey(), pretty(e.getKey()), v, x, y, w, true));
                if (opRemove) { remove = e.getKey(); opRemove = false; }
                y += 38;
            }
            if (remove != null) o.items.remove(remove);
            if (o.items.isEmpty()) { Ui.text("Items in your hand, with your pack texture.", x, y + 2, Ui.muted()); y += 20; }
        }
        y += 8;
        if (Ui.button("Reset all", x, y, w, 16, false)) { Config.get().opac = new Config.Opac(); toast("Opacity reset."); }
        return y + 28;
    }

    // ================================================================ CPVP
    private int cpvpOn() {
        Config.Cpvp p = Config.get().cpvp; Config.Tweaks t = Config.get().tweaks;
        return (t.lowTotemPop ? 1 : 0) + (t.smallShield ? 1 : 0) + (p.noCrystalExpl ? 1 : 0) + (p.noTotemSnd ? 1 : 0) + (p.noCrystalSnd ? 1 : 0) + (p.noExpl ? 1 : 0) + (p.smallHand ? 1 : 0) + (p.noBreakParticles ? 1 : 0) + (p.itemSizes.containsKey("totem_of_undying") ? 1 : 0);
    }

    private int cpvpTab(int x, int y, int w) {
        Config.Cpvp p = Config.get().cpvp;
        Config.Tweaks t = Config.get().tweaks;
        y = header("totem_of_undying", "CPvP", "Sizes, effects and sounds for crystal PvP", x, y, w);
        y = section("Size", x, y, w);
        { boolean n = togD("Totem pop size", "Makes the totem pop animation smaller.", x, y, w, t.lowTotemPop); if (n != t.lowTotemPop) Tweaks.dirty = true; t.lowTotemPop = n; y += 30; }
        if (t.lowTotemPop) { t.popScale = twSlider("ps", "  Pop size", " %", x, y, w, t.popScale, 10, 100); y += 26; }
        { boolean n = togD("Small shield", "Your shield is drawn smaller in first person.", x, y, w, t.smallShield); if (n != t.smallShield) Tweaks.dirty = true; t.smallShield = n; y += 30; }
        if (t.smallShield) { t.shieldScale = twSlider("ss", "  Shield size", " %", x, y, w, t.shieldScale, 30, 100); y += 26; }
        boolean stOn = p.itemSizes.containsKey("totem_of_undying");
        boolean stNew = togD("Small totem", "The totem in your hand gets smaller.", x, y, w, stOn); y += 30;
        if (stNew && !stOn) p.itemSizes.put("totem_of_undying", 50);
        if (!stNew && stOn) p.itemSizes.remove("totem_of_undying");
        if (stNew) {
            int cur = p.itemSizes.get("totem_of_undying") == null ? 50 : p.itemSizes.get("totem_of_undying");
            p.itemSizes.put("totem_of_undying", sl("stt", "  Totem size", cur + " %", x, y, w, cur, 10, 100)); y += 28;
        }
        p.smallHand = togD("Small handheld items", "Every item in your hand gets smaller.", x, y, w, p.smallHand); y += 30;
        if (p.smallHand) { p.handScale = sl("hsc", "  Item size", p.handScale + " %", x, y, w, p.handScale, 30, 100); y += 28; }
        y += 4;
        y = section("Item sizes", x, y, w);
        p.sizeThird = togD("Also in third person", "Sizes also apply to hands you see from outside (F5, other players).", x, y, w, p.sizeThird); y += 30;
        y = sizePicker(p, x, y, w);
        boolean d = Tweaks.dirty;
        if (d) {
            if (Ui.button("Apply (reloads resources)", x, y, w, 16, true)) { Tweaks.apply(); toast("Applied."); }
            y += 22;
        }
        y += 4;
        y = section("Effects", x, y, w);
        p.noCrystalExpl = togD("No crystal explosion", "Hides the crystal explosion effect.", x, y, w, p.noCrystalExpl); y += 30;
        p.noExpl = togD("No explosions", "Hides all explosion particles.", x, y, w, p.noExpl); y += 30;
        p.noBreakParticles = togD("No block break particles", "Breaking a block shows no particles.", x, y, w, p.noBreakParticles); y += 30;
        y = section("Sounds", x, y, w);
        p.noTotemSnd = togD("No totem sounds", "Mutes the totem pop sound.", x, y, w, p.noTotemSnd); y += 30;
        p.noCrystalSnd = togD("No crystal sounds", "Mutes crystal placing and explosion sounds.", x, y, w, p.noCrystalSnd); y += 30;
        return y + 24;
    }

    /** Per-item size of the item in your hand: search to add, slider per item, X to remove. */
    private int sizePicker(Config.Cpvp p, int x, int y, int w) {
        ctx("isize", "Add an item...", x, y, w);
        y += 24;
        String q = itemQ.trim().toLowerCase().replace(' ', '_');
        if (!q.isEmpty()) {
            List<String> hits = new ArrayList<>();
            for (net.minecraft.resources.Identifier id : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
                String path = id.getPath();
                if (path.contains(q) && !path.equals("air")) hits.add(path);
            }
            java.util.Collections.sort(hits);
            int shown = 0;
            for (String path : hits) {
                if (shown++ >= 8) break;
                boolean has = p.itemSizes.containsKey(path);
                itemRow(path, x, y);
                if (Ui.button(has ? "Added" : "Add", x + w - 56, y + 1, 52, 18, has) && !has) { p.itemSizes.put(path, 50); itemQ = ""; }
                y += 24;
            }
            if (hits.isEmpty()) { Ui.text("Nothing found.", x, y + 2, Ui.muted()); y += 20; }
        } else {
            String remove = null;
            for (java.util.Map.Entry<String, Integer> e : p.itemSizes.entrySet()) {
                String key = e.getKey();
                net.minecraft.world.item.Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(key));
                if (it != null) Ui.g.renderItem(new net.minecraft.world.item.ItemStack(it), x + 2, y + 2);
                int v = e.getValue() == null ? 100 : e.getValue();
                String nm = key.replace('_', ' ');
                int nv = Ui.slider("isz_" + key, nm + ": " + v + " %", "", x + 24, y, w - 24 - 40, v, 10, 200);
                e.setValue(nv);
                if (Ui.button("X", x + w - 36, y + 1, 36, 18, false)) remove = key;
                y += 28;
            }
            if (remove != null) p.itemSizes.remove(remove);
        }
        return y + 4;
    }

    // ================================================================ GLOW ITEMS (HUD module settings)
    private int glowSettings(int x, int py, int w) {
        Config.Glow g = Config.get().glow;
        py = section("Glow", x, py, w);
        g.rainbow = tog("Rainbow glow", x, py, w, g.rainbow); py += 20;
        g.pulse = tog("Pulsing glow", x, py, w, g.pulse); py += 20;
        g.poof = tog("Small cloud when an item disappears", x, py, w, g.poof); py += 20;
        g.outline = tog("Also outline the item", x, py, w, g.outline); py += 20;
        if (!g.rainbow) {
            if (fold("glowc", "Glow color", x, py, w, g.color)) { py += 22; g.color = Ui.colorEditor("glowcol", x, py, w, g.color); py += 130; } else py += 24;
        }
        g.power = sl("glowp", "Glow strength", g.power + " %", x, py, w, g.power, 10, 100); py += 28;
        g.all = tog("Glow every item", x, py, w, g.all); py += 22;
        if (g.all) return py + 12;
        py = section("Only these items", x, py, w);
        py = itemPicker(g.items, "glow", x, py, w);
        return py + 12;
    }

    // ================================================================ ITEM HIGHLIGHTER
    private int items(int x, int y, int w) {
        Config.ItemHl h = Config.get().ihl;
        Ui.text("Item Highlighter", x, y, Ui.text());
        y += 16;
        h.on = tog("Enabled", x, y, w, h.on); y += 22;
        y = section("Color", x, y, w);
        if (fold("ihlcf", "Edit color", x, y, w, h.color)) { y += 22; h.color = Ui.colorEditor("ihlc", x, y, w, h.color); y += 130; } else y += 24;
        h.opacity = sl("ihlo", "Opacity", h.opacity + " %", x, y, w, h.opacity, 10, 100); y += 28;
        y = section("Items", x, y, w);
        y = itemPicker(h.items, "items", x, y, w);
        return y + 12;
    }

    /** Search box + list of chosen items (shared by Item Highlighter and Glow Items). */
    private int itemPicker(java.util.Map<String, Boolean> items, String ctxName, int x, int y, int w) {
        ctx(ctxName, "Search items...", x, y, w);
        y += 24;
        String q = itemQ.trim().toLowerCase().replace(' ', '_');
        if (!q.isEmpty()) {
            List<String> hits = new ArrayList<>();
            for (net.minecraft.resources.Identifier id : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
                String path = id.getPath();
                if (path.contains(q) && !path.equals("air")) hits.add(path);
            }
            java.util.Collections.sort(hits);
            int shown = 0;
            for (String path : hits) {
                if (shown++ >= 8) break;
                boolean has = items.containsKey(path);
                itemRow(path, x, y);
                if (Ui.button(has ? "Added" : "Add", x + w - 56, y + 1, 52, 18, has) && !has) { items.put(path, true); itemQ = ""; }
                y += 24;
            }
            if (hits.isEmpty()) { Ui.text("Nothing found.", x, y + 2, Ui.muted()); y += 20; }
        } else {
            String remove = null;
            for (java.util.Map.Entry<String, Boolean> e : items.entrySet()) {
                itemRow(e.getKey(), x, y);
                boolean on = e.getValue() != null && e.getValue();
                if (Ui.button(on ? "ON" : "OFF", x + w - 80, y + 1, 36, 18, on)) e.setValue(!on);
                if (Ui.button("X", x + w - 40, y + 1, 36, 18, false)) remove = e.getKey();
                y += 24;
            }
            if (remove != null) items.remove(remove);
        }
        return y;
    }

    private void itemRow(String path, int x, int y) {
        net.minecraft.world.item.Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(path));
        if (it != null) Ui.g.renderItem(new net.minecraft.world.item.ItemStack(it), x + 2, y + 2);
        Ui.text(path.toUpperCase(), x + 24, y + 6, Ui.text());
    }

    // ================================================================ HIT COLOR
    private int hit(int x, int y, int w) {
        Config.Hit h = Config.get().hit;
        Ui.text("Hit Color", x, y, Ui.text());
        y += 16;
        h.on = tog("Enabled", x, y, w, h.on); y += 22;
        y = section("Color", x, y, w);
        if (fold("hitcf", "Edit color", x, y, w, h.color)) { y += 22; h.color = Ui.colorEditor("hitc", x, y, w, h.color); y += 130; } else y += 24;
        h.opacity = sl("hito", "Opacity", h.opacity + " %", x, y, w, h.opacity, 10, 100); y += 28;
        Ui.text("Preview", x, y, Ui.muted());
        Ui.rrect(x + 54, y - 3, 40, 14, 3, Ui.alpha(h.color, h.opacity * 255 / 100));
        y += 22;
        if (Ui.button("Reset hit color", x, y, w, 16, false)) { Config.get().hit = new Config.Hit(); toast("Hit color reset."); }
        return y + 28;
    }

    // ================================================================ CROSSHAIR
    private static int by(int y, int i) { return y + 4 + (i / 15); }

    private static final java.util.ArrayDeque<int[]> undo = new java.util.ArrayDeque<>();
    private static int[] clipCross;

    private static void pushUndo(int[] cur) {
        undo.push(cur.clone());
        while (undo.size() > 30) undo.removeLast();
    }

    private static int[] presetCross(int n) {
        int[] g = new int[225];
        for (int yy = 0; yy < 15; yy++) {
            for (int xx = 0; xx < 15; xx++) {
                int dx = xx - 7, dy = yy - 7;
                double d = Math.sqrt(dx * dx + dy * dy);
                boolean on = false;
                switch (n) {
                    case 0: on = (dx == 0 || dy == 0) && Math.abs(dx + dy) >= 3 && Math.abs(dx + dy) <= 5; break;
                    case 1: on = (dx == 0 || dy == 0) && Math.abs(dx + dy) <= 4; break;
                    case 2: on = Math.abs(d - 5) < 0.6; break;
                    case 3: on = dx == 0 && dy == 0; break;
                    case 4: on = (Math.abs(dx) == 5 && Math.abs(dy) >= 3 && Math.abs(dy) <= 5) || (Math.abs(dy) == 5 && Math.abs(dx) >= 3 && Math.abs(dx) <= 5); break;
                    case 5: on = Math.abs(d - 5.5) < 0.6 || ((dx == 0 || dy == 0) && d <= 3); break;
                    case 6: on = Math.abs(d - 4) < 0.6 || ((dx == 0 || dy == 0) && d <= 2); break;
                    case 7: on = ((dx == 0 || dy == 0) && d >= 3 && d <= 6) || (dx == 0 && dy == 0); break;
                    default: on = Math.abs(d - 3.5) < 0.6 || (dx == 0 && dy == 0); break;
                }
                if (on) g[yy * 15 + xx] = 1;
            }
        }
        return g;
    }

    private static int paintVal = -1;
    private int cross(int x, int y, int w) {
        Config.Cross c = Config.get().cross;
        Ui.text("Crosshair", x, y, Ui.text());
        y += 16;
        c.on = tog("Enabled", x, y, w, c.on); y += 22;
        c.custom = tog("Custom crosshair (draw your own)", x, y, w, c.custom); y += 22;
        if (c.cust == null || c.cust.length != 225) c.cust = new int[225];
        if (c.custom) {
            y = section("Editor", x, y, w);
            int bw5 = (w - 4 * 4) / 5;
            if (Ui.button("Undo", x, y, bw5, 16, false) && !undo.isEmpty()) c.cust = undo.pop();
            if (Ui.button("Clear", x + (bw5 + 4), y, bw5, 16, false)) { pushUndo(c.cust); c.cust = new int[225]; }
            if (Ui.button("Copy", x + 2 * (bw5 + 4), y, bw5, 16, false)) { clipCross = c.cust.clone(); toast("Copied."); }
            if (Ui.button("Paste", x + 3 * (bw5 + 4), y, bw5, 16, false) && clipCross != null) { pushUndo(c.cust); c.cust = clipCross.clone(); }
            if (Ui.button("Invert", x + 4 * (bw5 + 4), y, bw5, 16, false)) { pushUndo(c.cust); for (int i = 0; i < 225; i++) c.cust[i] = c.cust[i] == 0 ? 1 : 0; }
            y += 22;
            int cs = 11, gx = x, gy = y;
            Ui.rrect(gx - 2, gy - 2, 15 * cs + 4, 15 * cs + 4, 3, 0xFF0A0B12);
            for (int i = 0; i < 225; i++) {
                int px = gx + (i % 15) * cs, py = gy + (i / 15) * cs;
                boolean mid = i % 15 == 7 || i / 15 == 7;
                Ui.fill(px, py, cs - 1, cs - 1, c.cust[i] != 0 ? (0xFF000000 | (c.color & 0xFFFFFF)) : (mid ? 0xFF22263A : 0xFF171A28));
                if (Ui.down && Ui.hover(px, py, cs, cs)) {
                    if (Ui.pressed) { pushUndo(c.cust); paintVal = c.cust[i] == 0 ? 1 : 0; Ui.pressed = false; }
                    if (paintVal >= 0) c.cust[i] = paintVal;
                }
            }
            if (!Ui.down) paintVal = -1;
            int pvx = gx + 15 * cs + 16;
            Ui.text("Preview", pvx, gy, Ui.muted());
            int sc = Math.max(1, c.cscale), bw = 15 * sc + 8;
            Ui.rrect(pvx, gy + 12, bw, bw, 3, 0xFF3A4A3A);
            for (int i = 0; i < 225; i++) {
                if (c.cust[i] == 0) continue;
                Ui.fill(pvx + 4 + (i % 15) * sc, gy + 16 + (i / 15) * sc, sc, sc, 0xFF000000 | (c.color & 0xFFFFFF));
            }
            y = gy + 15 * cs + 8;
            Ui.text("Presets", x, y + 4, Ui.muted());
            int px0 = x + 52;
            for (int n = 0; n < 9; n++) {
                int bx = px0 + n * 26;
                boolean hv = Ui.hover(bx, y, 24, 24);
                Ui.rrect(bx, y, 24, 24, 3, hv ? Ui.surface2() : Ui.surface());
                int[] pr = presetCross(n);
                for (int i = 0; i < 225; i++) if (pr[i] != 0) Ui.fill(bx + 4 + (i % 15) * 1 + 0, by(y, i), 1, 1, 0xFFFFFFFF);
                if (hv && Ui.pressed) { Ui.pressed = false; pushUndo(c.cust); c.cust = pr; }
            }
            y += 32;
            c.cscale = sl("xcs", "Pixel size", String.valueOf(c.cscale), x, y, w, c.cscale, 1, 4); y += 28;
        }
        y = section("Color", x, y, w);
        c.rainbow = tog("Rainbow", x, y, w, c.rainbow); y += 20;
        if (!c.rainbow) {
            if (fold("xhcf", "Edit color", x, y, w, c.color)) { y += 22; c.color = Ui.colorEditor("xhc", x, y, w, c.color); y += 130; } else y += 24;
        }
        c.opacity = sl("xho", "Opacity", c.opacity + " %", x, y, w, c.opacity, 10, 100); y += 28;
        if (c.custom) {
            if (Ui.button("Reset crosshair", x, y, w, 16, false)) { Config.get().cross = new Config.Cross(); toast("Crosshair reset."); }
            return y + 28;
        }
        y = section("Shape", x, y, w);
        c.size = sl("xhs", "Size", String.valueOf(c.size), x, y, w, c.size, 1, 20); y += 28;
        c.gap = sl("xhg", "Gap", String.valueOf(c.gap), x, y, w, c.gap, 0, 12); y += 28;
        c.thick = sl("xht", "Thickness", String.valueOf(c.thick), x, y, w, c.thick, 1, 6); y += 28;
        c.dot = tog("Center dot", x, y, w, c.dot); y += 20;
        c.outline = tog("Outline", x, y, w, c.outline); y += 20;
        c.tShape = tog("T shape (no top line)", x, y, w, c.tShape); y += 26;
        if (Ui.button("Reset crosshair", x, y, w, 16, false)) { Config.get().cross = new Config.Cross(); toast("Crosshair reset."); }
        return y + 28;
    }

    // ================================================================ TWEAKS

    /** Toggle with a muted description line below the label (clean text, no icon behind it). */
    private boolean togD(String label, String desc, int x, int y, int w, boolean v) {
        Ui.text(label, x, y + 1, Ui.text());
        Ui.text(desc, x, y + 12, Ui.muted());
        return Ui.toggle(x + w - 26, y + 3, v) ? !v : v;
    }

    /** Page header: item picture tile, title and description. */
    private int header(String itemId, String title, String desc, int x, int y, int w) {
        Ui.rrect(x, y, 34, 34, 4, Ui.alpha(0xFF000000, 90));
        net.minecraft.world.item.Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(itemId));
        if (it != null) Ui.g.renderItem(new net.minecraft.world.item.ItemStack(it), x + 9, y + 9);
        Ui.text(title, x + 44, y + 5, Ui.text());
        Ui.text(desc, x + 44, y + 19, Ui.muted());
        return y + 44;
    }

    private boolean twTog(String label, int x, int y, int w, boolean v) {
        boolean nv = tog(label, x, y, w, v);
        if (nv != v) Tweaks.dirty = true;
        return nv;
    }

    private int twSlider(String id, String label, String unit, int x, int y, int w, int v, int min, int max) {
        int nv = sl(id, label, v + unit, x, y, w, v, min, max);
        if (nv != v) Tweaks.dirty = true;
        return nv;
    }

    private int tweaks(int x, int y, int w) {
        Config.Tweaks t = Config.get().tweaks;
        Ui.text("Tweaks", x, y, Ui.text());
        y += 18;

        t.lowFire = tog("Low Fire (first person only)", x, y, w, t.lowFire); y += 20;
        if (t.lowFire) { t.fireDrop = sl("fd", "  Fire drop", t.fireDrop + " px", x, y, w, t.fireDrop, 1, 12); y += 26; }
        t.lowShield = twTog("Low Shield", x, y, w, t.lowShield); y += 20;
        if (t.lowShield) { t.shieldDrop = twSlider("sd", "  Shield drop", "", x, y, w, t.shieldDrop, 1, 10); y += 26; }
        t.lowTotem = twTog("Low Totem (offhand)", x, y, w, t.lowTotem); y += 20;
        if (t.lowTotem) { t.totemDrop = twSlider("td", "  Totem drop", "", x, y, w, t.totemDrop, 1, 8); y += 26; }
        Config.get().fx.hideMyArmor = tog("Hide armor (only mine)", x, y, w, Config.get().fx.hideMyArmor); y += 20;
        t.noVignette = twTog("No vignette", x, y, w, t.noVignette); y += 28;

        boolean d = Tweaks.dirty;
        if (Ui.button(d ? "Apply tweaks (pending changes)" : "Apply tweaks", x, y, w, 20, d)) {
            after = () -> { Tweaks.apply(); toast("Tweaks applied - reloading resources..."); };
        }
        return y + 28;
    }

    // ================================================================ PERFORMANCE
    private int perf(int x, int y, int w) {
        Config.Perf p = Config.get().perf;
        Ui.text("Performance - FPS Boost", x, y, Ui.text());
        Ui.textR(minecraft.getFps() + " FPS", x + w, y, Ui.muted());
        y += 18;
        if (Ui.button("Apply FPS Boost", x, y, w, 22, true)) after = () -> { XLetxzXStyles.applyFpsBoost(minecraft); toast("FPS Boost applied."); };
        y += 32;

        y = section("Presets", x, y, w);
        int pw = (w - 8) / 3;
        if (Ui.button("Balanced", x, y, pw, 16, false)) { preset(p, 12, 8, 100, 1, 2, 5, false); toast("Balanced preset set - press Apply."); }
        if (Ui.button("Max FPS", x + pw + 4, y, pw, 16, false)) { preset(p, 8, 6, 75, 0, 0, 3, true); toast("Max FPS preset set - press Apply."); }
        if (Ui.button("Potato", x + 2 * (pw + 4), y, pw, 16, false)) { preset(p, 4, 5, 50, 0, 0, 3, true); toast("Potato preset set - press Apply."); }
        y += 26;

        y = section("Boost settings", x, y, w);
        p.renderDistance = sl("rd", "Render distance", p.renderDistance + " chunks", x, y, w, p.renderDistance, 2, 32); y += 26;
        p.simDistance = sl("sim", "Simulation distance", p.simDistance + " chunks", x, y, w, p.simDistance, 5, 32); y += 26;
        p.entityDistance = sl("ed", "Entity distance", p.entityDistance + "%", x, y, w, p.entityDistance, 50, 200); y += 26;
        p.biomeBlend = sl("bb", "Biome blend", String.valueOf(p.biomeBlend), x, y, w, p.biomeBlend, 0, 7); y += 26;
        p.mipmap = sl("mm", "Mipmap levels", String.valueOf(p.mipmap), x, y, w, p.mipmap, 0, 4); y += 26;
        p.weather = sl("wr", "Weather radius", String.valueOf(p.weather), x, y, w, p.weather, 3, 10); y += 28;
        p.vsyncOff = tog("Turn VSync off", x, y, w, p.vsyncOff); y += 20;
        p.unlimitedFps = tog("Unlimited FPS", x, y, w, p.unlimitedFps); y += 20;
        p.shadowsOff = tog("Turn entity shadows off", x, y, w, p.shadowsOff); y += 24;

        y = section("Graphics", x, y, w);
        p.particlesMin = tog("Minimal particles", x, y, w, p.particlesMin); y += 20;
        p.cloudsOff = tog("Clouds off", x, y, w, p.cloudsOff); y += 20;
        p.aoOff = tog("Smooth lighting off", x, y, w, p.aoOff); y += 20;
        p.fastLeaves = tog("Fast leaves", x, y, w, p.fastLeaves); y += 20;
        p.noImprovedTrans = tog("Improved transparency off", x, y, w, p.noImprovedTrans); y += 20;
        p.noFade = tog("No chunk fade-in", x, y, w, p.noFade); y += 20;
        p.noMenuBlur = tog("No menu background blur", x, y, w, p.noMenuBlur); y += 24;
        p.autoApply = tog("Apply boost when joining a world", x, y, w, p.autoApply); y += 28;

        y = section("Engine boost", x, y, w);
        p.noAnim = tog("Freeze animated textures", x, y, w, p.noAnim); y += 20;
        p.cullEntities = tog("Hide far entities", x, y, w, p.cullEntities); y += 20;
        if (p.cullEntities) { p.cullDist = sl("cd", "Entity cull distance", p.cullDist + " blocks", x, y, w, p.cullDist, 32, 128); y += 28; }
        return y + 14;
    }

    private static void preset(Config.Perf p, int rd, int sim, int ed, int blend, int mip, int weather, boolean aggressive) {
        p.renderDistance = rd; p.simDistance = sim; p.entityDistance = ed; p.biomeBlend = blend;
        p.mipmap = mip; p.weather = weather;
        p.vsyncOff = true; p.unlimitedFps = true; p.shadowsOff = true;
        p.particlesMin = true; p.cloudsOff = true; p.fastLeaves = true;
        p.noImprovedTrans = true; p.noFade = true; p.noMenuBlur = true;
        p.aoOff = aggressive;
    }

    // ================================================================ PACKS
    private void packs(int x, int y, int w, int h) {
        Ui.text("Texture Packs", x, y, Ui.text());
        Ui.textR(PackManager.packs.size() + " packs", x + w, y, Ui.muted());
        int by = y + 15;
        if (Ui.button("Rescan", x, by, 54, 16, false)) PackManager.rescan();
        if (Ui.button("Folder", x + 58, by, 50, 16, false)) {
            try { OpenUtil.open(PackManager.dir()); } catch (Exception ignored) {}
        }
        if (Ui.button("All off", x + 112, by, 54, 16, false)) PackManager.disableAll();

        int fx = x, fy = by + 21;
        List<String> chips = new ArrayList<>();
        chips.add("All"); chips.add("Active");
        for (String cat : PackManager.CATS) chips.add(cat);
        for (String chip : chips) {
            int cwid = Ui.w(chip) + 12;
            if (fx + cwid > x + w) break;
            if (Ui.button(chip, fx, fy, cwid, 14, filter.equals(chip))) filter = chip;
            fx += cwid + 3;
        }

        List<PackManager.Pack> list = new ArrayList<>();
        for (PackManager.Pack p : PackManager.packs) {
            boolean on = sel.contains(p.id());
            if (filter.equals("All") || (filter.equals("Active") && on) || filter.equals(p.cat)) list.add(p);
        }

        int ly = fy + 20, lh = y + h - ly, rowH = 26;
        maxScroll = Math.max(0, list.size() * rowH - lh);
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        Ui.clip(x, ly, w, lh);
        int rw = w - (maxScroll > 0 ? 7 : 0);
        for (int i = 0; i < list.size(); i++) {
            int ry = ly + i * rowH - scroll;
            if (ry + rowH < ly || ry > ly + lh) continue;
            PackManager.Pack p = list.get(i);
            boolean on = sel.contains(p.id());
            boolean hv = Ui.hover(x, ry, rw, rowH - 3);
            Ui.rrect(x, ry, rw, rowH - 3, Config.get().radius, hv ? Ui.mix(Ui.panel(), 0xFFFFFFFF, 0.08f) : Ui.panel());
            int tile = Ui.mix(Ui.accent(), 0xFF000000, 0.35f + (Math.abs(p.name.hashCode()) % 40) / 100f);
            Ui.rrect(x + 4, ry + 3, 17, 17, 3, tile);
            Ui.textC(p.name.isEmpty() ? "?" : p.name.substring(0, 1).toUpperCase(), x + 12, ry + 8, 0xFFFFFFFF);
            Ui.text(Ui.fit(p.name, rw - 100), x + 28, ry + 3, Ui.text());
            Ui.text(p.cat + " - " + PackManager.sizeText(p), x + 28, ry + 13, Ui.muted());
            if (Ui.toggle(x + rw - 32, ry + 5, on)) PackManager.toggle(p);
        }
        Ui.unclip();
        if (maxScroll > 0) {
            int bh = Math.max(14, lh * lh / (list.size() * rowH));
            int bt = ly + (int) ((lh - bh) * (scroll / (float) maxScroll));
            Ui.rrect(x + w - 4, bt, 3, bh, 1, Ui.alpha(Ui.accent(), 200));
        }
        if (list.isEmpty()) Ui.text("No packs in this view.", x, ly + 6, Ui.muted());
    }

    void closed() {
        Config.save();
        if (Tweaks.dirty) Tweaks.apply();
    }
}
