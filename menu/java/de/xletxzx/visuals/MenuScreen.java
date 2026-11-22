package de.xletxzx.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MenuScreen extends Screen {
    private static final Identifier LOGO = Identifier.of("xletxzx", "icon.png");
    private static final String VERSION = "1.0";
    private static final String DISCORD_URL = "https://discord.gg/XLetxzX";

    private static final String[] TABS = {"HOME", "TxT", "EXTRAS"};
    private static final int[] TAB_PAGES = {0, 9, 1};
    private static final int HOME = 0, EXTRAS = 1, DESIGN = 2, CREDITS = 3, IMPORT = 4, SETTINGS = 5,
            MODLIST = 6, CHANGELOG = 7, PRESETS = 8, PACKS = 9;

    // ---- tour (tutorial). Targets: 0 = centre panel, 1 = tabs, 2 = TxT tab, 3 = EXTRAS tab, 4 = look icon, 5 = settings icon
    private static final String[][] TOUR = {
        {"Welcome to XLetxzX Styles", "This short tour shows you around. You can skip it at any time."},
        {"Tabs", "HOME, TxT and EXTRAS at the top are the main pages of the mod."},
        {"TxT textures", "Pick the texture of every PvP item from any of your resource packs. Click Apply when you're done."},
        {"Extras", "HUD modules, Spotify HUD and lyrics, zoom, totem pop chams and lots of small tweaks."},
        {"Look & backgrounds", "Themes, colours, fonts and animated backgrounds for this menu."},
        {"Settings and keys", "Language, Discord status and the tour itself. The menu and zoom keys are in Minecraft: Options > Controls > Key Binds > XLetxzX Styles."}
    };
    private static final int[] TOUR_TARGET = {0, 1, 2, 3, 4, 5};
    private int tourStep = -1;
    private boolean tourOffered;
    private String[] presetNames = new String[0], presetInfo = new String[0];

    private static final String[] LOOK_NAMES = {"Modern", "Minecraft", "Classic", "Glass", "Flat"};
    private static final String[] BG_NAMES = {
        "Winter night", "Gradient", "Dark", "Image",
        "Aurora", "Starfield", "Matrix rain", "Nether embers", "Ocean waves", "Cyber grid",
        "Bubbles", "Fireflies", "Rain", "Plasma", "Warp speed", "Snowfall", "Disco", "Pulse rings",
        "Meteors", "Sunset sky",
        "Midnight Blue", "Charcoal", "Forest", "Crimson", "Deep Ocean", "Sunset", "Violet Haze",
        "Mint", "Slate", "Sakura",
        "Christmas Night"
    };
    // static gradients, index = background index - 20
    private static final int[][] STATIC_BG = {
        {0xFF0B1030, 0xFF1B2A6B}, {0xFF1A1A1F, 0xFF2E2E36}, {0xFF0B2A14, 0xFF1F5A2C},
        {0xFF2A0508, 0xFF6B1018}, {0xFF021A2E, 0xFF0B4A6B}, {0xFF2B1038, 0xFFFF8A4D},
        {0xFF1E1038, 0xFF5B3A9E}, {0xFF0B2A2A, 0xFF2FB38E}, {0xFF20252E, 0xFF4A5566},
        {0xFF3A1230, 0xFFFF9EC8}
    };
    private static final String[] THEME_NAMES = {
        "Purple Night", "Classic Christmas", "Snowy Winter", "Christmas Night", "Golden Christmas",
        "Aurora Glass", "Sunset", "Deep Ocean", "Nether", "The End", "Forest", "Candy",
        "Midnight", "Terminal", "Paper", "Vanilla"
    };
    // {accent color, background, look}
    private static final int[][] THEME_VALS = {
        {0xFF8A5CFF, 0, 0}, {0xFFE8433F, 0, 0}, {0xFF9FD3FF, 0, 3}, {0xFF3DDC84, 30, 0},
        {0xFFFFC857, 0, 0}, {0xFF5CFFC8, 1, 3}, {0xFFFF8A4D, 1, 4}, {0xFF3D8BFF, 1, 0},
        {0xFFFF4D5A, 2, 1}, {0xFFB36BFF, 2, 3}, {0xFF3DDC84, 0, 4}, {0xFFFF5CC8, 1, 0},
        {0xFF5C7CFF, 2, 0}, {0xFF39FF14, 2, 4}, {0xFF444444, 2, 2}, {0xFFAAAAAA, 2, 1}
    };

    // {display name, mod id} - click opens the mod settings
    private static final String[][] FPS_MODS = {
        {"Sodium", "sodium"}, {"Sodium Extra", "sodium-extra"}, {"Entity Culling", "entityculling"},
        {"More Culling", "moreculling"}, {"Dynamic FPS", "dynamic_fps"}, {"ImmediatelyFast", "immediatelyfast"},
        {"Lithium", "lithium"}, {"FerriteCore", "ferritecore"}, {"Krypton", "krypton"},
        {"Bobby", "bobby"}, {"Clumps", "clumps"}, {"Smooth Scroll", "smoothscroll"}
    };
    private static final String[][] VISUAL_MODS = {
        {"Skin Layers 3D", "skinlayers3d"}, {"Wavey Capes", "waveycapes"}, {"More Cosmetics", "morecosmetics"},
        {"Skyboxify", "skyboxify"}, {"LambDynamicLights", "lambdynlights"}, {"Item Physic", "itemphysic"},
        {"Natural Motion Blur", "naturalmotionblur"}, {"No Texture Rotations", "notexturerotations"},
        {"Block Highlight", "custom-block-highlight"}, {"Not Enough Animations", "notenoughanimations"},
        {"Zoomify", "zoomify"}, {"Freelook", "freelook"}, {"Gamma Utils", "gammautils"},
        {"Toggle Name Tags", "togglenametags"}, {"Creamy Keys", "creamykeys"}
    };
    private static final String[] CAT_NAMES = {"FPS & PERFORMANCE", "VISUALS"};
    private static final int[] CAT_COLORS = {0xFF3DDC84, 0xFF8A5CFF};
    private static final String[][][] CATS = {FPS_MODS, VISUAL_MODS};

    // Mods whose settings live inside the Sodium video settings screen
    private static final java.util.Set<String> SODIUM_PAGES = java.util.Set.of(
        "sodium", "sodium-extra", "moreculling", "bobby", "notexturerotations");

    // styled button record (drawn by us, clicked through an invisible vanilla button)
    private static class Btn {
        int x, y, w, h, kind, look;
        String label;
        boolean selected;
        net.minecraft.item.ItemStack icon;
    }

    private static final java.util.Set<String> startupMods = new java.util.HashSet<>();

    /** Remembers which jars were in the mods folder when the game started (to flag new ones after a rescan). */
    public static void snapshotMods() {
        try (java.util.stream.Stream<Path> st = Files.list(FabricLoader.getInstance().getGameDir().resolve("mods"))) {
            st.forEach(p -> startupMods.add(p.getFileName().toString()));
        } catch (Exception ignored) {
        }
    }

    private final List<Btn> btns = new ArrayList<>();
    private final int[] secY = new int[4];
    private int W, H, OX, OY;
    private int dScroll, dMax;
    private boolean listenKey;
    private boolean designClick, designDown, mPrev;
    private String dragId = "";
    private int bgPage = 0;
    private float rawMx, rawMy;
    private final List<int[]> headers = new ArrayList<>();
    private List<String> modFiles = new ArrayList<>();
    private TextFieldWidget searchField;
    private TextFieldWidget linkField;
    private static Text lit(String s) { return Text.literal(Core.tr(s)); }
    private int tw(String s) { return textRenderer.getWidth(Core.tr(s)); }
    private TextFieldWidget packSearch;
    private TextFieldWidget extrasSearch;
    private TextFieldWidget extrasCtx;
    private String ctxLastId = "";
    private static String packQuery = "";
    private static String texQuery = "";
    private String search = "";
    private int scroll = 0;
    private int maxScroll = 0;
    private int listTop = 0, listBottom = 0;
    private final Screen parent;
    private int page = HOME;
    private String note = "";
    private long noteUntil = 0;

    public MenuScreen(Screen parent) {
        super(lit("XLetxzX Styles"));
        this.parent = parent;
    }

    // ---- settings stored in the existing config fields: theme = theme index,
    // ---- background = look * 10 + background mode
    private static int accent() { return THEME_VALS[Config.get().theme % THEME_VALS.length][0]; }
    private static int look() { return Math.floorMod(Config.get().background / 10, 5); }
    private static int bgMode() { return Math.floorMod(Config.get().bgIndex, BG_NAMES.length); }
    private static void setLook(int l) { Config.get().background = l * 10; Config.save(); }
    private static void setBg(int b) { Config.get().bgIndex = b; Config.save(); }

    private void showNote(String s) {
        note = s;
        noteUntil = System.currentTimeMillis() + 5000;
    }

    private void openConfig(String name, String modId) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!modId.equals("sodium") && !modId.equals("sodium-extra")) {
            try {
                Class<?> c = Class.forName("com.terraformersmc.modmenu.ModMenu");
                Object s = c.getMethod("getConfigScreen", String.class, Screen.class).invoke(null, modId, this);
                if (s instanceof Screen scr) {
                    mc.setScreen(scr);
                    return;
                }
            } catch (Throwable t) {
                System.err.println("[XLetxzX] config screen error for " + modId + ": " + t);
            }
        }
        if (SODIUM_PAGES.contains(modId)) {
            try {
                mc.setScreen(new VideoOptionsScreen(this, mc, mc.options));
                return;
            } catch (Throwable t) {
                System.err.println("[XLetxzX] video settings error: " + t);
            }
        }
        showNote(name + " has no in-game settings screen (config file only).");
    }

    /** Opens the vanilla keybind screen (the XLetxzX menu key is listed under "Miscellaneous"). */
    private void openKeybinds() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.setScreen(new KeybindsScreen(this, mc.options));
        } catch (Throwable t) {
            showNote("Could not open the keybind screen.");
        }
    }

    private void openModList() {
        try {
            Class<?> c = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
            Object s = c.getConstructor(Screen.class).newInstance(this);
            if (s instanceof Screen scr) {
                MinecraftClient.getInstance().setScreen(scr);
                return;
            }
        } catch (Throwable ignored) {
        }
        showNote("Could not open Mod Menu.");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setLastEnum(SimpleOption opt) {
        try {
            Object cur = opt.getValue();
            if (cur instanceof Enum<?> e) {
                Object[] all = e.getDeclaringClass().getEnumConstants();
                opt.setValue(all[all.length - 1]);
            }
        } catch (Throwable ignored) {
        }
    }

    private void applyFpsPreset() {
        try {
            GameOptions o = MinecraftClient.getInstance().options;
            o.getViewDistance().setValue(8);
            o.getSimulationDistance().setValue(6);
            setLastEnum(o.getParticles());
            o.getEntityDistanceScaling().setValue(0.5);
            o.getMaxFps().setValue(260);
            o.getEnableVsync().setValue(false);
            o.getBiomeBlendRadius().setValue(1);
            o.write();
            showNote("FPS boost applied (render distance 8, minimal particles, VSync off).");
        } catch (Throwable t) {
            showNote("Could not apply FPS boost.");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEnumIndex(SimpleOption opt, int index) {
        try {
            Object cur = opt.getValue();
            if (cur instanceof Enum<?> e) {
                Object[] all = e.getDeclaringClass().getEnumConstants();
                int i = index < 0 ? all.length + index : Math.min(index, all.length - 1);
                opt.setValue(all[Math.max(0, i)]);
            }
        } catch (Throwable ignored) {
        }
    }

    /** level 0 = balanced, 1 = maximum, -1 = restore defaults. */
    private void applyFps(int level) {
        try {
            GameOptions o = MinecraftClient.getInstance().options;
            if (level < 0) {
                o.getViewDistance().setValue(12);
                o.getSimulationDistance().setValue(12);
                setEnumIndex(o.getParticles(), 0);
                o.getEntityDistanceScaling().setValue(1.0);
                o.getEnableVsync().setValue(true);
                o.getBiomeBlendRadius().setValue(2);
                o.getEntityShadows().setValue(true);
                setEnumIndex(o.getCloudRenderMode(), -1);
                o.write();
                showNote("Video settings restored.");
                return;
            }
            boolean max = level == 1;
            o.getViewDistance().setValue(max ? 6 : 8);
            o.getSimulationDistance().setValue(max ? 5 : 6);
            setEnumIndex(o.getParticles(), -1);
            o.getEntityDistanceScaling().setValue(max ? 0.5 : 0.75);
            o.getMaxFps().setValue(260);
            o.getEnableVsync().setValue(false);
            o.getBiomeBlendRadius().setValue(max ? 0 : 1);
            o.getEntityShadows().setValue(false);
            setEnumIndex(o.getCloudRenderMode(), 0);
            o.write();
            showNote(max ? "FPS boost: maximum applied." : "FPS boost: balanced applied.");
        } catch (Throwable t) {
            showNote("Could not apply FPS boost.");
        }
    }

    private Btn add(int kind, int x, int y, int w, int h, String label, boolean sel, Runnable r) {
        Btn b = new Btn();
        b.kind = kind; b.x = x; b.y = y; b.w = w; b.h = h; b.label = label; b.selected = sel;
        btns.add(b);
        // during the tour only the tour bubble can be clicked
        if (tourStep < 0) addDrawableChild(ButtonWidget.builder(Text.empty(), btn -> r.run()).dimensions(x + OX, y + OY, w, h).build());
        return b;
    }

    private void applyTheme(int i) {
        Config c = Config.get();
        c.theme = i;
        c.background = THEME_VALS[i][2] * 10;
        c.bgIndex = THEME_VALS[i][1];
        Config.save();
        clearAndInit();
    }


    private void startTour() {
        page = HOME;
        tourStep = 0;
        tourOffered = true;
        clearAndInit();
    }

    /** Ends the tour (finished or skipped); it will not open by itself again. */
    private void endTour() {
        tourStep = -1;
        Config.get().tourDone = true;
        Config.save();
        clearAndInit();
    }

    private int tabX0() { return Math.max(118, (W - (TABS.length * 60 + (TABS.length - 1) * 4)) / 2); }

    /** Area of the start page the current tour step points at (window coordinates). */
    private int[] tourRect() {
        int t = TOUR_TARGET[Math.max(0, Math.min(TOUR_TARGET.length - 1, tourStep))];
        int tx0 = tabX0();
        switch (t) {
            case 1: return new int[]{tx0 - 2, 3, TABS.length * 64, 22};
            case 2: return new int[]{tx0 + 64 - 2, 3, 64, 22};
            case 3: return new int[]{tx0 + 128 - 2, 3, 64, 22};
            case 4: return new int[]{W - 78, 3, 24, 22};
            case 5: return new int[]{W - 30, 3, 24, 22};
            default: {
                int img = (W - 176) / 3;
                int cx = (20 + img + 20 + W - 176 - 10) / 2;
                int gy = H < 300 ? 25 : 28;
                int by = Math.max(80, H / 2 - 10 - (H < 300 ? 28 : 0));
                return new int[]{cx - 117, by - 64, 234, gy * 2 + 22 + 62 + 36};
            }
        }
    }

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Tour overlay: dims everything except the highlighted area and shows a text bubble. */
    private void drawTour(DrawContext ctx, int mx, int my, boolean click, int acc) {
        int[] r = tourRect();
        int dim = 0xB0000000;
        ctx.fill(0, 0, W, r[1], dim);
        ctx.fill(0, r[1] + r[3], W, H, dim);
        ctx.fill(0, r[1], r[0], r[1] + r[3], dim);
        ctx.fill(r[0] + r[2], r[1], W, r[1] + r[3], dim);
        frame(ctx, r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2, acc);
        frame(ctx, r[0] - 2, r[1] - 2, r[2] + 4, r[3] + 4, (acc & 0x00FFFFFF) | 0x66000000);

        String[] step = TOUR[tourStep];
        int bw = Math.min(230, W - 40);
        List<net.minecraft.text.OrderedText> lines = textRenderer.wrapLines(lit(step[1]), bw - 20);
        int bh = 52 + lines.size() * 10 + 22;
        int bx, by;
        if (r[0] + r[2] + 14 + bw <= W - 8) {          // right of the highlight
            bx = r[0] + r[2] + 14;
            by = Math.max(36, Math.min(H - 30 - bh, r[1] + Math.min(r[3], 40) / 2 - 20));
        } else {                                          // centred on the window
            bx = (W - bw) / 2;
            by = Math.max(36, (H - bh) / 2);
        }
        ctx.fill(bx + 2, by + 3, bx + bw + 2, by + bh + 3, 0x66000000);
        box(ctx, bx, by, bw, bh, acc, 0xF2141722, true);
        ctx.fill(bx + 1, by + 1, bx + bw - 1, by + 3, acc);
        ctx.drawText(textRenderer, lit("Tour  ·  " + (tourStep + 1) + " / " + TOUR.length), bx + 10, by + 9, acc, false);
        ctx.drawText(textRenderer, lit(step[0]), bx + 10, by + 22, 0xFFFFFFFF, true);
        int ty = by + 36;
        for (net.minecraft.text.OrderedText l : lines) { ctx.drawText(textRenderer, l, bx + 10, ty, 0xFFC8CAD8, false); ty += 10; }

        // buttons
        boolean last = tourStep == TOUR.length - 1;
        String next = last ? "Done" : "Next";
        int nw = Math.max(44, tw(next) + 16), by2 = by + bh - 24;
        int nx = bx + 10;
        boolean hn = inRect(mx, my, nx, by2, nw, 16);
        box(ctx, nx, by2, nw, 16, acc, hn ? (acc | 0xFF303030) : acc, true);
        ctx.drawText(textRenderer, lit(next), nx + (nw - tw(next)) / 2, by2 + 4, 0xFFFFFFFF, true);
        int px = nx + nw + 6, pw2 = Math.max(40, tw("Back") + 14);
        boolean hb = tourStep > 0 && inRect(mx, my, px, by2, pw2, 16);
        if (tourStep > 0) {
            box(ctx, px, by2, pw2, 16, hb ? acc : 0xFF3B4056, hb ? 0xFF262A3C : 0xFF1C1F2E, true);
            ctx.drawText(textRenderer, lit("Back"), px + (pw2 - tw("Back")) / 2, by2 + 4, 0xFFDDDDEE, false);
        }
        String skip = "Skip tour";
        int sx = bx + bw - tw(skip) - 10;
        boolean hs = inRect(mx, my, sx - 3, by2, tw(skip) + 6, 16);
        ctx.drawText(textRenderer, lit(skip), sx, by2 + 4, hs ? 0xFFFFFFFF : 0xFF8F91A8, false);

        if (click) {
            if (hn) { if (last) endTour(); else tourStep++; }
            else if (hb) tourStep--;
            else if (hs) endTour();
        }
    }

    private void openModsFolder() {
        try {
            Files.createDirectories(modsDir());
            net.minecraft.util.Util.getOperatingSystem().open(modsDir().toUri());
        } catch (Throwable t) {
            showNote("Could not open the folder: " + t.getMessage());
        }
    }

    private void computeWindow() {
        int sw = this.width, sh = this.height;
        W = Math.min(sw - 8, Math.max(480, Math.min(900, sw * 84 / 100)));
        W = Math.max(420, Math.min(sw - 8, W * Math.max(60, Math.min(100, Config.get().winScale)) / 100));
        H = Math.min(sh - 8, Math.max(300, Math.min(460, sh * 80 / 100)));
        OX = (sw - W) / 2;
        OY = (sh - H) / 2;
    }


    private Path modsDir() {
        return FabricLoader.getInstance().getGameDir().resolve("mods");
    }

    private static boolean isFabricMod(Path p) {
        try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(p.toFile())) {
            return z.getEntry("fabric.mod.json") != null;
        } catch (Exception e) {
            return false;
        }
    }

    private List<String> listMods() {
        try (java.util.stream.Stream<Path> st = Files.list(modsDir())) {
            List<String> out = new ArrayList<>();
            st.forEach(p -> {
                String n = p.getFileName().toString();
                if (n.toLowerCase().endsWith(".jar")) out.add(n);
            });
            java.util.Collections.sort(out);
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** Drag & drop of .jar files onto the window copies them into the mods folder. */
    @Override
    public void onFilesDropped(List<Path> paths) {
        int ok = 0;
        String last = "";
        for (Path p : paths) {
            String n = p.getFileName().toString();
            if (n.toLowerCase().endsWith(".zip")) {
                try {
                    Path rp = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
                    Files.createDirectories(rp);
                    Files.copy(p, rp.resolve(n), StandardCopyOption.REPLACE_EXISTING);
                    showNote("Added texture pack " + n);
                    Core.call("rescanPacks");
                    page = IMPORT;
                    clearAndInit();
                } catch (Exception e) {
                    showNote("Import failed: " + e.getMessage());
                }
                continue;
            }
            if (!n.toLowerCase().endsWith(".jar")) { showNote("Drop a texture pack (.zip) or a mod (.jar)."); continue; }
            if (!isFabricMod(p)) { showNote(n + " is not a Fabric mod."); continue; }
            try {
                Files.createDirectories(modsDir());
                Files.copy(p, modsDir().resolve(n), StandardCopyOption.REPLACE_EXISTING);
                ok++;
                last = n;
            } catch (Exception e) {
                showNote("Import failed: " + e.getMessage());
            }
        }
        if (ok > 0) {
            showNote(ok == 1 ? "Imported " + last + " - restart the game to load it."
                    : "Imported " + ok + " mods - restart the game to load them.");
            page = IMPORT;
            clearAndInit();
        }
    }

    /** Downloads a mod from a direct https link into the mods folder. */
    private void downloadMod(String raw) {
        final String link = raw == null ? "" : raw.trim();
        if (link.isEmpty()) { showNote("Paste a direct download link first."); return; }
        showNote("Downloading...");
        Thread th = new Thread(() -> {
            String msg;
            try {
                URI u = URI.create(link);
                if (!"https".equalsIgnoreCase(u.getScheme())) throw new IllegalStateException("the link must start with https://");
                HttpClient hc = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
                HttpRequest req = HttpRequest.newBuilder(u).header("User-Agent", "XLetxzX-Visuals").build();
                HttpResponse<InputStream> resp = hc.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() != 200) throw new IllegalStateException("server answered " + resp.statusCode());
                String path = resp.uri().getPath();
                String fn = path.substring(path.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._+-]", "_");
                if (!fn.toLowerCase().endsWith(".jar")) throw new IllegalStateException("the link must point to a .jar file");
                Path dir = modsDir();
                Files.createDirectories(dir);
                Path tmp = dir.resolve(fn + ".part");
                long max = 100L * 1024 * 1024;
                try (InputStream in = resp.body(); OutputStream out = Files.newOutputStream(tmp)) {
                    byte[] buf = new byte[8192];
                    long total = 0;
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        total += n;
                        if (total > max) throw new IllegalStateException("file is too big");
                        out.write(buf, 0, n);
                    }
                }
                if (!isFabricMod(tmp)) {
                    Files.deleteIfExists(tmp);
                    throw new IllegalStateException("that file is not a Fabric mod");
                }
                Files.move(tmp, dir.resolve(fn), StandardCopyOption.REPLACE_EXISTING);
                msg = "Imported " + fn + " - restart the game to load it.";
            } catch (Exception e) {
                msg = "Download failed: " + e.getMessage();
            }
            final String m = msg;
            MinecraftClient.getInstance().execute(() -> {
                showNote(m);
                if (page == IMPORT) clearAndInit();
            });
        }, "XLetxzX-Import");
        th.setDaemon(true);
        th.start();
    }

    private int contentHeight(int cols, String q) {
        int h = 0;
        for (int c = 0; c < CATS.length; c++) {
            int n = 0;
            for (String[] m : CATS[c]) if (m[0].toLowerCase().contains(q)) n++;
            if (n == 0) continue;
            h += 20 + ((n + cols - 1) / cols) * 30 + 6;
        }
        return h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (page == MODLIST) {
            scroll = (int) Math.max(0, scroll - verticalAmount * 28);
            clearAndInit();
            return true;
        }
        if (page == DESIGN) {
            dScroll = Math.max(0, Math.min(dMax, dScroll - (int) (verticalAmount * 28)));
            return true;
        }
        if (page == EXTRAS || page == PACKS || page == SETTINGS || page == IMPORT || page == PRESETS) {
            Core.call("scroll", page == EXTRAS ? 1 : page == PACKS ? 2 : page == SETTINGS ? 3 : page == PRESETS ? 5 : 4, mouseX, mouseY, verticalAmount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void init() {
        Core.clearTr();
        computeWindow();
        btns.clear();
        headers.clear();
        searchField = null;
        linkField = null;
        packSearch = null;
        extrasSearch = null;
        extrasCtx = null;
        ctxLastId = "";
        Config c = Config.get();
        int areaW = W - 40;

        // the tour starts by itself the first time (until it was finished or skipped)
        if (page == HOME && tourStep < 0 && !tourOffered && c.tourAuto && !c.tourDone) {
            tourOffered = true;
            tourStep = 0;
        }

        // tabs
        int tabW = 60;
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            int tx0 = tabX0();
            add(5, tx0 + i * (tabW + 4), 5, tabW, 18, TABS[i], page == TAB_PAGES[i], () -> { page = TAB_PAGES[idx]; scroll = 0; clearAndInit(); });
        }
        // icons (top right)
        add(15, W - 124, 5, 20, 18, "Presets", page == PRESETS, () -> { page = PRESETS; clearAndInit(); });
        add(13, W - 100, 5, 20, 18, "Changelog", page == CHANGELOG, () -> { page = CHANGELOG; clearAndInit(); });
        add(14, W - 76, 5, 20, 18, "Look & backgrounds", page == DESIGN, () -> { page = DESIGN; clearAndInit(); });
        add(6, W - 52, 5, 20, 18, "Texture packs", page == IMPORT, () -> { page = IMPORT; clearAndInit(); });
        add(8, W - 28, 5, 20, 18, "Settings", page == SETTINGS, () -> { page = SETTINGS; clearAndInit(); });

        if (page == HOME) {
            int img = (W - 176) / 3;
            int regionStart = 20 + img + 20;
            int regionEnd = W - 176 - 10;
            int bx = (regionStart + regionEnd) / 2 - 95;
            int gy = H < 300 ? 25 : 28;
            int bh = H < 300 ? 20 : 22;
            int by = Math.max(80, H / 2 - 10 - (H < 300 ? 28 : 0));
            add(1, bx, by, 190, bh, "Continue", false, this::close);
            add(0, bx, by + gy, 190, bh, "TxT textures", false, () -> { page = PACKS; clearAndInit(); });
            add(0, bx, by + gy * 2, 190, bh, "Credits", false, () -> { page = CREDITS; clearAndInit(); });
            // tiles: 2 columns x 3 rows, all the same size, each with an item icon
            int tx = W - 168, ty = 44, tw = 78, th = 40, tg = 4;
            add(10, tx, ty, tw, th, "Extras|" + Core.num("hudOn") + " HUD on", false, () -> { page = EXTRAS; clearAndInit(); }).icon = new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHER_STAR);
            add(10, tx + tw + tg, ty, tw, th, "TxT|" + Core.num("mixChanged") + " changed", false, () -> { page = PACKS; clearAndInit(); }).icon = new net.minecraft.item.ItemStack(net.minecraft.item.Items.END_CRYSTAL);
            add(10, tx, ty + th + tg, tw, th, "Settings|FPS & keys", false, () -> { page = SETTINGS; clearAndInit(); }).icon = new net.minecraft.item.ItemStack(net.minecraft.item.Items.COMPARATOR);
            add(10, tx + tw + tg, ty + th + tg, tw, th, "Look|Themes", false, () -> { page = DESIGN; clearAndInit(); }).icon = new net.minecraft.item.ItemStack(net.minecraft.item.Items.PAINTING);
            add(10, tx, ty + 2 * (th + tg), tw, th, "Changelog|What's new", false, () -> { page = CHANGELOG; clearAndInit(); }).icon = new net.minecraft.item.ItemStack(net.minecraft.item.Items.WRITABLE_BOOK);
            add(10, tx + tw + tg, ty + 2 * (th + tg), tw, th, "Tour|How it works", false, this::startTour).icon = new net.minecraft.item.ItemStack(net.minecraft.item.Items.COMPASS);
        } else if (page == DESIGN) {
            add(0, 20, 36, 60, 16, "< Back", false, () -> { page = HOME; clearAndInit(); });
        } else if (page == SETTINGS) {
            boolean dc = Core.bool("discord");
            int bw = 170;
            add(0, 20, 76, bw, 20, "Keybinds", false, this::openKeybinds);
            add(2, 20, 100, bw, 20, "Tour at start: " + (c.tourAuto ? "ON" : "OFF"), c.tourAuto, () -> {
                c.tourAuto = !c.tourAuto;
                if (c.tourAuto) c.tourDone = false;   // switched on again: show it next time
                Config.save(); clearAndInit();
            });
            add(0, 20, 124, bw, 20, "Credits", false, () -> { page = CREDITS; clearAndInit(); });
            add(2, 20, 148, bw, 20, "3D mode: " + (c.mode3d ? "ON" : "OFF"), c.mode3d, () -> {
                c.mode3d = !c.mode3d;
                Config.save(); clearAndInit();
            });
            add(2, 20, 172, bw, 20, "Discord status: " + (dc ? "ON" : "OFF"), dc, () -> {
                Core.call("setDiscord", !dc); clearAndInit();
            });
            add(0, 20, 196, bw, 20, "Restore video settings", false, () -> applyFps(-1));
            add(2, 20, 220, bw, 20, listenKey ? "Press a key..." : "Menu key: " + XLetxzXVisuals.keyName(), listenKey, () -> { listenKey = !listenKey; clearAndInit(); });
            Object cur = Core.call("language");
            for (int i = 0; i < LANG_CODES.length; i++) {
                final String code = LANG_CODES[i];
                add(2, 20 + (i % 3) * 57, 240 + (i / 3) * 22, 56, 20, "    " + LANG_NAMES[i], code.equals(cur), () -> {
                    Core.call("setLanguage", code); clearAndInit();
                });
            }
        } else if (page == PRESETS) {
            add(0, 20, 36, 60, 16, "< Back", false, () -> { page = HOME; clearAndInit(); });
            ctxLastId = "";
            extrasCtx = new TextFieldWidget(textRenderer, OX + 24, OY + 90, 200, 16, lit("Input"));
            extrasCtx.setMaxLength(32);
            extrasCtx.setVisible(false);
            extrasCtx.setChangedListener(t -> Core.call("setExtrasCtxText", t));
            addDrawableChild(extrasCtx);
        } else if (page == CREDITS) {
            add(0, 20, H - 50, 120, 18, "Back", false, () -> { page = HOME; clearAndInit(); });
        } else if (page == EXTRAS) {
            int[] r = (int[]) Core.call("extrasSearchRect", OX + 12, OY + 36);
            if (r != null) {
                extrasSearch = new TextFieldWidget(textRenderer, r[0], r[1], r[2], r[3], lit("Search"));
                extrasSearch.setMaxLength(30);
                extrasSearch.setPlaceholder(lit("Search settings..."));
                Object q0 = Core.call("extrasQuery");
                extrasSearch.setText(q0 == null ? "" : q0.toString());
                extrasSearch.setChangedListener(t -> Core.call("setExtrasQuery", t));
                addDrawableChild(extrasSearch);
                extrasCtx = new TextFieldWidget(textRenderer, r[0], r[1], 100, 16, lit("Input"));
                extrasCtx.setMaxLength(32);
                extrasCtx.setVisible(false);
                extrasCtx.setChangedListener(t -> Core.call("setExtrasCtxText", t));
                addDrawableChild(extrasCtx);
            }
        } else if (page == IMPORT) {
            int[] r = (int[]) Core.call("packSearchRect", OX + 12, OY + 36, W - 24);
            if (r != null) {
                packSearch = new TextFieldWidget(textRenderer, r[0], r[1], r[2], r[3], lit("Search"));
                packSearch.setMaxLength(40);
                packSearch.setPlaceholder(lit("Search packs..."));
                packSearch.setText(texQuery);
                packSearch.setChangedListener(t -> texQuery = t);
                addDrawableChild(packSearch);
            }
        } else if (page == MODLIST) {
            // MODS: all mods in one list
            int top = 64;
            int bottom = H - 30;
            listTop = top;
            listBottom = bottom;
            searchField = new TextFieldWidget(textRenderer, 20 + OX, 38 + OY, 150, 18, lit("Search"));
            searchField.setMaxLength(40);
            searchField.setPlaceholder(lit("Search mods..."));
            searchField.setText(search);
            searchField.setChangedListener(t -> {
                if (!t.equals(search)) {
                    search = t;
                    scroll = 0;
                    clearAndInit();
                }
            });
            addDrawableChild(searchField);
            if (!search.isEmpty()) setInitialFocus(searchField);
            add(1, 176, 38, 70, 18, "FPS boost", false, () -> applyFps(0));
            add(0, 252, 38, 70, 18, "Mod Menu", false, this::openModList);
            add(0, 328, 38, 70, 18, "Import", false, () -> { page = IMPORT; clearAndInit(); });

            int cols = Math.max(2, Math.min(4, areaW / 150));
            int gap = 6;
            int cw = (areaW - gap * (cols - 1)) / cols;
            int ch = 24;
            String q = search.toLowerCase().trim();
            maxScroll = Math.max(0, contentHeight(cols, q) - (bottom - top));
            scroll = Math.min(scroll, maxScroll);
            int y = top - scroll;
            for (int cat = 0; cat < CATS.length; cat++) {
                List<String[]> vis = new ArrayList<>();
                for (String[] m : CATS[cat]) if (m[0].toLowerCase().contains(q)) vis.add(m);
                if (vis.isEmpty()) continue;
                if (y >= top - 2 && y + 14 <= bottom) headers.add(new int[]{y, cat});
                y += 20;
                for (int i = 0; i < vis.size(); i++) {
                    final String name = vis.get(i)[0];
                    final String id = vis.get(i)[1];
                    int x = 20 + (i % cols) * (cw + gap);
                    int yy = y + (i / cols) * (ch + gap);
                    if (yy >= top && yy + ch <= bottom) {
                        boolean inst = FabricLoader.getInstance().isModLoaded(id);
                        Btn b = add(7, x, yy, cw, ch, name, inst, inst ? () -> openConfig(name, id)
                                : () -> ConfirmLinkScreen.open(this, "https://modrinth.com/mods?q=" + name.replace(" ", "+")));
                        b.look = cat;
                    }
                }
                y += ((vis.size() + cols - 1) / cols) * (ch + gap) + 6;
            }
        } else if (page == PACKS) {
            int[] r = (int[]) Core.call("packSearchRect", OX + 12, OY + 36, W - 24);
            if (r != null) {
                packSearch = new TextFieldWidget(textRenderer, r[0], r[1], r[2], r[3], lit("Search"));
                packSearch.setMaxLength(40);
                packSearch.setPlaceholder(lit("Search packs..."));
                packSearch.setText(packQuery);
                packSearch.setChangedListener(t -> packQuery = t);
                addDrawableChild(packSearch);
            }
        } else if (page == CHANGELOG) {
            add(0, 20, 36, 60, 16, "< Back", false, () -> { page = HOME; clearAndInit(); });
        }

        add(4, 6, H - 20, 74, 16, "Discord", false, () -> ConfirmLinkScreen.open(this, DISCORD_URL));
        add(0, W - 66, H - 20, 60, 16, "Close", false, this::close);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // custom background is drawn in render()
    }

    private void drawCentered(DrawContext ctx, String s, int cx, int y, int color) {
        ctx.drawText(textRenderer, lit(s), cx - tw(s) / 2, y, color, true);
    }

    private static int[] palette(int look, boolean hov) {
        return switch (look) {
            case 1 -> new int[]{hov ? 0xFF7A7A8A : 0xFF6B6B6B, 0xFF000000, 0xFFFFFFFF};
            case 2 -> new int[]{hov ? 0xFFE0E0E0 : 0xFFC8C8C8, 0xFF333333, 0xFF202020};
            case 3 -> new int[]{hov ? 0x88FFFFFF : 0x55FFFFFF, 0x99FFFFFF, 0xFFFFFFFF};
            case 4 -> new int[]{hov ? 0xFF2C2C38 : 0xFF1E1E26, 0xFF1E1E26, 0xFFFFFFFF};
            default -> new int[]{hov ? 0xFF2E2E3C : 0xFF23232E, 0xFF3A3A4C, 0xFFFFFFFF};
        };
    }

    /** Outline only, nothing inside. */
    private void frame(DrawContext ctx, int x, int y, int w, int h, int col) {
        ctx.fill(x, y, x + w, y + 1, col);
        ctx.fill(x, y + h - 1, x + w, y + h, col);
        ctx.fill(x, y, x + 1, y + h, col);
        ctx.fill(x + w - 1, y, x + w, y + h, col);
    }

    private float opNow = 1f;
    private float bgA = 1f;

    private int ba(int c) {
        if (bgA >= 0.999f) return c;
        return (Math.max(0, (int) (((c >>> 24) & 255) * bgA)) << 24) | (c & 0xFFFFFF);
    }

    private void fa(DrawContext ctx, int x1, int y1, int x2, int y2, int c) { ctx.fill(x1, y1, x2, y2, ba(c)); }

    private void fg(DrawContext ctx, int x1, int y1, int x2, int y2, int c1, int c2) { ctx.fillGradient(x1, y1, x2, y2, ba(c1), ba(c2)); }

    private int ao(int c) {
        if (opNow >= 0.97f) return c;
        return (Math.max(0, (int) (((c >>> 24) & 255) * (0.25f + 0.75f * opNow))) << 24) | (c & 0xFFFFFF);
    }

    private void box(DrawContext ctx, int x, int y, int w, int h, int border, int bg, boolean round) {
        border = ao(border);
        bg = ao(bg);
        if (round) {
            ctx.fill(x + 1, y, x + w - 1, y + h, border);
            ctx.fill(x, y + 1, x + w, y + h - 1, border);
        } else {
            ctx.fill(x, y, x + w, y + h, border);
        }
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, bg);
    }

    private void drawBtn(DrawContext ctx, Btn b, int mx, int my) {
        boolean hov = mx >= b.x && mx < b.x + b.w && my >= b.y && my < b.y + b.h;
        int acc = accent();
        int look = look();
        if (b.kind == 9) {
            if (hov) ctx.fill(b.x, b.y, b.x + b.w, b.y + b.h, 0x22FFFFFF);
            return;
        }
        if (b.kind == 10) { drawTile(ctx, b, hov, acc); return; }
        if (b.kind == 12) { drawToggleRow(ctx, b, hov, acc); return; }
        if (b.kind == 15) { drawPresetIcon(ctx, b, hov, acc); return; }
        if (b.kind == 13) { drawPenIcon(ctx, b, hov, acc); return; }
        if (b.kind == 3) {
            drawCard(ctx, b, hov, acc);
            return;
        }
        if (b.kind == 6) {
            drawImportIcon(ctx, b, hov, acc);
            return;
        }
        if (b.kind == 7) {
            drawModCard(ctx, b, hov, acc);
            return;
        }
        if (b.kind == 14) {
            drawLookIcon(ctx, b, hov, acc);
            return;
        }
        if (b.kind == 8) {
            drawGearIcon(ctx, b, hov, acc);
            return;
        }
        boolean d3 = Config.get().mode3d && (b.kind == 0 || b.kind == 1 || b.kind == 2 || b.kind == 4);
        int origY = b.y;
        if (d3 && hov) b.y -= 2;
        int bg, border, text;
        boolean round = look != 1 && look != 2;
        if (b.kind == 4) {
            bg = hov ? 0xFF6D79F5 : 0xFF5865F2; border = bg; text = 0xFFFFFFFF;
        } else if (b.kind == 1) {
            bg = hov ? (acc | 0xFF303030) : acc; border = acc; text = 0xFFFFFFFF;
        } else if (b.kind == 2) {
            bg = hov ? 0xFF262A3C : 0xFF1C1F2E; border = (b.selected || hov) ? acc : 0xFF3B4056; text = 0xFFDDDDEE;
        } else if (b.kind == 5) {
            bg = hov ? 0xFF262A3C : 0x00000000; border = 0x00000000; text = b.selected ? 0xFFFFFFFF : 0xFFAAAACC;
        } else {
            int[] p = palette(look, hov);
            bg = p[0]; border = p[1]; text = p[2];
        }
        if (b.kind == 5) {
            if (hov || b.selected) ctx.fill(b.x, b.y, b.x + b.w, b.y + b.h, hov ? 0x33FFFFFF : 0x22FFFFFF);
            if (b.selected) ctx.fill(b.x, b.y + b.h - 2, b.x + b.w, b.y + b.h, acc);
        } else {
            if (d3) ctx.fill(b.x, b.y + 3, b.x + b.w, b.y + b.h + 3, 0xFF05050A);
            if (b.kind == 0 && b.selected) border = acc;
            if (b.kind == 0 && !hov && !b.selected && look == 0) border = 0xFF2E3246;   // calm outline
            box(ctx, b.x, b.y, b.w, b.h, border, bg, round);
            if (look == 1 && b.kind == 0) ctx.fill(b.x + 1, b.y + 1, b.x + b.w - 1, b.y + 2, 0x55FFFFFF);
        }
        int tw = tw(b.label);
        int tx = b.x + (b.w - tw) / 2;
        if (b.kind == 4) {
            int ix = b.x + (b.w - (23 + tw)) / 2;
            drawDiscordIcon(ctx, ix, b.y + (b.h - 15) / 2);
            tx = ix + 23;
        }
        ctx.drawText(textRenderer, lit(b.label), tx, b.y + (b.h - 8) / 2, text, false);
        b.y = origY;
    }

    /** Cuts a text to a width and adds ".." when it is too long. */
    private String fitText(String s, int maxW) {
        String t = Core.tr(s);
        if (textRenderer.getWidth(t) <= maxW) return t;
        while (t.length() > 1 && textRenderer.getWidth(t + "..") > maxW) t = t.substring(0, t.length() - 1);
        return t + "..";
    }

    /** Start page tile: icon top right, title and a short line below, accent strip on hover. */
    private void drawTile(DrawContext ctx, Btn b, boolean hov, int acc) {
        box(ctx, b.x, b.y, b.w, b.h, hov ? (acc & 0x00FFFFFF) | 0xCC000000 : 0xFF262A3B, hov ? 0xF01C2030 : 0xE6141722, true);
        if (hov) ctx.fill(b.x + 1, b.y + 4, b.x + 3, b.y + b.h - 4, acc);
        boolean icon = b.icon != null && !b.icon.isEmpty();
        if (icon) ctx.drawItem(b.icon, b.x + b.w - 20, b.y + 5);
        String[] parts = b.label.split("\\|", 2);
        int textW = b.w - 14 - (icon ? 20 : 0);
        boolean two = parts.length > 1;
        int ty = two ? b.y + (b.h - 20) / 2 : b.y + (b.h - 8) / 2;
        ctx.drawText(textRenderer, Text.literal(fitText(parts[0], textW)), b.x + 8, ty, 0xFFFFFFFF, false);
        if (two) ctx.drawText(textRenderer, Text.literal(fitText(parts[1], b.w - 14)), b.x + 8, ty + 12, hov ? 0xFFB8BACB : 0xFF8487A3, false);
    }

    private void drawToggleRow(DrawContext ctx, Btn b, boolean hov, int acc) {
        box(ctx, b.x, b.y, b.w, b.h, hov ? acc : 0xFF2B2F42, hov ? 0xFF1F2336 : 0xFF161926, true);
        ctx.drawText(textRenderer, lit(b.label), b.x + 10, b.y + (b.h - 8) / 2, 0xFFE6E6F2, false);
        int sw = 26, sh = 12;
        int sx = b.x + b.w - sw - 10, sy = b.y + (b.h - sh) / 2;
        ctx.fill(sx, sy, sx + sw, sy + sh, b.selected ? acc : 0xFF3B4056);
        int kx = b.selected ? sx + sw - 10 : sx + 2;
        ctx.fill(kx, sy + 2, kx + 8, sy + sh - 2, 0xFFFFFFFF);
    }

    private void drawPenIcon(DrawContext ctx, Btn b, boolean hov, int acc) {
        box(ctx, b.x, b.y, b.w, b.h, (hov || b.selected) ? acc : 0xFF3B4056, hov ? 0xFF262A3C : 0xFF1C1F2E, true);
        int px = b.x + 5, py = b.y + 3;
        for (int i = 0; i < 9; i++) ctx.fill(px + i, py + 9 - i, px + i + 2, py + 10 - i, 0xFFFFFFFF);
        ctx.fill(px - 1, py + 10, px + 3, py + 12, 0xFFFFC857);
        ctx.fill(px + 9, py, px + 11, py + 2, 0xFFFF7A8A);
        if (hov) {
            int tw = tw(b.label);
            ctx.fill(b.x + b.w - tw - 10, b.y + b.h + 4, b.x + b.w, b.y + b.h + 18, 0xEE0B0B12);
            ctx.drawText(textRenderer, lit(b.label), b.x + b.w - tw - 5, b.y + b.h + 8, 0xFFFFFFFF, false);
        }
    }

    /** Small pixel-art Discord face. */
    private static final Identifier DISCORD = Identifier.of("xletxzx", "discord.png");

    private void drawDiscordIcon(DrawContext ctx, int x, int y) {
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, DISCORD, x, y, 0f, 0f, 20, 15, 40, 30, 40, 30);
    }

    private void drawImportIcon(DrawContext ctx, Btn b, boolean hov, int acc) {
        box(ctx, b.x, b.y, b.w, b.h, (hov || b.selected) ? acc : 0xFF3B4056, hov ? 0xFF262A3C : 0xFF1C1F2E, true);
        int cx = b.x + b.w / 2;
        int col = 0xFFFFFFFF;
        ctx.fill(cx - 1, b.y + 3, cx + 1, b.y + 9, col);
        for (int i = 0; i < 4; i++) ctx.fill(cx - 3 + i, b.y + 8 + i, cx + 3 - i, b.y + 9 + i, col);
        ctx.fill(b.x + 4, b.y + b.h - 5, b.x + b.w - 4, b.y + b.h - 4, col);
        ctx.fill(b.x + 4, b.y + b.h - 8, b.x + 5, b.y + b.h - 4, col);
        ctx.fill(b.x + b.w - 5, b.y + b.h - 8, b.x + b.w - 4, b.y + b.h - 4, col);
        if (hov) {
            int tw = tw(b.label);
            ctx.fill(b.x + b.w - tw - 10, b.y + b.h + 4, b.x + b.w, b.y + b.h + 18, 0xEE0B0B12);
            ctx.drawText(textRenderer, lit(b.label), b.x + b.w - tw - 5, b.y + b.h + 8, 0xFFFFFFFF, false);
        }
    }

    /** Small painting icon for looks & backgrounds. */
    private void drawLookIcon(DrawContext ctx, Btn b, boolean hov, int acc) {
        box(ctx, b.x, b.y, b.w, b.h, (hov || b.selected) ? acc : 0xFF3B4056, hov ? 0xFF262A3C : 0xFF1C1F2E, true);
        int x = b.x + 4, y = b.y + 3, w = b.w - 8, h = b.h - 6;
        ctx.fill(x, y, x + w, y + h, 0xFFFFFFFF);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF3D8BFF);
        ctx.fill(x + 1, y + h - 5, x + w - 1, y + h - 1, 0xFF3DDC84);
        ctx.fill(x + 3, y + h - 8, x + 7, y + h - 5, 0xFF2E9E5E);
        ctx.fill(x + w - 5, y + 2, x + w - 2, y + 5, 0xFFFFD27A);
        if (hov) {
            int tw = tw(b.label);
            ctx.fill(b.x + b.w - tw - 10, b.y + b.h + 4, b.x + b.w, b.y + b.h + 18, 0xEE0B0B12);
            ctx.drawText(textRenderer, lit(b.label), b.x + b.w - tw - 5, b.y + b.h + 8, 0xFFFFFFFF, false);
        }
    }

    private void drawPresetIcon(DrawContext ctx, Btn b, boolean hov, int acc) {
        int bg = hov ? 0xFF262A3C : 0xFF1C1F2E;
        box(ctx, b.x, b.y, b.w, b.h, (hov || b.selected) ? acc : 0xFF3B4056, bg, true);
        int cx = b.x + b.w / 2, cy = b.y + b.h / 2, col = 0xFFFFFFFF;
        ctx.fill(cx - 4, cy - 6, cx + 4, cy + 6, col);
        for (int r = 0; r < 4; r++) ctx.fill(cx - r, cy + 3 + r, cx + r + 1, cy + 4 + r, bg);
        if (hov) {
            int tw = tw(b.label);
            ctx.fill(b.x + b.w - tw - 10, b.y + b.h + 4, b.x + b.w, b.y + b.h + 18, 0xEE0B0B12);
            ctx.drawText(textRenderer, lit(b.label), b.x + b.w - tw - 5, b.y + b.h + 8, 0xFFFFFFFF, false);
        }
    }

    private void drawGearIcon(DrawContext ctx, Btn b, boolean hov, int acc) {
        int bg = hov ? 0xFF262A3C : 0xFF1C1F2E;
        box(ctx, b.x, b.y, b.w, b.h, (hov || b.selected) ? acc : 0xFF3B4056, bg, true);
        int cx = b.x + b.w / 2, cy = b.y + b.h / 2;
        int col = 0xFFFFFFFF;
        ctx.fill(cx - 4, cy - 4, cx + 4, cy + 4, col);
        ctx.fill(cx - 1, cy - 6, cx + 1, cy + 6, col);
        ctx.fill(cx - 6, cy - 1, cx + 6, cy + 1, col);
        ctx.fill(cx - 5, cy - 5, cx - 3, cy - 3, col);
        ctx.fill(cx + 3, cy - 5, cx + 5, cy - 3, col);
        ctx.fill(cx - 5, cy + 3, cx - 3, cy + 5, col);
        ctx.fill(cx + 3, cy + 3, cx + 5, cy + 5, col);
        ctx.fill(cx - 2, cy - 2, cx + 2, cy + 2, bg);
        if (hov) {
            int tw = tw(b.label);
            ctx.fill(b.x + b.w - tw - 10, b.y + b.h + 4, b.x + b.w, b.y + b.h + 18, 0xEE0B0B12);
            ctx.drawText(textRenderer, lit(b.label), b.x + b.w - tw - 5, b.y + b.h + 8, 0xFFFFFFFF, false);
        }
    }

    private void drawModCard(DrawContext ctx, Btn b, boolean hov, int acc) {
        int cat = CAT_COLORS[Math.floorMod(b.look, CAT_COLORS.length)];
        if (Config.get().mode3d) ctx.fill(b.x, b.y + 3, b.x + b.w, b.y + b.h + 3, 0xFF05050A);
        box(ctx, b.x, b.y, b.w, b.h, hov ? acc : 0xFF2B2F42, hov ? 0xFF1F2336 : 0xFF161926, true);
        ctx.fill(b.x + 1, b.y + 3, b.x + 3, b.y + b.h - 3, cat);
        String label = b.label;
        int maxW = b.w - 44;
        while (label.length() > 3 && tw(label) > maxW) label = label.substring(0, label.length() - 2);
        if (!label.equals(b.label)) label = label + "..";
        boolean inst = b.selected;
        ctx.fill(b.x + 9, b.y + (b.h - 4) / 2, b.x + 13, b.y + (b.h + 4) / 2, inst ? 0xFF3DDC84 : 0xFF555566);
        ctx.drawText(textRenderer, lit(label), b.x + 17, b.y + (b.h - 8) / 2, inst ? 0xFFE6E6F2 : 0xFF777788, false);
        if (inst) {
            ctx.drawText(textRenderer, lit("\u203A"), b.x + b.w - 12, b.y + (b.h - 8) / 2, hov ? acc : 0xFF666680, false);
        } else {
            ctx.drawText(textRenderer, lit("Get"), b.x + b.w - 24, b.y + (b.h - 8) / 2, hov ? acc : 0xFF8888AA, false);
        }
    }

    private void drawCard(DrawContext ctx, Btn b, boolean hov, int acc) {
        int l = b.look;
        int[] panel = {0xFF1A1A22, 0xFF3A3A3A, 0xFFC8C8C8, 0xFF262640, 0xFF16161C};
        boolean dark = l != 2;
        int border = b.selected ? 0xFFFFFFFF : (hov ? acc : 0xFF2A2A36);
        box(ctx, b.x, b.y, b.w, b.h, border, panel[l], true);
        int lineCol = dark ? 0xFFDDDDEE : 0xFF555555;
        ctx.fill(b.x + 8, b.y + 8, b.x + 8 + b.w / 3, b.y + 10, lineCol);
        ctx.fill(b.x + 8, b.y + 13, b.x + 8 + b.w / 2, b.y + 14, dark ? 0xFF888899 : 0xFF888888);
        int[] p = palette(l, false);
        int bx = b.x + b.w / 5, bw = b.w * 3 / 5, by = b.y + b.h / 2 - 4, bh = Math.max(8, b.h / 5);
        if (l == 0) {
            ctx.fill(bx, by, bx + bw, by + bh, 0xFF8A6CFF);
        } else if (l == 4) {
            ctx.fill(bx, by, bx + bw, by + bh, 0xFF5C8DEB);
        } else if (l == 3) {
            ctx.fill(bx, by, bx + bw, by + bh, 0xFFC8C8D8);
        } else {
            box(ctx, bx, by, bw, bh, p[1], p[0], false);
        }
        ctx.drawText(textRenderer, lit(b.label), b.x + (b.w - tw(b.label)) / 2,
                b.y + b.h - 12, dark ? 0xFFFFFFFF : 0xFF202020, false);
    }

    private static final String[] LANG_CODES = {"de_de", "en_us", "es_es", "pl_pl", "fr_fr", "it_it", "ru_ru"};
    private static final String[] LANG_NAMES = {"Deutsch", "English", "Español", "Polski", "Français", "Italiano", "Русский"};

    /** Small flag (10x7) drawn from plain rectangles. */
    private void drawFlag(DrawContext ctx, int i, int x, int y) {
        int w = 10, h = 7;
        switch (i) {
            case 0 -> { // Germany
                ctx.fill(x, y, x + w, y + 2, 0xFF000000);
                ctx.fill(x, y + 2, x + w, y + 5, 0xFFDD0000);
                ctx.fill(x, y + 5, x + w, y + h, 0xFFFFCE00);
            }
            case 1 -> { // United Kingdom (simplified)
                ctx.fill(x, y, x + w, y + h, 0xFF012169);
                for (int k = 0; k < w; k++) {
                    int d = k * h / w;
                    ctx.fill(x + k, y + d, x + k + 1, y + d + 1, 0xFFFFFFFF);
                    ctx.fill(x + w - 1 - k, y + d, x + w - k, y + d + 1, 0xFFFFFFFF);
                }
                ctx.fill(x, y + 2, x + w, y + 5, 0xFFFFFFFF);
                ctx.fill(x + 4, y, x + 6, y + h, 0xFFFFFFFF);
                ctx.fill(x, y + 3, x + w, y + 4, 0xFFC8102E);
                ctx.fill(x + 4, y, x + 5, y + h, 0xFFC8102E);
                ctx.fill(x + 5, y, x + 6, y + h, 0xFFC8102E);
            }
            case 2 -> { // Spain
                ctx.fill(x, y, x + w, y + h, 0xFFAA151B);
                ctx.fill(x, y + 2, x + w, y + 5, 0xFFF1BF00);
            }
            case 3 -> { // Poland
                ctx.fill(x, y, x + w, y + 4, 0xFFFFFFFF);
                ctx.fill(x, y + 4, x + w, y + h, 0xFFDC143C);
            }
            case 4 -> { // France
                ctx.fill(x, y, x + 3, y + h, 0xFF0055A4);
                ctx.fill(x + 3, y, x + 7, y + h, 0xFFFFFFFF);
                ctx.fill(x + 7, y, x + w, y + h, 0xFFEF4135);
            }
            case 5 -> { // Italy
                ctx.fill(x, y, x + 3, y + h, 0xFF009246);
                ctx.fill(x + 3, y, x + 7, y + h, 0xFFFFFFFF);
                ctx.fill(x + 7, y, x + w, y + h, 0xFFCE2B37);
            }
            default -> { // Russia
                ctx.fill(x, y, x + w, y + 2, 0xFFFFFFFF);
                ctx.fill(x, y + 2, x + w, y + 5, 0xFF0039A6);
                ctx.fill(x, y + 5, x + w, y + h, 0xFFD52B1E);
            }
        }
    }

    private void drawSection(DrawContext ctx, String label, int y) { drawSection(ctx, label, y, W - 20); }

    private void drawSection(DrawContext ctx, String label, int y, int endX) {
        ctx.drawText(textRenderer, lit(label), 20, y, 0xFF8888AA, false);
        ctx.fill(20 + tw(label) + 8, y + 4, endX, y + 5, 0x33FFFFFF);
    }

    private void drawTree(DrawContext ctx, int cx, int baseY, int h, int color) {
        fa(ctx, cx - Math.max(1, h / 14), baseY, cx + Math.max(1, h / 14) + 1, baseY + 8, 0xFF2A1A12);
        for (int row = 0; row < h; row++) {
            float f = row / (float) h;
            float local = (f * 3f) % 1f;
            int half = (int) ((0.25f + 0.75f * local) * (h * 0.12f + f * h * 0.22f));
            fa(ctx, cx - half, baseY - h + row, cx + half + 1, baseY - h + row + 1, color);
        }
    }

    private void drawWinter(DrawContext ctx, long t) {
        fg(ctx, 0, 0, W, H, 0xFF1B2250, 0xFF0A0E22);
        int mr = Math.max(18, H / 9);
        int mx = (int) (W * 0.38), my = (int) (H * 0.38);
        for (int dy = -mr; dy <= mr; dy++) {
            int dx = (int) Math.sqrt(mr * mr - dy * dy);
            fa(ctx, mx - dx, my + dy, mx + dx, my + dy + 1, 0x22FFFFFF);
        }
        for (int i = 0; i < 60; i++) {
            int x = (int) (((i * 7919) % 1000) / 1000f * W);
            int y = (int) (((i * 104729) % 1000) / 1000f * H * 0.7f);
            int a = 0x55 + (int) (0x44 * Math.sin(t / 700.0 + i));
            fa(ctx, x, y, x + 1, y + 1, (a << 24) | 0x00FFFFFF);
        }
        int n = 9, base = H - 24;
        for (int i = 0; i < n; i++) {
            int cx = (int) (W * (i + 1f) / n);
            int h = (int) (H * (0.2f + ((i * 53) % 25) / 100f) * 1.15f);
            drawTree(ctx, cx, base, h, 0xFF111A33);
        }
        for (int i = 0; i < n; i++) {
            int cx = (int) (W * (i + 0.5f) / n) + ((i * 37) % 20 - 10);
            int h = (int) (H * (0.18f + ((i * 53) % 25) / 100f));
            drawTree(ctx, cx, base, h, 0xFF0A0E1A);
            int top = base - h;
            fa(ctx, cx - 2, top, cx + 3, top + 1, 0xAAFFD27A);
            fa(ctx, cx, top - 2, cx + 1, top + 3, 0xAAFFD27A);
        }
    }

    private void panel(DrawContext ctx, int x, int y, int w, int h, int acc) {
        fa(ctx, x + 3, y + 3, x + w + 3, y + h + 3, 0x55000000);
        box(ctx, x, y, w, h, 0x55FFFFFF, 0xDD0D0F1A, true);
        fa(ctx, x + 2, y, x + w - 2, y + 2, acc);
    }

    // ------------------------------------------------------------------
    // Backgrounds
    // ------------------------------------------------------------------
    private static float hash(int n) {
        int x = n * 374761393 + 668265263;
        x = (x ^ (x >>> 13)) * 1274126177;
        x ^= (x >>> 16);
        return (x & 0xFFFF) / 65535f;
    }

    private static int hsv(double h, double s, double v, int a) {
        h = h - Math.floor(h);
        double c = v * s;
        double x = c * (1 - Math.abs((h * 6) % 2 - 1));
        double m = v - c;
        double r, g, b;
        switch ((int) (h * 6)) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        return ((a & 0xFF) << 24) | ((int) ((r + m) * 255) << 16) | ((int) ((g + m) * 255) << 8) | (int) ((b + m) * 255);
    }

    private void disc(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt(r * r - dy * dy);
            fa(ctx, cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private boolean dHit(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + h && my >= 56 && my < H - 6;
    }

    private int dSection(DrawContext ctx, String label, int y) {
        ctx.drawText(textRenderer, lit(label), 20, y, 0xFF8888AA, false);
        ctx.fill(20 + tw(label) + 8, y + 4, W - 20, y + 5, 0x33FFFFFF);
        return y + 16;
    }

    private static final int[] TEXT_COLS = {0xFFFFFFFF, 0xFFEBDDFF, 0xFFFFDD88, 0xFFB6F5C6, 0xFFB8E1FF, 0xFFFFB8D8, 0xFFDDDDDD, 0xFF1A1A1A};

    /** Draws the whole Look page (scrolls) and reacts to clicks in the same pass. */
    private void designPage(DrawContext ctx, int mx, int my, boolean click, long t, int acc) {
        final int top = 58, bot = H - 6, x0 = 20, cw = W - 40, gap = 8;
        ctx.enableScissor(0, top, W, bot);
        int y = top + 2 - dScroll;

        // LOOK
        y = dSection(ctx, "LOOK", y);
        int cardH = 50, lw = Math.min(140, (cw - gap * 4) / 5);
        for (int i = 0; i < 5; i++) {
            Btn b = new Btn();
            b.kind = 3; b.look = i; b.label = LOOK_NAMES[i]; b.selected = look() == i;
            b.x = x0 + i * (lw + gap); b.y = y; b.w = lw; b.h = cardH;
            boolean hov = dHit(b.x, b.y, b.w, b.h, mx, my);
            drawCard(ctx, b, hov, acc);
            if (click && hov) { setLook(i); }
        }
        y += cardH + 14;

        // THEMES
        y = dSection(ctx, "THEMES", y);
        int x = x0;
        for (int i = 0; i < THEME_NAMES.length; i++) {
            int w = tw(THEME_NAMES[i]) + 16;
            if (x + w > x0 + cw) { x = x0; y += 22; }
            boolean sel = Config.get().theme == i && look() == THEME_VALS[i][2] && bgMode() == THEME_VALS[i][1];
            boolean hov = dHit(x, y, w, 18, mx, my);
            box(ctx, x, y, w, 18, (sel || hov) ? acc : 0xFF3B4056, hov ? 0xFF262A3C : 0xFF1C1F2E, true);
            ctx.drawText(textRenderer, lit(THEME_NAMES[i]), x + 8, y + 5, 0xFFDDDDEE, false);
            if (click && hov) applyTheme(i);
            x += w + 6;
        }
        y += 30;

        // FONT
        y = dSection(ctx, "FONT", y);
        Object fno = Core.call("fontNames");
        String[] fonts = fno instanceof String[] fa ? fa : new String[0];
        int curFont = Core.num("fontIndex");
        int fcols = Math.max(2, Math.min(4, cw / 150));
        int fw = (cw - gap * (fcols - 1)) / fcols, fh = 36;
        for (int i = 0; i < fonts.length; i++) {
            int fx = x0 + (i % fcols) * (fw + gap), fy = y + (i / fcols) * (fh + 6);
            boolean sel = curFont == i, hov = dHit(fx, fy, fw, fh, mx, my);
            box(ctx, fx, fy, fw, fh, sel ? acc : hov ? 0xFF6A7090 : 0xFF3B4056, sel ? 0xFF2A2250 : 0xFF1C1F2E, true);
            Core.call("drawFontSample", ctx, i, "XLetxzX Styles 123", fx + 10, fy + 8, 0xFFFFFFFF);
            ctx.drawText(textRenderer, lit(fonts[i]), fx + 10, fy + 24, 0xFF8888AA, false);
            if (sel) ctx.fill(fx + fw - 12, fy + 6, fx + fw - 6, fy + 12, acc);
            if (click && hov) Core.call("setFont", i);
        }
        y += ((fonts.length + fcols - 1) / fcols) * (fh + 6) + 4;
        String[] flags = {"Bold", "Italic", "Underlined"};
        x = x0;
        for (int k = 0; k < 3; k++) {
            boolean on = Core.call("fontFlag", k) instanceof Boolean bo && bo;
            int w = tw(flags[k]) + 16;
            boolean hov = dHit(x, y, w, 18, mx, my);
            box(ctx, x, y, w, 18, (on || hov) ? acc : 0xFF3B4056, on ? 0xFF2A2250 : 0xFF1C1F2E, true);
            ctx.drawText(textRenderer, lit(flags[k]), x + 8, y + 5, 0xFFDDDDEE, false);
            if (click && hov) Core.call("setFontFlag", k, !on);
            x += w + 6;
        }
        y += 30;

        // TEXT COLOUR
        y = dSection(ctx, "TEXT COLOUR", y);
        int curCol = Core.num("textColor");
        boolean autoHov = dHit(x0, y, 40, 18, mx, my);
        box(ctx, x0, y, 40, 18, (curCol == 0xFFFFFFFF || autoHov) ? acc : 0xFF3B4056, 0xFF1C1F2E, true);
        ctx.drawText(textRenderer, lit("Auto"), x0 + 8, y + 5, 0xFFDDDDEE, false);
        if (click && autoHov) Core.call("setTextColor", 0xFFFFFFFF);
        for (int i = 0; i < TEXT_COLS.length; i++) {
            int cx = x0 + 60 + i * 26, cy = y + 9;
            boolean hov = dHit(cx - 10, cy - 10, 20, 20, mx, my);
            if (curCol == TEXT_COLS[i] && i > 0 || hov) disc(ctx, cx, cy, 10, 0xFFFFFFFF);
            disc(ctx, cx, cy, 8, 0xFF000000 | TEXT_COLS[i]);
            if (click && hov) Core.call("setTextColor", TEXT_COLS[i]);
        }
        y += 34;

        // BACKGROUND
        y = dSection(ctx, "BACKGROUND", y);
        int bcols = Math.max(3, (cw + gap) / (126 + gap));
        int tW = (cw - gap * (bcols - 1)) / bcols, tH = tW * 56 / 100;
        for (int i = 0; i < BG_NAMES.length; i++) {
            int tx = x0 + (i % bcols) * (tW + gap), ty = y + (i / bcols) * (tH + 18);
            boolean sel = bgMode() == i, hov = dHit(tx, ty, tW, tH, mx, my);
            if (ty + tH >= top && ty <= bot) {
                int sw = W, sh = H;
                W = 300; H = 168;
                ctx.enableScissor(tx, Math.max(ty, top), tx + tW, Math.min(ty + tH, bot));
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate((float) tx, (float) ty);
                float sc = tW / 300f;
                ctx.getMatrices().scale(sc, sc);
                drawBackgroundMode(ctx, t, acc, i);
                ctx.getMatrices().popMatrix();
                ctx.disableScissor();
                W = sw; H = sh;
                frame(ctx, tx - 1, ty - 1, tW + 2, tH + 2, sel ? 0xFFFFFFFF : hov ? acc : 0xFF2A2E44);
                if (sel) frame(ctx, tx - 2, ty - 2, tW + 4, tH + 4, 0xFFFFFFFF);
                ctx.drawText(textRenderer, lit(BG_NAMES[i]), tx, ty + tH + 4, sel ? 0xFFFFFFFF : 0xFFAAAACC, false);
            }
            if (click && hov) setBg(i);
        }
        y += ((BG_NAMES.length + bcols - 1) / bcols) * (tH + 18) + 6;

        // WINDOW
        y = dSection(ctx, "WINDOW", y);
        y = dSlider(ctx, "size", "Window size", Config.get().winScale, 60, 100, y, mx, my, click, acc);
        y = dSlider(ctx, "alpha", "Window opacity", Core.num("menuAlpha") * 100 / 255, 20, 100, y, mx, my, click, acc);
        y += 8;

        int content = y + dScroll - top;
        dMax = Math.max(0, content - (bot - top));
        dScroll = Math.max(0, Math.min(dMax, dScroll));
        ctx.disableScissor();
        if (dMax > 0) {
            int trackH = bot - top;
            int barH = Math.max(20, trackH * trackH / (trackH + dMax));
            int barY = top + (int) ((trackH - barH) * (dScroll / (float) dMax));
            ctx.fill(W - 8, top, W - 6, bot, 0x22FFFFFF);
            ctx.fill(W - 8, barY, W - 6, barY + barH, acc);
        }
    }

    /** One slider row with - and + buttons. Returns the next y. */
    private int dSlider(DrawContext ctx, String id, String label, int val, int min, int max, int y, int mx, int my, boolean click, int acc) {
        int x0 = 20, tw0 = W - 40 - 56, h = 18;
        boolean overBar = dHit(x0, y, tw0, h, mx, my);
        if (click && overBar) dragId = id;
        if (!designDown) {
            if (dragId.equals(id)) {
                dragId = "";
                if (id.equals("size")) clearAndInit();
            }
        }
        if (dragId.equals(id)) {
            int v = min + Math.round((mx - x0) / (float) tw0 * (max - min));
            v = Math.max(min, Math.min(max, v));
            if (v != val) { val = v; setWin(id, v); }
        }
        box(ctx, x0, y, tw0, h, 0xFF2A2E44, 0xFF1C1F2E, true);
        int fillW = (int) ((tw0 - 2) * (val - min) / (float) (max - min));
        ctx.fill(x0 + 1, y + 1, x0 + 1 + fillW, y + h - 1, 0xFF6B3FD0);
        String txt = label + ": " + val + "%";
        ctx.drawText(textRenderer, lit(txt), x0 + (tw0 - tw(txt)) / 2, y + 5, 0xFFFFFFFF, false);
        for (int k = 0; k < 2; k++) {
            int bx = x0 + tw0 + 6 + k * 26;
            boolean hov = dHit(bx, y, 22, h, mx, my);
            box(ctx, bx, y, 22, h, hov ? acc : 0xFF3B4056, 0xFF1C1F2E, true);
            ctx.drawText(textRenderer, lit(k == 0 ? "-" : "+"), bx + 9, y + 5, 0xFFFFFFFF, false);
            if (click && hov) {
                int v = Math.max(min, Math.min(max, val + (k == 0 ? -5 : 5)));
                if (v != val) { setWin(id, v); if (id.equals("size")) clearAndInit(); }
            }
        }
        return y + h + 8;
    }

    private void setWin(String id, int v) {
        if (id.equals("size")) { Config.get().winScale = v; Config.save(); }
        else Core.call("setMenuAlpha", Math.max(40, v * 255 / 100));
    }

    private void drawBackground(DrawContext ctx, long t, int acc) { drawBackgroundMode(ctx, t, acc, bgMode()); }

    private void drawBackgroundMode(DrawContext ctx, long t, int acc, int bg) {
        double sec = t / 1000.0;
        if (bg >= 30) {
            drawXmasNight(ctx, sec);
            return;
        }
        if (bg >= 20) {
            int[] pair = STATIC_BG[Math.min(bg - 20, STATIC_BG.length - 1)];
            fg(ctx, 0, 0, W, H, pair[0], pair[1]);
            return;
        }
        switch (bg) {
            case 0 -> drawWinter(ctx, t);
            case 1 -> fg(ctx, 0, 0, W, H, (acc & 0x00FFFFFF) | 0xFF000000, 0xFF06060A);
            case 2 -> fa(ctx, 0, 0, W, H, 0xFF101018);
            case 3 -> {
                fa(ctx, 0, 0, W, H, 0xFF000000);
                int sz = Math.min(W, H);
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, (W - sz) / 2, (H - sz) / 2,
                        0f, 0f, sz, sz, 256, 256, 256, 256);
                fa(ctx, 0, 0, W, H, 0xC0000000);
            }
            case 4 -> drawAurora(ctx, sec);
            case 5 -> drawStarfield(ctx, sec);
            case 6 -> drawMatrix(ctx, sec);
            case 7 -> drawEmbers(ctx, sec);
            case 8 -> drawOcean(ctx, sec);
            case 9 -> drawGrid(ctx, sec, acc);
            case 10 -> drawBubbles(ctx, sec);
            case 11 -> drawFireflies(ctx, sec);
            case 12 -> drawRain(ctx, sec);
            case 13 -> drawPlasma(ctx, sec);
            case 14 -> drawWarp(ctx, sec);
            case 15 -> drawSnowfall(ctx, sec);
            case 16 -> drawDisco(ctx, sec);
            case 17 -> drawRings(ctx, sec, acc);
            case 18 -> drawMeteors(ctx, sec);
            case 19 -> drawSunset(ctx, sec);
            default -> fa(ctx, 0, 0, W, H, 0xFF101018);
        }
    }

    private static final int[] BULBS = {0xFFFF5A5A, 0xFFFFD25A, 0xFF5AB4FF, 0xFF6BE08A};

    private void lightString(DrawContext ctx, double sec, int y0, int amp, int step, int seed) {
        int prevX = 0, prevY = y0;
        for (int x = 0; x <= W; x += step) {
            int yy = y0 + (int) (Math.sin(x * Math.PI / (W / 4.0)) * amp);
            fa(ctx, Math.min(prevX, x), Math.min(prevY, yy), Math.max(prevX, x) + 1, Math.max(prevY, yy) + 1, 0xFF1C2A1C);
            prevX = x; prevY = yy;
        }
        int n = 0;
        for (int x = step / 2; x < W; x += step, n++) {
            int yy = y0 + (int) (Math.sin(x * Math.PI / (W / 4.0)) * amp);
            boolean on = Math.sin(sec * 2.2 + n * 1.7 + seed) > -0.4;
            int c = BULBS[n % 4];
            if (on) {
                fa(ctx, x - 2, yy, x + 3, yy + 6, (c & 0x00FFFFFF) | 0xFF000000);
                fa(ctx, x - 4, yy - 2, x + 5, yy + 8, (c & 0x00FFFFFF) | 0x33000000);
            } else {
                fa(ctx, x - 2, yy, x + 3, yy + 6, 0xFF2A2A2A);
            }
        }
    }

    private void litTree(DrawContext ctx, double sec, int cx, int base, int h, int color, int seed) {
        drawTree(ctx, cx, base, h, color);
        for (int k = 0; k < 7; k++) {
            float f = (k + 1) / 8f;
            int ty = base - (int) (h * (1 - f)) + (int) (hash(seed + k) * 4);
            int half = (int) (h * 0.12f * (1 - f) + 3);
            int lx = cx + (int) ((hash(seed + k * 3) - 0.5f) * 2f * half);
            boolean on = Math.sin(sec * 2 + k + seed) > -0.3;
            int c = BULBS[(k + seed) % 4];
            fa(ctx, lx, ty, lx + 2, ty + 2, on ? ((c & 0x00FFFFFF) | 0xFF000000) : 0xFF222222);
        }
        fa(ctx, cx - 1, base - h - 2, cx + 2, base - h + 1, 0xFFFFD27A);
    }

    private void snowFall(DrawContext ctx, double sec, int count, int color) {
        for (int i = 0; i < count; i++) {
            float spd = 8 + hash(i + 9) * 14;
            int x = (int) (hash(i) * W + Math.sin(sec * 0.8 + i) * 4);
            int y = (int) ((hash(i + 50) * H + sec * spd) % H);
            int sz = hash(i + 3) > 0.8f ? 2 : 1;
            fa(ctx, x, y, x + sz, y + sz, color);
        }
    }

    private void drawXmasNight(DrawContext ctx, double sec) {
        fg(ctx, 0, 0, W, H, 0xFF0B1238, 0xFF1D2C5E);
        for (int i = 0; i < 36; i++) {
            int x = (int) (hash(i + 200) * W), y = (int) (hash(i + 300) * H * 0.7f);
            int a = 0x55 + (int) (0x44 * Math.sin(sec * 1.4 + i));
            fa(ctx, x, y, x + 1, y + 1, (a << 24) | 0xFFFFFF);
        }
        snowFall(ctx, sec, 70, 0xCCFFFFFF);
        int base = H - 6;
        fa(ctx, 0, base - 2, W, H, 0xFF18254E);
        litTree(ctx, sec, (int) (W * 0.2f), base, (int) (H * 0.34f), 0xFF0F3A2A, 11);
        litTree(ctx, sec, (int) (W * 0.62f), base, (int) (H * 0.3f), 0xFF0F3A2A, 23);
        for (int i = 0; i < 4; i++) {
            int x = (int) (W * (0.05f + hash(i + 60) * 0.12f)), y = base - 4 - (int) (hash(i + 70) * 14);
            fa(ctx, x, y, x + 2, y + 2, 0xFFFFC857);
        }
        int gs = Math.max(8, H / 10);
        fa(ctx, W - gs - 8, base - gs, W - 8, base, 0xFF2EBE55);
        fa(ctx, W - gs / 2 - 8 - 1, base - gs, W - gs / 2 - 8 + 1, base, 0xFFFFE08A);
    }




    private void drawAurora(DrawContext ctx, double sec) {
        fg(ctx, 0, 0, W, H, 0xFF040816, 0xFF0A1C2E);
        for (int i = 0; i < 40; i++) {
            int x = (int) (hash(i) * W), y = (int) (hash(i + 77) * H * 0.6f);
            fa(ctx, x, y, x + 1, y + 1, 0x88FFFFFF);
        }
        for (int i = 0; i < 3; i++) {
            for (int x = 0; x < W; x += 4) {
                double wave = Math.sin(x / 60.0 + sec * 0.7 + i * 2.0) * 22 + Math.sin(x / 23.0 - sec * 1.3 + i) * 8;
                int y = (int) (H * (0.18 + 0.13 * i) + wave);
                int len = (int) (H * 0.30 + 16 * Math.sin(x / 40.0 + sec + i));
                double hue = 0.36 + 0.14 * i + 0.05 * Math.sin(sec * 0.5 + x / 200.0);
                fg(ctx, x, y, x + 4, y + len, hsv(hue, 0.75, 1.0, 0x55), hsv(hue, 0.75, 1.0, 0x00));
            }
        }
    }

    private void drawStarfield(DrawContext ctx, double sec) {
        fg(ctx, 0, 0, W, H, 0xFF02030A, 0xFF0A1030);
        for (int i = 0; i < 130; i++) {
            double speed = 4 + hash(i * 3 + 2) * 16;
            int x = (int) (((hash(i * 3) * W - sec * speed) % W + W) % W);
            int y = (int) (hash(i * 3 + 1) * H);
            int sz = hash(i * 5 + 7) > 0.85f ? 2 : 1;
            int a = (int) (120 + 135 * (0.5 + 0.5 * Math.sin(sec * 2 + i)));
            fa(ctx, x, y, x + sz, y + sz, (a << 24) | 0xFFFFFF);
        }
    }

    private void drawMatrix(DrawContext ctx, double sec) {
        fa(ctx, 0, 0, W, H, 0xFF020A04);
        int cols = W / 8;
        for (int c = 0; c < cols; c++) {
            double speed = 40 + hash(c) * 70;
            double head = (sec * speed + hash(c + 991) * (H + 160)) % (H + 160) - 40;
            for (int k = 0; k < 12; k++) {
                int y = (int) (head - k * 8);
                if (y < -8 || y > H) continue;
                int a = (int) (230 * (1 - k / 12.0));
                int col = k == 0 ? 0xCCFFCC : 0x2BFF5A;
                fa(ctx, c * 8, y, c * 8 + 5, y + 6, (a << 24) | col);
            }
        }
    }

    private void drawEmbers(DrawContext ctx, double sec) {
        fg(ctx, 0, 0, W, H, 0xFF1A0505, 0xFF541408);
        for (int i = 0; i < 90; i++) {
            double speed = 12 + hash(i * 3) * 34;
            double p = (sec * speed + hash(i * 3 + 1) * H) % H;
            int y = (int) (H - p);
            int x = (int) (hash(i * 3 + 2) * W + Math.sin(sec * 1.2 + i) * 10);
            int sz = 1 + (int) (hash(i + 500) * 3);
            int a = (int) (220 * (1 - p / H));
            int col = hash(i + 900) > 0.5f ? 0xFF8A1E : 0xFFC84A;
            fa(ctx, x, y, x + sz, y + sz, (a << 24) | col);
        }
    }

    private void drawOcean(DrawContext ctx, double sec) {
        fg(ctx, 0, 0, W, H, 0xFF03203A, 0xFF0A5C8A);
        int[] cols = {0x4430A0D8, 0x5520709E, 0x66103C66};
        for (int l = 0; l < 3; l++) {
            for (int x = 0; x < W; x += 3) {
                int y = (int) (H * (0.55 + 0.12 * l) + Math.sin(x / 38.0 + sec * (0.8 + 0.3 * l) + l * 1.7) * 7
                        + Math.sin(x / 17.0 - sec * 1.1) * 3);
                fa(ctx, x, y, x + 3, H, cols[l]);
            }
        }
    }

    private void drawGrid(DrawContext ctx, double sec, int acc) {
        fg(ctx, 0, 0, W, H, 0xFF0A0420, 0xFF2A0A4A);
        int hz = (int) (H * 0.55);
        int rad = (int) (H * 0.16);
        for (int dy = 0; dy < rad; dy++) {
            int dx = (int) Math.sqrt(rad * rad - dy * dy);
            fa(ctx, W / 2 - dx, hz - dy - 1, W / 2 + dx, hz - dy, hsv(0.95 - 0.12 * dy / (double) rad, 0.8, 1.0, 0xFF));
        }
        fa(ctx, 0, hz, W, H, 0xFF0A0418);
        int gc = (acc & 0xFFFFFF) | 0xAA000000;
        double phase = (sec * 0.35) % 1.0;
        for (int r2 = 0; r2 < 12; r2++) {
            double f = (r2 + phase) / 12.0;
            int y = (int) (hz + (H - hz) * f * f);
            fa(ctx, 0, y, W, y + 1, gc);
        }
        for (int k = -8; k <= 8; k++) {
            for (int y = hz; y < H; y += 6) {
                int x = (int) (W / 2.0 + k * (y - hz) * W / (double) (H - hz) / 9.0);
                fa(ctx, x, y, x + 1, y + 6, gc);
            }
        }
    }

    private void drawBubbles(DrawContext ctx, double sec) {
        fg(ctx, 0, 0, W, H, 0xFF04263B, 0xFF0C6B8F);
        for (int i = 0; i < 40; i++) {
            int rad = 2 + (int) (hash(i) * 6);
            double speed = 14 + hash(i + 50) * 30;
            double p = (sec * speed + hash(i + 100) * H) % (H + 20);
            int y = (int) (H + 10 - p);
            int x = (int) (hash(i + 200) * W + Math.sin(sec + i) * 8);
            disc(ctx, x, y, rad, 0x33FFFFFF);
            fa(ctx, x - rad / 2, y - rad / 2, x - rad / 2 + 1, y - rad / 2 + 1, 0x99FFFFFF);
        }
    }

    private void drawFireflies(DrawContext ctx, double sec) {
        fg(ctx, 0, 0, W, H, 0xFF04100A, 0xFF0C2A18);
        for (int i = 0; i < 45; i++) {
            int x = (int) (hash(i) * W + Math.sin(sec * 0.6 + i) * 20);
            int y = (int) (hash(i + 90) * H + Math.cos(sec * 0.5 + i * 1.7) * 15);
            double pulse = 0.5 + 0.5 * Math.sin(sec * 1.5 + i * 1.9);
            int a = (int) (255 * pulse);
            fa(ctx, x - 2, y - 2, x + 3, y + 3, ((a / 6) << 24) | 0xFFF59E);
            fa(ctx, x - 1, y - 1, x + 2, y + 2, ((a / 3) << 24) | 0xFFF59E);
            fa(ctx, x, y, x + 1, y + 1, (a << 24) | 0xFFFFD0);
        }
    }

    private void drawRain(DrawContext ctx, double sec) {
        fg(ctx, 0, 0, W, H, 0xFF0B0F1A, 0xFF1A2434);
        for (int i = 0; i < 140; i++) {
            double speed = 260 + hash(i) * 220;
            int y = (int) ((sec * speed + hash(i + 300) * H) % (H + 24)) - 12;
            int x = (int) (hash(i + 700) * W);
            fa(ctx, x, y, x + 1, y + 9, 0x66AFC8E8);
        }
        double fl = Math.sin(sec * 1.3) * Math.sin(sec * 7.1) - 0.85;
        if (fl > 0) fa(ctx, 0, 0, W, H, (((int) Math.min(0x50, fl * 600)) << 24) | 0xFFFFFF);
    }

    private void drawPlasma(DrawContext ctx, double sec) {
        for (int x = 0; x < W; x += 16) {
            for (int y = 0; y < H; y += 16) {
                double v = Math.sin(x / 50.0 + sec) + Math.sin(y / 40.0 - sec * 0.8) + Math.sin((x + y) / 70.0 + sec * 0.5);
                fa(ctx, x, y, x + 16, y + 16, hsv(0.72 + v * 0.07, 0.8, 0.28 + 0.1 * (v + 3) / 6.0, 0xFF));
            }
        }
    }

    private void drawWarp(DrawContext ctx, double sec) {
        fa(ctx, 0, 0, W, H, 0xFF02030A);
        double cx = W / 2.0, cy = H / 2.0, maxR = Math.hypot(cx, cy);
        for (int i = 0; i < 160; i++) {
            double ang = hash(i) * Math.PI * 2;
            double p = (sec * 0.45 + hash(i + 400)) % 1.0;
            double rad = p * p * maxR;
            int x = (int) (cx + Math.cos(ang) * rad);
            int y = (int) (cy + Math.sin(ang) * rad);
            int sz = p > 0.7 ? 2 : 1;
            int a = (int) (60 + 195 * p);
            fa(ctx, x, y, x + sz, y + sz, (a << 24) | 0xCFE0FF);
            double rad2 = Math.max(0, p - 0.04);
            rad2 = rad2 * rad2 * maxR;
            int xm = (int) (cx + Math.cos(ang) * (rad + rad2) / 2);
            int ym = (int) (cy + Math.sin(ang) * (rad + rad2) / 2);
            fa(ctx, xm, ym, xm + 1, ym + 1, ((a / 2) << 24) | 0xCFE0FF);
        }
    }

    private void drawSnowfall(DrawContext ctx, double sec) {
        fg(ctx, 0, 0, W, H, 0xFF1B2A44, 0xFF8FA6C8);
        for (int i = 0; i < 150; i++) {
            int sz = 1 + (int) (hash(i) * 3);
            double speed = 15 + sz * 14;
            int y = (int) ((sec * speed + hash(i + 60) * H) % H);
            int x = (int) (hash(i + 120) * W + Math.sin(sec * 0.8 + i) * 10);
            fa(ctx, x, y, x + sz, y + sz, 0xCCFFFFFF);
        }
    }

    private void drawDisco(DrawContext ctx, double sec) {
        fg(ctx, 0, 0, W, H, hsv(sec * 0.05, 0.7, 0.30, 0xFF), hsv(sec * 0.05 + 0.35, 0.7, 0.18, 0xFF));
        for (int i = 0; i < 5; i++) {
            int x = (int) (W / 2.0 + Math.sin(sec * 0.8 + i * 1.3) * W * 0.4);
            double hue = sec * 0.1 + i * 0.2;
            fg(ctx, x - 10, 0, x + 10, H, hsv(hue, 0.6, 1.0, 0x55), hsv(hue, 0.6, 1.0, 0x00));
        }
    }

    private void drawRings(DrawContext ctx, double sec, int acc) {
        fg(ctx, 0, 0, W, H, 0xFF08060F, 0xFF120A24);
        double cx = W / 2.0, cy = H / 2.0, maxR = Math.max(W, H) * 0.65;
        for (int i = 0; i < 5; i++) {
            double p = (sec * 0.25 + i / 5.0) % 1.0;
            double rad = p * maxR;
            int a = (int) (180 * (1 - p));
            for (int k = 0; k < 140; k++) {
                double ang = k * Math.PI * 2 / 140;
                int x = (int) (cx + Math.cos(ang) * rad);
                int y = (int) (cy + Math.sin(ang) * rad);
                fa(ctx, x, y, x + 2, y + 2, (a << 24) | (acc & 0xFFFFFF));
            }
        }
    }

    private void drawMeteors(DrawContext ctx, double sec) {
        fg(ctx, 0, 0, W, H, 0xFF03050E, 0xFF0B1230);
        for (int i = 0; i < 70; i++) {
            int x = (int) (hash(i) * W), y = (int) (hash(i + 140) * H);
            int a = (int) (60 + 120 * (0.5 + 0.5 * Math.sin(sec * 1.7 + i)));
            fa(ctx, x, y, x + 1, y + 1, (a << 24) | 0xFFFFFF);
        }
        for (int i = 0; i < 6; i++) {
            double period = 3.0 + hash(i) * 2.5;
            double p = ((sec + hash(i + 20) * period) % period) / period;
            if (p > 0.5) continue;
            double q = p / 0.5;
            double hx = hash(i + 30) * W * 0.9 + q * W * 0.35;
            double hy = hash(i + 40) * H * 0.4 + q * H * 0.45;
            for (int k = 0; k < 16; k++) {
                int x = (int) (hx - k * 3);
                int y = (int) (hy - k * 2.2);
                int a = (int) (230 * (1 - k / 16.0) * (1 - q * 0.5));
                fa(ctx, x, y, x + 2, y + 2, (a << 24) | 0xD8E8FF);
            }
        }
    }

    private void drawSunset(DrawContext ctx, double sec) {
        fg(ctx, 0, 0, W, H, 0xFF241044, 0xFFFF8A4D);
        int hz = (int) (H * 0.68);
        int rad = Math.max(16, (int) (H * 0.13));
        int sy = hz - (int) (Math.sin(sec * 0.25) * 6) - rad / 3;
        disc(ctx, W / 2, sy, rad, 0xFFFFD27A);
        for (int i = 0; i < 6; i++) {
            double speed = 4 + hash(i) * 8;
            int cx = (int) (((hash(i + 10) * W + sec * speed) % (W + 120)) - 60);
            int cy = (int) (H * (0.12 + 0.4 * hash(i + 20)));
            fa(ctx, cx, cy, cx + 60, cy + 6, 0x55FFC9A0);
            fa(ctx, cx + 10, cy - 5, cx + 44, cy, 0x55FFC9A0);
            fa(ctx, cx + 20, cy + 6, cx + 54, cy + 10, 0x33FFC9A0);
        }
        fa(ctx, 0, hz, W, H, 0xFF1A0A2A);
        for (int k = 0; k < 6; k++) {
            fa(ctx, W / 2 - rad + k * 4, hz + 4 + k * 6, W / 2 + rad - k * 4, hz + 6 + k * 6, 0x66FFB46A);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int acc = accent();
        long t = System.currentTimeMillis();

        rawMx = mouseX;
        rawMy = mouseY;
        boolean mdown = false;
        try { mdown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), 0) == 1; } catch (Throwable ignored) {}
        if (listenKey) {
            try {
                long wh = MinecraftClient.getInstance().getWindow().getHandle();
                for (int k = 32; k <= 348; k++) {
                    if (org.lwjgl.glfw.GLFW.glfwGetKey(wh, k) == 1) {
                        listenKey = false;
                        if (k != 256) XLetxzXVisuals.rebind(k);
                        clearAndInit();
                        break;
                    }
                }
            } catch (Throwable ignored) { listenKey = false; }
        }
        designClick = mdown && !mPrev;
        designDown = mdown;
        mPrev = mdown;
        final float op = Core.num("menuAlpha") / 255f;
        opNow = op;
        final boolean airy = page == EXTRAS || op < 0.97f;
        final int ma = page == EXTRAS ? Math.max(24, Core.num("menuAlpha") * 55 / 100) : Math.max(10, (int) (op * 235));
        ctx.fill(0, 0, this.width, this.height, airy ? (page == EXTRAS ? 0x30000000 : ((int) (0x99 * op) << 24)) : 0x99000000);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) OX, (float) OY);
        mouseX -= OX;
        mouseY -= OY;
        ctx.fill(-3, -3, W + 3, H + 3, airy ? 0x22000000 : 0x55000000);
        if (page == EXTRAS) {
            // see-through, lighter look for the Extras page
            ctx.fillGradient(0, 0, W, H, (ma << 24) | 0x1A2848, (ma << 24) | 0x3A1C5E);
        } else {
            // the chosen background stays, it just gets more see-through with the opacity slider
            bgA = op < 0.97f ? Math.max(0.08f, op) : 1f;
            drawBackground(ctx, t, acc);
            // soft vignette
            fg(ctx, 0, 0, W, 60, 0x66000000, 0x00000000);
            fg(ctx, 0, H - 70, W, H, 0x00000000, 0x77000000);
        }

        // snow (classic backgrounds only)
        if (bgMode() <= 3 && page != EXTRAS) {
            for (int i = 0; i < 70; i++) {
                int x = (int) ((((i * 61) % 100) / 100f) * W + Math.sin(t / 900.0 + i) * 6);
                int y = (int) (((t / (20 + (i % 5) * 8)) + i * 53L) % Math.max(1, H));
                fa(ctx, x, y, x + 2, y + 2, 0xAAFFFFFF);
            }
        }
        bgA = 1f;

        // header: calm bar, thin accent line
        ctx.fill(0, 0, W, 28, ao(0xE60B0B14));
        ctx.fill(0, 28, W, 29, (acc & 0x00FFFFFF) | 0xAA000000);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, 6, 4, 0f, 0f, 20, 20, 256, 256, 256, 256);
        ctx.drawText(textRenderer, lit("XLetxzX Styles"), 32, 6, 0xFFFFFFFF, false);
        ctx.drawText(textRenderer, lit("v" + VERSION + "  ·  1.21.11"), 32, 17, 0xFF7C7F9A, false);

        // bottom bar
        ctx.fill(0, H - 24, W, H, ao(0xE60B0B14));
        ctx.fill(0, H - 24, W, H - 23, 0x1FFFFFFF);
        if (page == HOME) {
            int img = (W - 176) / 3;
            int boxH = H - 90;
            MinecraftClient mc = MinecraftClient.getInstance();
            String who = "XLetxzX";
            if (mc.player != null) {
                ctx.getMatrices().popMatrix();
                float emx = rawMx, emy = rawMy;
                int sc = (int) Math.min(boxH * 0.47f, img * 0.9f);
                // show the skin with empty hands: swap the hand slots only for this draw (no equip sound)
                net.minecraft.entity.player.PlayerInventory inv = mc.player.getInventory();
                int sel = inv.getSelectedSlot();
                net.minecraft.item.ItemStack mainHand = inv.getStack(sel), offHand = inv.getStack(40);
                int arrows = 0, stingers = 0;
                try {
                    inv.setStack(sel, net.minecraft.item.ItemStack.EMPTY);
                    inv.setStack(40, net.minecraft.item.ItemStack.EMPTY);
                    Core.call("setMenuPreview", true);   // plain skin: no fire, name tag, arrows ...
                    arrows = mc.player.getStuckArrowCount();
                    stingers = mc.player.getStingerCount();
                    if (arrows != 0) mc.player.setStuckArrowCount(0);
                    if (stingers != 0) mc.player.setStingerCount(0);
                    InventoryScreen.drawEntity(ctx, OX + 20, OY + 40, OX + 20 + img, OY + 40 + boxH,
                            sc, 0.0625f, emx, emy, mc.player);
                } finally {
                    Core.call("setMenuPreview", false);
                    if (arrows != 0) mc.player.setStuckArrowCount(arrows);
                    if (stingers != 0) mc.player.setStingerCount(stingers);
                    inv.setStack(sel, mainHand);
                    inv.setStack(40, offHand);
                }
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate((float) OX, (float) OY);
                who = mc.getSession().getUsername();
            } else {
                int ls = Math.min(img, boxH);
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, 20 + (img - ls) / 2, 40, 0f, 0f, ls, ls, 256, 256, 256, 256);
            }
            ctx.fill(20, 40 + boxH + 6, 20 + img, 40 + boxH + 8, acc);
            drawCentered(ctx, who, 20 + img / 2, 40 + boxH + 14, 0xFFFFFFFF);

            int regionStart = 20 + img + 20;
            int regionEnd = W - 176 - 10;
            int cx = (regionStart + regionEnd) / 2;
            int gy = H < 300 ? 25 : 28;
            int by = Math.max(80, H / 2 - 10 - (H < 300 ? 28 : 0));
            panel(ctx, cx - 115, by - 62, 230, gy * 2 + 22 + 62 + 32, acc);
            drawCentered(ctx, "XLetxzX Styles", cx, by - 50, acc);
            drawCentered(ctx, "v" + VERSION, cx, by - 38, 0xFF8888AA);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime xmas = LocalDateTime.of(now.getYear(), 12, 25, 0, 0);
            if (now.isAfter(xmas)) xmas = xmas.plusYears(1);
            Duration d = Duration.between(now, xmas);
            drawCentered(ctx, "Christmas in " + d.toDays() + "d " + d.toHoursPart() + "h " + d.toMinutesPart() + "m",
                    cx, by - 24, 0xFFFFD27A);
        } else if (page == DESIGN) {
            ctx.drawText(textRenderer, lit("Look"), 90, 40, 0xFFFFFFFF, false);
            designPage(ctx, mouseX, mouseY, designClick, t, acc);
        } else if (page == SETTINGS) {
            ctx.drawText(textRenderer, lit("Settings"), 20, 40, 0xFFFFFFFF, false);
            drawSection(ctx, "GENERAL", 62, 190);
            drawSection(ctx, "LANGUAGES", 226, 190);
        } else if (page == PRESETS) {
            ctx.drawText(textRenderer, lit("Presets"), 90, 40, 0xFFFFFFFF, false);
        } else if (page == CREDITS) {
            ctx.drawText(textRenderer, lit("Credits"), 20, 44, acc, true);
            String[] lines = {
                "XLetxzX Styles - made by XLetxzX",
                "",
                "Thanks for playing!"
            };
            for (int i = 0; i < lines.length; i++) {
                ctx.drawText(textRenderer, lit(lines[i]), 20, 66 + i * 12, 0xFFCCCCCC, false);
            }
        } else if (page == MODLIST) {
            // MODS
            for (int[] h : headers) {
                String label = CAT_NAMES[h[1]];
                ctx.fill(20, h[0] + 3, 24, h[0] + 11, CAT_COLORS[h[1]]);
                ctx.drawText(textRenderer, lit(label), 30, h[0] + 3, 0xFF9999BB, false);
                ctx.fill(30 + tw(label) + 8, h[0] + 7, W - 20, h[0] + 8, 0x22FFFFFF);
            }
            boolean anyCard = false;
            for (Btn bb : btns) if (bb.kind == 7) { anyCard = true; break; }
            if (!anyCard) {
                drawCentered(ctx, "No mods found", W / 2, H / 2, 0xFF888899);
            }
            if (maxScroll > 0) {
                int trackH = listBottom - listTop;
                int barH = Math.max(20, trackH * trackH / (trackH + maxScroll));
                int barY = listTop + (int) ((trackH - barH) * (scroll / (float) maxScroll));
                ctx.fill(W - 10, listTop, W - 8, listBottom, 0x22FFFFFF);
                ctx.fill(W - 10, barY, W - 8, barY + barH, acc);
            }
        } else if (page == CHANGELOG) {
            ctx.drawText(textRenderer, lit("Changelog"), 90, 40, 0xFFFFFFFF, false);
            String[] log = {
                "v" + VERSION + " - XLetxzX Styles",
                "! Opacity Editor: item opacity in your hand reworked (marker carried in the item tint data); name shown as \"Opacity Editor\" everywhere",
                "! CPvP: Small shield now scales the shield model itself, so the shield in your hand gets smaller too",
                "! Opacity Editor: shield (special-model items) now supports opacity too",
                "+ License: the mod and its code may not be copied, sold or given away (All Rights Reserved)",
                "! Opacity Editor: held-item opacity reworked again - works through the item's own tint data, independent of Sodium",
                "- Pixel editor removed completely",
                "+ TxT > HUD & screen: new Buttons tile (menu button textures)",
                "! CPvP: Small shield now shrinks the shield in your hand directly (first and third person), no resource reload needed",
                "! Opacity Editor: item opacity hook now runs before Sodium's item renderer (third attempt)",
                "+ Opacity Editor: \"No fire on my body\" hides flames on your own player; TxT: new category Blocks (grass, bedrock, stone, blackstone, deepslate ...) and more HUD & screen buttons (air bubbles, jump bar, effects, boss bar, crafting, furnace, anvil, enchanting)",
                "! Opacity Editor: held-item opacity now draws the item itself with reduced alpha (second attempt)",
                "! Spotify HUD: removed the \"Spotify\" text at the bottom",
                "! Opacity Editor: tab renamed; item preview fades with the slider; duplicate options removed from Tweaks (Totem pop size lives in CPvP, pumpkin overlay = Opacity Editor at 0 %)",
                "+ CPvP & Opacity Editor: new clean layout with page header and a description under every option",
                "+ Opacity Editor (Extras): clean rows with item picture; add any item (totem, shield ...) and set its opacity in your hand (new, untested); slider labels no longer sit inside icons",
                "+ CPvP: Small totem (size of the totem in your hand, adjustable); Discord button icon redrawn sharper",
                "! TxT: HUD & screen tiles (hearts, hunger, hotbar, crosshair ...) now show their own pack texture",
                "+ Spotify HUD: new look with cover tile, progress knob, time left and an equalizer",
                "+ Extras > Opacity: sliders to make fire, underwater view, pumpkin overlay and the hotbar see-through",
                "+ Discord button: now with the real icon, centred and sharp",
                "+ Backgrounds keep working when the menu is see-through (they just fade with the opacity slider)",
                "+ Look: background previews fixed; lower Window opacity makes the whole menu (buttons, tiles, panels) see-through",
                "! Lyrics now float in the world around you (not fixed on the screen) and drift; old lyrics box removed",
                "+ Lyrics at crosshair: the current line shows right below where you look and changes with the song",
                "+ Lyrics window: lyrics float in their own window above all apps, drag it anywhere (Extras > Spotify)",
                "! Fixed crash when holding items (third person item size)",
                "+ CPvP: Item sizes - pick items and set the size of each one in your hand",
                "   (also in third person and on other players)",
                "+ Look page: font cards (13 fonts), text colour, backgrounds with",
                "   live previews, a Christmas Night backdrop, window size",
                "   and opacity; the window is wider",
                "+ Settings: Menu key button - press any key to change it",
                "+ Presets (top right): save your setup under a name with a",
                "   color, share codes, import, apply at game start",
                "+ Fix: every pack in the picker now shows its own texture",
                "+ Previews are sharp again (no blur)",
                "+ Crystal previews are drawn as a crystal, not as the raw skin",
                "+ Russian language",
                "+ Home: your skin fills the left side; Keybinds and",
                "   Customize the look buttons removed",
                "+ TxT: pick a pack and stay in the picker; your skin",
                "   holds the item; smooth previews; shield and elytra",
                "   shown as items; each tile shows the pack's texture",
                "+ CPvP: small shield, totem pop size, small hand items,",
                "   no explosions / crystal / totem / break effects",
                "+ Glow Items: soft glow + small cloud for dropped items",
                "+ Colour picker everywhere (square, hue bar, palette)",
                "+ Potion effects HUD is hidden when you have none",
                "+ One resource pack \"XLetxzX Styles\" with the mod icon",
                "+ HUD modules can be moved while chat is open",
                "+ Shield Status, Item Physics, Motion Blur, Saturation",
                "+ Spear PvP category, Keystrokes styles and extra keys",
                "+ Home with skin preview, looks, themes, 30 backgrounds",
                "+ TxT: pick the texture of every PvP item, pixel editor",
                "+ EXTRAS: HUD modules, Spotify HUD + lyrics, Zoom,",
                "   Kill animation, Tweaks, FPS boost, Discord status",
                "+ Import mods by drag and drop"
            };
            for (int i = 0; i < log.length; i++) {
                if (66 + i * 12 > H - 30) break;
                boolean ver = log[i].startsWith("v");
                ctx.drawText(textRenderer, lit(log[i]), 20, 66 + i * 12, ver ? acc : 0xFFCCCCCC, ver);
            }
        }

        for (Btn b : new ArrayList<>(btns)) drawBtn(ctx, b, mouseX, mouseY);
        if (tourStep >= 0 && page == HOME) drawTour(ctx, mouseX, mouseY, designClick, acc);
        if (page == SETTINGS) {
            for (int i = 0; i < LANG_CODES.length; i++) drawFlag(ctx, i, 23 + (i % 3) * 57, 247 + (i / 3) * 22);
        }
        ctx.getMatrices().popMatrix();
        if (page == SETTINGS) {
            Core.call("renderPerf", ctx, OX + 210, OY + 40, W - 230, H - 72, (int) rawMx, (int) rawMy, accent());
        }
        if (page == PRESETS) {
            Core.call("renderPresets", ctx, OX + 12, OY + 60, W - 24, H - 88, (int) rawMx, (int) rawMy, acc);
        }
        if (page == EXTRAS) {
            Core.call("renderExtras", ctx, OX + 12, OY + 36, W - 24, H - 64, (int) rawMx, (int) rawMy, acc);
        } else if (page == IMPORT) {
            Core.call("renderTexturePacks", ctx, OX + 12, OY + 36, W - 24, H - 64, (int) rawMx, (int) rawMy, accent(), texQuery);
        } else if (page == PACKS) {
            Core.call("renderPacks", ctx, OX + 12, OY + 36, W - 24, H - 64, (int) rawMx, (int) rawMy, acc, packQuery);
        }
        if (extrasSearch != null) {
            Object q1 = Core.call("extrasQuery");
            String qs = q1 == null ? "" : q1.toString();
            if (!qs.equals(extrasSearch.getText())) extrasSearch.setText(qs);
            extrasSearch.render(ctx, (int) rawMx, (int) rawMy, delta);
        }
        if (extrasCtx != null) {
            int[] cr = (int[]) Core.call("extrasCtx");
            if (cr == null) {
                extrasCtx.setVisible(false);
                if (extrasCtx.isFocused()) extrasCtx.setFocused(false);
            } else {
                Object idO = Core.call("extrasCtxId"), txO = Core.call("extrasCtxText");
                String id = idO == null ? "" : idO.toString(), tx = txO == null ? "" : txO.toString();
                extrasCtx.setVisible(true);
                extrasCtx.setX(cr[0]);
                extrasCtx.setY(cr[1]);
                extrasCtx.setWidth(cr[2]);
                if (!id.equals(ctxLastId)) {
                    ctxLastId = id;
                    Object hO = Core.call("extrasCtxHint");
                    extrasCtx.setPlaceholder(lit(hO == null ? "" : hO.toString()));
                    extrasCtx.setText(tx);
                } else if (!extrasCtx.isFocused() && !tx.equals(extrasCtx.getText())) {
                    extrasCtx.setText(tx);
                }
                extrasCtx.render(ctx, (int) rawMx, (int) rawMy, delta);
            }
        }
        if (packSearch != null) packSearch.render(ctx, (int) rawMx, (int) rawMy, delta);
        if (searchField != null) searchField.render(ctx, (int) rawMx, (int) rawMy, delta);
        if (linkField != null) linkField.render(ctx, (int) rawMx, (int) rawMy, delta);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((float) OX, (float) OY);

        if (System.currentTimeMillis() < noteUntil) {
            int nw = tw(note) + 16;
            ctx.fill(W / 2 - nw / 2, H - 46, W / 2 + nw / 2, H - 30, 0xEE0B0B14);
            ctx.fill(W / 2 - nw / 2, H - 46, W / 2 - nw / 2 + 2, H - 30, acc);
            drawCentered(ctx, note, W / 2, H - 42, 0xFFFFD27A);
        }
        // thin, quiet window outline
        int edge = (acc & 0x00FFFFFF) | 0x66000000;
        ctx.fill(0, 0, W, 1, edge);
        ctx.fill(0, H - 1, W, H, edge);
        ctx.fill(0, 0, 1, H, edge);
        ctx.fill(W - 1, 0, W, H, edge);
        ctx.getMatrices().popMatrix();
    }

    @Override
    public void close() {
        Core.call("menuClosed");
        if (client != null) client.setScreen(parent);
    }
}
