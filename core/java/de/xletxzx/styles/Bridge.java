package de.xletxzx.styles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Link between the XLetxzX menu (de.xletxzx.visuals) and the features in this package.
 * The menu calls these methods by name; the draw context is passed as a plain Object.
 */
public final class Bridge {
    /** GLFW key code of the menu key (set by the menu every tick). */
    public static volatile int menuKey = 344;

    private static ExtrasPanel extras;
    private static PacksPanel packs;

    private Bridge() {}

    static ExtrasPanel extras() {
        if (extras == null) extras = new ExtrasPanel();
        return extras;
    }

    static PacksPanel packs() {
        if (packs == null) packs = new PacksPanel();
        return packs;
    }

    /** Uses the colors of the menu theme for all panels. */
    private static void theme(int accent) {
        Config c = Config.get();
        c.accent = accent;
        c.bg = Ui.mix(0xFF0C0E17, accent, 0.05f);
        c.panel = Ui.mix(0xFF171A28, accent, 0.10f);
        c.text = 0xFFFFFFFF;
        c.radius = 2;
    }

    public static void setMenuKey(int key) { menuKey = key; }

    public static void renderExtras(Object g, int x, int y, int w, int h, int mx, int my, int accent) {
        theme(accent);
        GuiGraphics gg = (GuiGraphics) g;
        Ui.begin(gg, Minecraft.getInstance().font, mx, my);
        extras().render(gg, x, y, w, h, mx, my, false);
    }

    public static void renderPacks(Object g, int x, int y, int w, int h, int mx, int my, int accent, String search) {
        theme(accent);
        GuiGraphics gg = (GuiGraphics) g;
        Ui.begin(gg, Minecraft.getInstance().font, mx, my);
        packs().render(gg, x, y, w, h, mx, my, search);
    }

    public static void renderPerf(Object g, int x, int y, int w, int h, int mx, int my, int accent) {
        theme(accent);
        GuiGraphics gg = (GuiGraphics) g;
        Ui.begin(gg, Minecraft.getInstance().font, mx, my);
        extras().renderPerf(x, y, w, h);
    }

    public static void renderTexturePacks(Object g, int x, int y, int w, int h, int mx, int my, int accent, String search) {
        theme(accent);
        GuiGraphics gg = (GuiGraphics) g;
        Ui.begin(gg, Minecraft.getInstance().font, mx, my);
        packs().renderManager(gg, x, y, w, h, search);
    }

    /** x, y, w, h of the pack search field inside the PACKS area. */
    public static int[] packSearchRect(int x, int y, int w) { return PacksPanel.searchRect(x, y, w); }

    public static int[] extrasSearchRect(int x, int y) { return new int[]{x + 6, y + 32, ExtrasPanel.SIDE - 12, 16}; }
    public static void setExtrasQuery(String q) { ExtrasPanel.query = q == null ? "" : q; }
    public static String extrasQuery() { return ExtrasPanel.query; }

    public static int[] extrasCtx() { return ExtrasPanel.ctxOn ? new int[]{ExtrasPanel.ctxX, ExtrasPanel.ctxY, ExtrasPanel.ctxW} : null; }
    public static String extrasCtxId() { return ExtrasPanel.ctxId; }
    public static String extrasCtxHint() { return ExtrasPanel.ctxHint; }
    public static String extrasCtxText() { return ExtrasPanel.ctxText(); }
    public static void setExtrasCtxText(String t) { ExtrasPanel.setCtx(t); }

    public static String tr(String s) { return Tr.t(s); }

    /** page 1 = extras, 2 = packs. */
    public static void scroll(int page, double mx, double my, double amount) {
        if (page == 1) extras().mouseScrolled(mx, my, 0, amount);
        else if (page == 3) extras().scrollPerf(amount);
        else if (page == 4) packs().scrollManager(amount);
        else if (page == 5) PresetsPanel.scrollBy(amount);
        else packs().scroll(mx, my, amount);
    }

    public static void menuClosed() {
        if (extras != null) extras.closed();
        if (packs != null) packs.closed();
    }

    public static void rescanPacks() { PacksPanel.rescanPacks(); }

    public static boolean discord() { return Config.get().fx.discord; }

    public static void setDiscord(boolean on) {
        Config.get().fx.discord = on;
        Config.save();
        DiscordRpc.setEnabled(on);
    }

    public static boolean english() { return Config.get().fx.forceEnglish; }

    public static void setEnglish(boolean on) {
        Config.get().fx.forceEnglish = on;
        Config.save();
        if (on) XLetxzXStyles.forceEnglish(Minecraft.getInstance());
    }

    public static int menuAlpha() { return Config.get().bgAlpha; }

    public static String language() { return Minecraft.getInstance().options.languageCode; }

    public static void setLanguage(String code) {
        Config.get().fx.forceEnglish = "en_us".equals(code);
        Config.save();
        XLetxzXStyles.setLang(Minecraft.getInstance(), code);
    }

    public static void fpsBoost() { XLetxzXStyles.applyFpsBoost(Minecraft.getInstance()); }

    public static int hudOn() {
        int n = 0;
        for (String k : HudRenderer.KEYS) if (Config.get().hud.get(k).on) n++;
        return n;
    }

    public static int mixChanged() { return Config.get().mix.size(); }

    public static void renderPresets(Object g, int x, int y, int w, int h, int mx, int my, int accent) {
        theme(accent);
        GuiGraphics gg = (GuiGraphics) g;
        Ui.begin(gg, Minecraft.getInstance().font, mx, my);
        PresetsPanel.render(x, y, w, h);
    }

    public static String[] fontNames() { return Ui.FONTS; }
    public static int fontIndex() { return Config.get().font; }
    public static void setFont(int i) { Config.get().font = i; Config.save(); }
    public static boolean fontFlag(int k) { Config c = Config.get(); return k == 0 ? c.fontBold : k == 1 ? c.fontItalic : c.fontUnderline; }
    public static void setFontFlag(int k, boolean v) {
        Config c = Config.get();
        if (k == 0) c.fontBold = v; else if (k == 1) c.fontItalic = v; else c.fontUnderline = v;
        Config.save();
    }
    public static void drawFontSample(Object g, int i, String s, int x, int y, int color) { Ui.sample((GuiGraphics) g, i, s, x, y, color); }
    public static int textColor() { return Config.get().text; }
    public static void setTextColor(int c) { Config.get().text = c; Config.save(); }
    public static void setMenuAlpha(int a) { Config.get().bgAlpha = Math.max(40, Math.min(255, a)); Config.save(); }
}
