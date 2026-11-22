package de.xletxzx.styles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All settings of XLetxzX Styles. Saved to config/xletxzx_styles.json. */
public class Config {
    /** Settings of one HUD module. */
    public static class Hud {
        public boolean on;
        public boolean center = true;           // lyrics: current line right below the crosshair
        public boolean overlay;          // lyrics: separate window above all apps
        public int ox = 80, oy = 80;     // overlay window position
        public int x, y;
        public int color = 0xFFFFFFFF;
        public float scale = 1.0f;
        public boolean bg = true;
        public int bgColor = 0xFF0B0D18;
        public int bgAlpha = 150;
        public boolean shadow = false;
        public boolean border = false;
        public int borderColor = 0xFF8B3DFF;
        public boolean label = true;     // show the "FPS:" style prefix
        public boolean chroma = false;   // rainbow text
        public boolean showBar = true;   // Spotify progress bar
        public boolean cx = true, cy = true, cz = true;   // coordinates: which axes to show
        public Hud() {}
    }

    /** Keystrokes look and extra keys. */
    public static class Keys {
        public int style = 0;                 // 0 rounded, 1 square, 2 outline, 3 text only
        public boolean mouse = true, space = true;
        public int idle = 0xFF000000, down = 0xFF8B3DFF, text = 0xFFB070FF, downText = 0xFFFFFFFF;
        public int opacity = 120;
        public java.util.List<String> extra = new java.util.ArrayList<>();
    }

    /** Tweaks that are applied through an auto-generated resource pack. */
    public static class Tweaks {
        public boolean lowFire;      public int fireDrop = 6;      // pixels of 16
        public boolean lowShield;    public int shieldDrop = 4;    // model units
        public boolean lowTotem;     public int totemDrop = 3;     // model units
        public boolean lowTotemPop;  public int popScale = 60;     // percent
        public boolean hideArmor;
        public boolean noPumpkin;
        public boolean noVignette;
        public boolean noNausea;
        public boolean smallShield; public int shieldScale = 70;  // percent
    }

    /** Client features that work without a resource pack. */
    public static class Fx {
        public boolean fullbright;
        public boolean zoom = true;
        public int zoomKey = 67;     // GLFW_KEY_C
        public int zoomFov = 30;
        public boolean killAnim;
        public int killType = 0;
        public boolean killSound = true;
        public boolean discord = true;
        public int menuKey = 344;    // GLFW_KEY_RIGHT_SHIFT
        public boolean forceEnglish = true;
        public boolean noBob;
        public boolean hideMyArmor;
        public boolean noSprintFov;
    }

    /** Block highlight (outline + fill of the block you look at). */
    /** One outline layer of the block highlight. */
    public static class Layer {
        public boolean on = false, grad = false, rainbow = false;
        public int c1 = 0xFF8B3DFF, c2 = 0xFF3DD5FF;
        public int opacity = 100;
        public int width = 20;         // tenths of a pixel
        public int sizePct = 100;      // 50..200
        public int blocks = 0;         // hundredths of a block, -50..50
        public int cutCorner = 0, cutCenter = 0, outerMul = 100, innerMul = 100;   // subdivision
        Layer() {}
        Layer(boolean on, int c1, int c2, int opacity, int width, int cutCenter, int outerMul, int innerMul) {
            this.on = on; this.c1 = c1; this.c2 = c2; this.grad = true; this.opacity = opacity; this.width = width;
            this.cutCenter = cutCenter; this.outerMul = outerMul; this.innerMul = innerMul;
        }
    }

