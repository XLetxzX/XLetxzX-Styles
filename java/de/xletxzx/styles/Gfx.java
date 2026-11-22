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
    private static Method mHas, mLoad, mDraw, mCube, mCrystal;
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
                    case "cube": mCube = m; break;
                    case "crystal": mCrystal = m; break;
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

    static boolean cube(GuiGraphics g, String pack, String top, String side, int cx, int cy, int size) {
        boolean a = ensure(pack, top), b = ensure(pack, side);
        if (!a || !b) return false;
        try { return (Boolean) mCube.invoke(null, g, key(pack, top), key(pack, side), cx, cy, size); } catch (Throwable t) { return false; }
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
