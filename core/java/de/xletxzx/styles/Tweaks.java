package de.xletxzx.styles;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Tweaks (low fire, low shield, low totem, hide armor ...). They are built from the vanilla
 * files at runtime into a small generated resource pack folder, so no mixins are needed.
 */
final class Tweaks {
    static final String PACK = "XLetxzX Styles";
    static final String[] LEGACY = {"XLetxzX_Styles_Tweaks", "XLetxzX_Mix"};
    static boolean dirty;
    private static String lastSig;

    private static String sig(Config.Tweaks t) {
        return t.lowShield + "," + t.shieldDrop + "," + t.lowTotem + "," + t.totemDrop + "," + t.lowTotemPop + "," + t.popScale
            + "," + t.noPumpkin + "," + t.noVignette + "," + t.smallShield + "," + t.shieldScale;
    }

    /** Remembers the current state as already applied (call once at start). */
    static void markApplied() { lastSig = sig(Config.get().tweaks); }

    static Path dir() { return PackManager.dir().resolve(PACK); }
    static String id() { return "file/" + PACK; }

    static boolean any(Config.Tweaks t) {
        return t.smallShield || t.lowShield || t.lowTotem || t.lowTotemPop || t.noPumpkin || t.noVignette;
    }

    /** Rebuilds the generated pack and reloads resources. */
    static void apply() {
        Minecraft mc = Minecraft.getInstance();
        Config.Tweaks t = Config.get().tweaks;
        boolean on = any(t);
        if (lastSig == null) lastSig = sig(t);
        else if (lastSig.equals(sig(t))) { dirty = false; return; } // nothing that needs a resource reload changed
        lastSig = sig(t);
        rebuild(true);
    }

    /** Rebuilds the single generated pack "XLetxzX Styles" (tweaks + texture mix) and reloads. */
    static void rebuild(boolean reload) {
        Minecraft mc = Minecraft.getInstance();
        Config.Tweaks t = Config.get().tweaks;
        boolean tw = any(t);
        boolean mixOn = !PacksPanel.currentMix().isEmpty();
        try {
            for (String old : LEGACY) wipe(PackManager.dir().resolve(old));
            wipe(dir());
            if (tw || mixOn) {
                Files.createDirectories(dir());
                Files.writeString(dir().resolve("pack.mcmeta"),
                    "{\"pack\":{\"description\":\"XLetxzX Styles - tweaks and textures\",\"min_format\":[75,0],\"max_format\":[75,0]}}");
                try (InputStream in = Tweaks.class.getResourceAsStream("/assets/xletxzx/icon.png")) {
                    if (in != null) Files.write(dir().resolve("pack.png"), in.readAllBytes());
                } catch (Exception ignored) {}
                if (tw) generate(mc.getResourceManager(), t);
                if (mixOn) PacksPanel.copyMix(dir());
            }
        } catch (Throwable e) {
            System.err.println("[XLetxzX] Pack build failed: " + e);
        }
        dirty = false;
        lastSig = sig(t);
        PackManager.setEnabled(id(), tw || mixOn, reload);
    }

    private static void generate(ResourceManager rm, Config.Tweaks t) throws Exception {
        if (t.lowShield) {
            int drop = t.shieldDrop, sc = 0;
            editModel(rm, "models/item/shield.json", false, drop, 0, sc);
            editModel(rm, "models/item/shield_blocking.json", false, drop, 0, sc);
        }
        if (t.lowTotem || t.lowTotemPop) {
            editModel(rm, "models/item/totem_of_undying.json", true,
                t.lowTotem ? t.totemDrop : 0, t.lowTotemPop ? t.popScale : 0, 0);
        }
        if (t.noPumpkin) transparent(rm, "textures/misc/pumpkinblur.png");
        if (t.noVignette) transparent(rm, "textures/misc/vignette.png");
    }

    // ---------------------------------------------------------------- vanilla access
    /** Finds the vanilla version of a file (ignores resource packs on top). */
    static Resource vanilla(ResourceManager rm, String path) {
        int sl = path.lastIndexOf('/');
        String d = sl < 0 ? "" : path.substring(0, sl);
        var map = rm.listResources(d, id -> id.getNamespace().equals("minecraft") && id.getPath().equals(path));
        for (var e : map.entrySet()) {
            Resource first = null;
            for (Resource r : rm.getResourceStack(e.getKey())) {
                if (first == null) first = r;
                if ("vanilla".equals(r.sourcePackId())) return r;
            }
            return first;
        }
        return null;
    }

    static BufferedImage readImage(Resource r) throws Exception {
        try (InputStream in = r.open()) { return ImageIO.read(in); }
    }

    static String readString(Resource r) throws Exception {
        try (InputStream in = r.open()) { return new String(in.readAllBytes(), StandardCharsets.UTF_8); }
    }

    private static Path out(String path) throws Exception {
        Path p = dir().resolve("assets/minecraft/" + path);
        Files.createDirectories(p.getParent());
        return p;
    }