    public static class Hl {
        public boolean renderOn = true;        // Enable mod rendering
        public boolean keepVanilla = false;    // Show vanilla outline
        public Layer o = new Layer(true, 0xFF3D3C3C, 0xFF787272, 50, 25, 25, 100, 100),
            o2 = new Layer(true, 0xFFFFFFFF, 0xFF4A4A4A, 100, 50, 25, 200, 50),
            o3 = new Layer(false, 0xFF000000, 0xFFFFFFFF, 100, 20, 0, 100, 100);
        public int oMode = 0;          // 0 All, 1 Edges, 2 Air Exposed, 3 Concealed Faces
        public int shape = 0;          // 0 Collision Shape, 1 Full Block
        public boolean fill = false;
        public int fColor = 0xFF8B3DFF, fColor2 = 0xFF3DD5FF;
        public boolean fGradient = false;
        public int fOpacity = 25;
        public boolean fRainbow = false;
        public int fMode = 0;          // 0 Fill, 1 Air Exposed, 2 Concealed Faces, 3 Locked Fill
        public boolean pulse = false;
        public int rainbowSpeed = 5;   // 1..10
        public int rbDelay = 250, rbSat = 100, rbBri = 100;
        void fix() {
            if (o == null) o = new Layer();
            if (o2 == null) o2 = new Layer(true, 0xFFFFFFFF, 0xFF4A4A4A, 100, 50, 25, 200, 50);
            if (o3 == null) o3 = new Layer(false, 0xFF000000, 0xFFFFFFFF, 100, 20, 0, 100, 100);
            if (rbSat <= 0) rbSat = 100;
            if (rbBri <= 0) rbBri = 100;
            for (Layer l : new Layer[]{o, o2, o3}) {
                if (l.outerMul <= 0) l.outerMul = 100;
                if (l.innerMul <= 0) l.innerMul = 100;
            }
            if (o.sizePct < 50) o.sizePct = 100;
            if (o2.sizePct < 50) o2.sizePct = 100;
            if (o3.sizePct < 50) o3.sizePct = 100;
        }
    }

    /** Custom crosshair. */
    public static class Cross {
        public boolean on = false;
        public int color = 0xFFFFFFFF;
        public int size = 6, gap = 3, thick = 1, opacity = 100;
        public boolean dot = false, outline = true, rainbow = false, tShape = false;
        public boolean custom = false;
        public int cscale = 2;
        public int[] cust = new int[225];
    }

    /** Color of the flash on hit entities. */
    public static class Hit {
        public boolean on = false;
        public int color = 0xFFFF0000;
        public int opacity = 70;
        public boolean rainbow = false;
    }

    /** Custom name that only you see above your own head. */
    public static class Nick {
        public boolean on = false;
        public String name = "";
    }

    /** Custom scoreboard (sidebar). */
    public static class Sb {
        public boolean on = false, numbers = true, bg = true, shadow = true, title = true, customColor = false;
        public int bgAlpha = 40, scale = 100, offX = 0, offY = 0;
        public int color = 0xFFFFFFFF, bgColor = 0xFF000000;
    }

    /** Shield tint by state (own shield). */
    public static class Shield {
        public boolean on = false, fade = true, tintReady = true, tintBlock = true, tintRaise = true, tintDisabled = true;
        public int ready = 0xFF3DDC84, block = 0xFF3D8BFF, raise = 0xFFFFD800, disabled = 0xFFFF4040;
    }

    /** Screen effects: motion blur and colour saturation. */
    public static class Post {
        public boolean blurOn = false, satOn = false;
        public int blur = 3, sat = 6;
    }

    /** Dropped items lie flat on the ground. */
    public static class Phys {
        public boolean on = false, flat = true;
    }

    /** Highlights chosen items in inventories. */
    public static class ItemHl {
        public boolean on = false;
        public int color = 0xFFFFD800, opacity = 45;
        public Map<String, Boolean> items = new LinkedHashMap<>();
        public ItemHl() {
            items.put("potion", true); items.put("splash_potion", true);
            items.put("golden_apple", true); items.put("totem_of_undying", true);
        }
    }

    /** CPvP visuals and sounds. */
    public static class Cpvp {
        public boolean noCrystalExpl, noExpl, noTotemSnd, noCrystalSnd, smallHand, noBreakParticles;
        public int handScale = 70;
        public boolean sizeThird = true;
        public Map<String, Integer> itemSizes = new LinkedHashMap<>();   // item id -> size in percent
    }

