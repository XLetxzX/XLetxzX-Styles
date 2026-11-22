package de.xletxzx.styles;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Saved setups: all EXTRAS settings under one name (and, if wanted, the picked textures). */
final class Presets {
    static final class Entry {
        String name;
        int color = 0xFF8B3DFF;
        boolean textures;
        int modules;
    }

    private static final Gson GSON = new Gson();
    private static List<Entry> cache;

    private Presets() {}

    static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("xletxzx_presets"); }

    static void refresh() { cache = null; }

    static List<Entry> list() {
        if (cache != null) return cache;
        List<Entry> out = new ArrayList<>();
        try {
            if (Files.isDirectory(dir()))
                try (Stream<Path> s = Files.list(dir())) {
                    List<Path> files = new ArrayList<>();
                    s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().forEach(files::add);
                    for (Path p : files) {
                        try {
                            JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                            Entry e = new Entry();
                            String fn = p.getFileName().toString();
                            e.name = fn.substring(0, fn.length() - 5);
                            if (o.has("color")) e.color = o.get("color").getAsInt();
                            if (o.has("textures")) e.textures = o.get("textures").getAsBoolean();
                            Config c = Config.fromJson(o.get("config").toString());
                            if (c != null && c.hud != null) for (Config.Hud h : c.hud.values()) if (h.on) e.modules++;
                            out.add(e);
                        } catch (Exception ignored) {}
                    }
                }
        } catch (Exception ignored) {}
        cache = out;
        return out;
    }

    private static String safe(String n) {
        String s = n.replaceAll("[\\\\/:*?\"<>|]", "").trim();
        return s.isEmpty() ? "Preset" : (s.length() > 32 ? s.substring(0, 32) : s);
    }

    private static JsonObject wrap(int color, boolean textures) {
        JsonObject o = new JsonObject();
        o.addProperty("v", 1);
        o.addProperty("color", color);
        o.addProperty("textures", textures);
        o.add("config", JsonParser.parseString(Config.toJson(Config.get())));
        if (textures) o.add("mix", GSON.toJsonTree(new LinkedHashMap<>(Config.get().mix)));
        return o;
    }

    /** Saves the current setup; returns the final name or null. */
    static String save(String name, int color, boolean textures) {
        try {
            Files.createDirectories(dir());
            String base = safe(name), n = base;
            int i = 2;
            while (Files.exists(dir().resolve(n + ".json"))) n = base + " " + i++;
            Files.writeString(dir().resolve(n + ".json"), wrap(color, textures).toString());
            refresh();
            return n;
        } catch (Exception e) {
            return null;
        }
    }

    static void delete(String name) {
        try { Files.deleteIfExists(dir().resolve(name + ".json")); } catch (Exception ignored) {}
        Config c = Config.get();
        if (name.equals(c.presetDefault)) c.presetDefault = "";
        if (name.equals(c.presetLast)) c.presetLast = "";
        Config.save();
        refresh();
    }

    static boolean apply(String name) {
        try {
            JsonObject o = JsonParser.parseString(Files.readString(dir().resolve(name + ".json"))).getAsJsonObject();
            Config n = Config.fromJson(o.get("config").toString());
            if (n == null) return false;
            boolean tex = o.has("textures") && o.get("textures").getAsBoolean() && o.has("mix");
            Map<String, String> mix = null;
            if (tex) {
                mix = new LinkedHashMap<>();
                for (Map.Entry<String, com.google.gson.JsonElement> e : o.getAsJsonObject("mix").entrySet()) mix.put(e.getKey(), e.getValue().getAsString());
            }
            Config.restore(n);
            Config c = Config.get();
            if (mix != null) { c.mix.clear(); c.mix.putAll(mix); PacksPanel.syncMix(); }
            c.presetLast = name;
            Config.save();
            try { Tweaks.apply(); } catch (Throwable ignored) {}
            if (c.perf.autoApply) { try { XLetxzXStyles.applyFpsBoost(Minecraft.getInstance()); } catch (Throwable ignored) {} }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static String share(String name) {
        try {
            byte[] raw = Files.readAllBytes(dir().resolve(name + ".json"));
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(bo)) { gz.write(raw); }
            return "XSP1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(bo.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    /** Imports a share code or a preset file text; returns the new name or null. */
    static String importText(String text) {
        try {
            String t = text == null ? "" : text.trim();
            String json;
            if (t.startsWith("XSP1:")) {
                byte[] b = Base64.getUrlDecoder().decode(t.substring(5));
                try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(b))) { json = new String(gz.readAllBytes(), StandardCharsets.UTF_8); }
            } else json = t;
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (!o.has("config") || Config.fromJson(o.get("config").toString()) == null) return null;
            Files.createDirectories(dir());
            String n = "Imported";
            int i = 2;
            while (Files.exists(dir().resolve(n + ".json"))) n = "Imported " + i++;
            Files.writeString(dir().resolve(n + ".json"), o.toString());
            refresh();
            return n;
        } catch (Exception e) {
            return null;
        }
    }

    /** At game start: apply the last or the default preset, as chosen. */
    static void startup() {
        Config c = Config.get();
        String n = c.presetStart == 1 ? c.presetLast : c.presetStart == 2 ? c.presetDefault : "";
        if (n != null && !n.isEmpty() && Files.exists(dir().resolve(n + ".json"))) apply(n);
    }
}