    private static void copyMeta(ResourceManager rm, String path) {
        try {
            Resource m = vanilla(rm, path + ".mcmeta");
            if (m != null) Files.writeString(out(path + ".mcmeta"), readString(m));
        } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------- textures
    private static void shiftFrames(ResourceManager rm, String path, int drop) throws Exception {
        Resource r = vanilla(rm, path);
        if (r == null) return;
        BufferedImage src = readImage(r);
        int w = src.getWidth(), h = src.getHeight(), fh = Math.min(w, h);
        int frames = Math.max(1, h / fh);
        int d = Math.max(0, Math.min(drop, fh - 1));
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int f = 0; f < frames; f++)
            for (int y = 0; y < fh - d; y++)
                for (int x = 0; x < w; x++)
                    dst.setRGB(x, f * fh + y + d, src.getRGB(x, f * fh + y));
        ImageIO.write(dst, "png", out(path).toFile());
        copyMeta(rm, path);
    }

    private static void transparent(ResourceManager rm, String path) throws Exception {
        Resource r = vanilla(rm, path);
        if (r == null) return;
        BufferedImage src = readImage(r);
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(dst, "png", out(path).toFile());
        copyMeta(rm, path);
    }

    private static void transparentAll(ResourceManager rm, String root, String contains) throws Exception {
        var map = rm.listResources(root, id -> id.getNamespace().equals("minecraft")
            && id.getPath().endsWith(".png") && id.getPath().contains(contains));
        for (var e : map.entrySet()) transparent(rm, e.getKey().getPath());
    }

    // ---------------------------------------------------------------- models
    private static JsonArray arr(double a, double b, double c) {
        JsonArray j = new JsonArray();
        j.add(a); j.add(b); j.add(c);
        return j;
    }

    private static JsonObject disp(double[] rot, double[] tr, double[] sc) {
        JsonObject o = new JsonObject();
        o.add("rotation", arr(rot[0], rot[1], rot[2]));
        o.add("translation", arr(tr[0], tr[1], tr[2]));
        o.add("scale", arr(sc[0], sc[1], sc[2]));
        return o;
    }

    /**
     * Lowers the first-person item position (drop) and/or scales the "fixed" display (popPercent,
     * used by the totem pop animation). generated = the model inherits item/generated defaults.
     */
    private static void editModel(ResourceManager rm, String path, boolean generated, int drop, int popPercent, int handScale) throws Exception {
        Resource r = vanilla(rm, path);
        if (r == null) return;
        JsonObject j = JsonParser.parseString(readString(r)).getAsJsonObject();
        JsonObject d = j.has("display") && j.get("display").isJsonObject() ? j.getAsJsonObject("display") : new JsonObject();
        j.add("display", d);

        if (drop != 0) {
            String[] ctx = {"firstperson_righthand", "firstperson_lefthand"};
            for (int i = 0; i < 2; i++) {
                JsonObject o;
                if (d.has(ctx[i])) o = d.getAsJsonObject(ctx[i]);
                else if (generated) o = i == 0
                    ? disp(new double[]{0, -90, 25}, new double[]{1.13, 3.2, 1.13}, new double[]{0.68, 0.68, 0.68})
                    : disp(new double[]{0, 90, -25}, new double[]{1.13, 3.2, 1.13}, new double[]{0.68, 0.68, 0.68});
                else continue; // unknown base transform: do not guess
                JsonArray tr = o.has("translation") ? o.getAsJsonArray("translation") : arr(0, 0, 0);
                JsonElement y = tr.get(1);
                tr.set(1, new JsonPrimitive(y.getAsDouble() - drop));
                o.add("translation", tr);
                d.add(ctx[i], o);
            }
        }
        if (handScale > 0) {
            for (String cx : new String[]{"firstperson_righthand", "firstperson_lefthand", "thirdperson_righthand", "thirdperson_lefthand"}) {
                if (!d.has(cx)) continue;
                JsonObject o = d.getAsJsonObject(cx);
                JsonArray sc = o.has("scale") ? o.getAsJsonArray("scale") : arr(1, 1, 1);
                for (int i = 0; i < 3; i++) sc.set(i, new JsonPrimitive(sc.get(i).getAsDouble() * handScale / 100.0));
                o.add("scale", sc);
            }
        }
        if (popPercent > 0 && generated) {
            double s = 0.5 * popPercent / 100.0;
            d.add("fixed", disp(new double[]{0, 180, 0}, new double[]{0, 0, 0}, new double[]{s, s, s}));
        }
        Files.writeString(out(path), new Gson().toJson(j));
    }

    private static void wipe(Path p) {
        if (!Files.exists(p)) return;
        try (Stream<Path> s = Files.walk(p)) {
            s.sorted(Comparator.reverseOrder()).forEach(x -> { try { Files.delete(x); } catch (Exception ignored) {} });
        } catch (Exception ignored) {}
    }
}