    /** Glow outline on chosen dropped items. */
    public static class Glow {
        public int color = 0xFFFFD800;
        public boolean rainbow = false, pulse = false, all = true, outline = false, poof = true;
        public int power = 100;
        public Map<String, Boolean> items = new LinkedHashMap<>();
        public Glow() {
            items.put("totem_of_undying", true); items.put("golden_apple", true); items.put("enchanted_golden_apple", true);
        }
    }

    /** FPS boost preset. */
    public static class Perf {
        public int renderDistance = 8;
        public int simDistance = 8;
        public boolean vsyncOff = true;
        public boolean unlimitedFps = true;
        public boolean shadowsOff = true;
        public int biomeBlend = 1;
        public int entityDistance = 75;   // percent
        public boolean particlesMin = true, cloudsOff = true, aoOff = true, fastLeaves = true;
        public boolean noImprovedTrans = true, noFade = true, noMenuBlur = true, autoApply = false;
        public int mipmap = 0, weather = 3;
        public boolean cullEntities = false, noAnim = false;
        public int cullDist = 64;
    }

    public int accent = 0xFF8B3DFF;
    public int bg = 0xFF12162B;
    public int panel = 0xFF1B2140;
    public int text = 0xFFFFFFFF;
    public int bgAlpha = 235;
    public int radius = 4;
    public boolean snow = true;
    public int bgIndex = 0;
    public Map<String, Hud> hud = new LinkedHashMap<>();
    /** See-through settings (100 = normal, 0 = invisible). */
    public static class Opac {
        public int fire = 100, under = 100, pumpkin = 100, hotbar = 100;
        public boolean hideOwnFire;
        public java.util.Map<String, Integer> items = new java.util.LinkedHashMap<>();
        boolean any() { return hideOwnFire || fire != 100 || under != 100 || pumpkin != 100 || hotbar != 100 || (items != null && !items.isEmpty()); }
    }
    public Opac opac = new Opac();
    public Tweaks tweaks = new Tweaks();
    public Fx fx = new Fx();
    public Perf perf = new Perf();
    public Hl hl = new Hl();
    public int hlVer = 0;
    public int uiVer = 0;
    public int font = 0;
    public boolean fontItalic = false, fontUnderline = false;
    public boolean fontBold = false;
    public transient boolean legacyTweaks;
    public Cross cross = new Cross();
    public Hit hit = new Hit();
    public Keys keys = new Keys();
    public Nick nick = new Nick();
    public Sb sb = new Sb();
    public ItemHl ihl = new ItemHl();
    public Cpvp cpvp = new Cpvp();
    public Glow glow = new Glow();
    public Shield shield = new Shield();
    public Phys phys = new Phys();
    public Post post = new Post();
    public int hudVer = 0;
    public boolean packMerged = false;
    public String editPack = "";
    public List<String> installedPacks = new ArrayList<>();
    /** Texture mixer: tile id -> pack file name. */
    public Map<String, String> mix = new LinkedHashMap<>();
    public List<String> favTiles = new ArrayList<>();
    public int presetStart = 0;                  // 0 nothing, 1 last, 2 default
    public String presetLast = "", presetDefault = "";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config inst;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("xletxzx_styles.json");
    }

    public static Config get() {
        if (inst == null) load();
        return inst;
    }

    public static void load() {
        try {
            if (Files.exists(file())) inst = GSON.fromJson(Files.readString(file()), Config.class);
        } catch (Exception ignored) {}
        if (inst == null) inst = new Config();
        inst.fix();
    }

    /** Replaces the settings by n but keeps the texture choices, keys, language and look. */
    static void restore(Config n) {
        Config o = get();
        n.mix = o.mix; n.installedPacks = o.installedPacks; n.favTiles = o.favTiles; n.editPack = o.editPack;
        n.accent = o.accent; n.bg = o.bg; n.panel = o.panel; n.text = o.text; n.bgAlpha = o.bgAlpha; n.radius = o.radius;
        n.snow = o.snow; n.bgIndex = o.bgIndex; n.font = o.font; n.fontItalic = o.fontItalic; n.fontUnderline = o.fontUnderline;
        n.packMerged = o.packMerged;
        n.presetStart = o.presetStart; n.presetLast = o.presetLast; n.presetDefault = o.presetDefault;
        if (n.fx == null) n.fx = new Fx();
        n.fx.menuKey = o.fx.menuKey; n.fx.forceEnglish = o.fx.forceEnglish; n.fx.discord = o.fx.discord;
        n.fix();
        inst = n;
        save();
    }

    static String toJson(Config c) { return GSON.toJson(c); }

    static Config fromJson(String json) {
        try { return GSON.fromJson(json, Config.class); } catch (Exception e) { return null; }
    }

    public static void save() {
        try { Files.writeString(file(), GSON.toJson(get())); } catch (Exception ignored) {}
    }

    private static Hud defaultHud(String k) {
        int i = HudRenderer.indexOf(k);
        Hud h = new Hud();
        h.x = 4 + (i / 9) * 118;
        h.y = 4 + (i % 9) * 22;
        h.on = k.equals("fps") || k.equals("coords") || k.equals("cps");
        return h;
    }

    private void fix() {
        if (hud == null) hud = new LinkedHashMap<>();
        if (tweaks == null) tweaks = new Tweaks();
        if (fx == null) fx = new Fx();
        if (fx.menuKey <= 0) fx.menuKey = 344;
        // options removed because they gave an unfair advantage or a disadvantage
        if (tweaks.hideArmor || tweaks.noNausea || fx.fullbright) legacyTweaks = true;
        tweaks.hideArmor = false; tweaks.noNausea = false; fx.fullbright = false;
        if (perf != null && perf.cullDist < 32) perf.cullDist = 64;
        if (perf == null) perf = new Perf();
        if (hl == null) hl = new Hl();
        if (hlVer < 2) { hl = new Hl(); hlVer = 2; }
        hl.fix();
        if (uiVer < 1) { bgAlpha = 170; uiVer = 1; }
        if (cross == null) cross = new Cross();
        if (nick == null) nick = new Nick();
        if (sb == null) sb = new Sb();
        if (ihl == null) ihl = new ItemHl();
        if (shield == null) shield = new Shield();
        if (phys == null) phys = new Phys();
        if (post == null) post = new Post();
        if (keys == null) keys = new Keys();
        if (keys.extra == null) keys.extra = new java.util.ArrayList<>();
        if (ihl.items == null) ihl.items = new LinkedHashMap<>();
        if (hit == null) hit = new Hit();
        if (cpvp == null) cpvp = new Cpvp();
        if (cpvp.itemSizes == null) cpvp.itemSizes = new LinkedHashMap<>();
        if (glow == null) glow = new Glow();
        if (glow.items == null) glow.items = new LinkedHashMap<>();
        if (editPack == null) editPack = "";
        if (installedPacks == null) installedPacks = new ArrayList<>();
        if (mix == null) mix = new LinkedHashMap<>();
        if (favTiles == null) favTiles = new ArrayList<>();
        if (presetLast == null) presetLast = "";
        if (presetDefault == null) presetDefault = "";
        for (String k : HudRenderer.KEYS) hud.putIfAbsent(k, defaultHud(k));
        if (hudVer < 1) {
            Hud se = hud.get("session");
            if (se != null) { se.color = 0xFFB98CFF; se.bg = true; se.border = true; se.bgColor = 0xFF1B1030; se.bgAlpha = 150; se.borderColor = 0xFF8B3DFF; }
            hudVer = 1;
        }
        if (hudVer < 2) { keys = new Keys(); hudVer = 2; }
        for (Hud h : hud.values()) if (h.scale < 0.3f || h.scale > 4f) h.scale = 1f;
    }

    public static void resetHudPositions() {
        for (String k : HudRenderer.KEYS) {
            Hud d = defaultHud(k), h = get().hud.get(k);
            h.x = d.x; h.y = d.y;
        }
        save();
    }

    public static void resetModule(String k) {
        Hud d = defaultHud(k);
        Hud h = get().hud.get(k);
        d.on = h.on;
        get().hud.put(k, d);
        save();
    }

    public static void setAll(boolean on) {
        for (Hud h : get().hud.values()) h.on = on;
        save();
    }
}
