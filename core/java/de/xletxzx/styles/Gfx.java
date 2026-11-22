package de.xletxzx.styles;

import net.minecraft.client.gui.GuiGraphics;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Real pack textures in the GUI. Files are read in the background; the texture itself is
 * created and drawn by de.xletxzx.visuals.Tex (menu part) on the render thread.
 */
final class Gfx {
    private static final Pattern FRAMETIME = Pattern.compile("\"frametime\"\\s*:\\s*(\\d+)");
    private static final ExecutorService IO = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "XLetxzX-Textures");
        t.setDaemon(true);
        return t;
    });
    private static final Set<String> requested = ConcurrentHashMap.newKeySet();
    private static final Map<String, Object[]> ready = new ConcurrentHashMap<>();
    private static Method mHas, mLoad, mDraw, mCube, mCrystal, mScene;
    static final String V_OBSIDIAN = "textures/block/obsidian.png";
    static final String V_CRYSTAL = "textures/entity/end_crystal/end_crystal.png";
    private static boolean broken;

    private Gfx() {}

    private static boolean init() {
        if (mHas != null) return true;
        if (broken) return false;
        try {
            Class<?> c = Class.forName("de.xletxzx.visuals.Tex");
            for (Method m : c.getMethods()) {
                switch (m.getName()) {
                    case "has": mHas = m; break;
                    case "load": mLoad = m; break;
                    case "draw": mDraw = m; break;
                    case "cube": if (m.getParameterCount() == 8) mCube = m; break;   // the version with the top colour
                    case "crystal": mCrystal = m; break;
                    case "crystalScene": mScene = m; break;
                    default: break;
                }
            }
            return mHas != null;
        } catch (Throwable t) {
            broken = true;
            return false;
        }
    }

    /** Key of a file inside a pack. */
    static String key(String pack, String path) { return pack + "|" + path; }

    /** True when the texture can be drawn now; otherwise loading is started. */
    static boolean ensure(String pack, String path) {
        if (!init()) return false;
        String key = key(pack, path);
        try {
            if ((Boolean) mHas.invoke(null, key)) return true;
            Object[] r = ready.remove(key);
            if (r != null) {
                mLoad.invoke(null, key, r[0], r[1]);
                return true;
            }
        } catch (Throwable t) {
            return false;
        }
        if (requested.add(key)) {
            IO.submit(() -> {
                byte[] png = PackFiles.read(pack, path);
                int ft = 1;
                byte[] meta = PackFiles.read(pack, path + ".mcmeta");
                if (meta != null) {
                    Matcher m = FRAMETIME.matcher(new String(meta));
                    if (m.find()) ft = Integer.parseInt(m.group(1));
                }
                ready.put(key, new Object[]{png, ft});
            });
        }
        return false;
    }

    static boolean draw(GuiGraphics g, String pack, String path, int x, int y, int w, int h) {
        if (!ensure(pack, path)) return false;
        try { return (Boolean) mDraw.invoke(null, g, key(pack, path), x, y, w, h); } catch (Throwable t) { return false; }
    }

    /** Block with the vanilla textures (ignores resource packs), drawn like the pack blocks. */
    static boolean cubeVanilla(GuiGraphics g, String top, String side, int cx, int cy, int size, int topTint) {
        boolean a = ensureVanilla(top), b = ensureVanilla(side);
        if (!a || !b || mCube == null) return false;
        try { return (Boolean) mCube.invoke(null, g, key("vanilla", top), key("vanilla", side), cx, cy, size, topTint); } catch (Throwable t) { return false; }
    }

    /** topTint = colour multiplied onto the top face (0xFFFFFFFF = none), e.g. green for grass. */
    static boolean cube(GuiGraphics g, String pack, String top, String side, int cx, int cy, int size, int topTint) {
        boolean a = ensure(pack, top), b = ensure(pack, side);
        if (!a || !b) return false;
        try { return (Boolean) mCube.invoke(null, g, key(pack, top), key(pack, side), cx, cy, size, topTint); } catch (Throwable t) { return false; }
    }

    /** Like ensure(), but reads the file from vanilla Minecraft (ignores resource packs). */
    static boolean ensureVanilla(String path) {
        if (!init()) return false;
        String key = key("vanilla", path);
        try {
            if ((Boolean) mHas.invoke(null, key)) return true;
            Object[] r = ready.remove(key);
            if (r != null) {
                mLoad.invoke(null, key, r[0], r[1]);
                return true;
            }
        } catch (Throwable t) {
            return false;
        }
        if (requested.add(key)) {
            IO.submit(() -> {
                byte[] png = null;
                try {
                    var res = Tweaks.vanilla(net.minecraft.client.Minecraft.getInstance().getResourceManager(), path);
                    if (res != null) try (java.io.InputStream in = res.open()) { png = in.readAllBytes(); }
                } catch (Throwable ignored) {}
                ready.put(key, new Object[]{png, 1});
            });
        }
        return false;
    }

    /**
     * Normal obsidian block with an end crystal floating and turning above it. The crystal uses the pack's
     * entity skin when the pack has one (entPath), otherwise the vanilla crystal.
     */
    static boolean crystalScene(GuiGraphics g, String pack, String entPath, int cx, int cy, int size) {
        if (!init() || mScene == null) return false;
        String ck;
        boolean ok;
        if (pack != null && entPath != null) { ok = ensure(pack, entPath); ck = key(pack, entPath); }
        else { ok = ensureVanilla(V_CRYSTAL); ck = key("vanilla", V_CRYSTAL); }
        boolean ob = ensureVanilla(V_OBSIDIAN);
        if (!ok || !ob) return false;
        try { return (Boolean) mScene.invoke(null, g, ck, key("vanilla", V_OBSIDIAN), cx, cy, size); } catch (Throwable t) { return false; }
    }

    static boolean crystal(GuiGraphics g, String pack, String path, int cx, int cy, int size) {
        if (!ensure(pack, path) || mCrystal == null) return false;
        try { return (Boolean) mCrystal.invoke(null, g, key(pack, path), cx, cy, size); } catch (Throwable t) { return false; }
    }

    /** Forget cached files (after a rescan). Already created textures are reused. */
    static void forget() {
        requested.clear();
        ready.clear();
    }
}
