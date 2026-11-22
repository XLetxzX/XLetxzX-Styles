package de.xletxzx.styles;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Reads single files from resource packs (folders or ZIPs). */
final class PackFiles {
    private PackFiles() {}

    /** path is relative to the pack root, e.g. "pack.png" or "assets/minecraft/textures/item/totem_of_undying.png". */
    static byte[] read(String pack, String path) {
        try {
            Path f = PackManager.dir().resolve(pack);
            if (Files.isDirectory(f)) {
                Path p = f.resolve(path);
                return Files.exists(p) ? Files.readAllBytes(p) : null;
            }
            try (ZipFile z = new ZipFile(f.toFile())) {
                ZipEntry e = z.getEntry(path);
                if (e == null) return null;
                try (InputStream in = z.getInputStream(e)) { return in.readAllBytes(); }
            }
        } catch (Exception e) {
            return null;
        }
    }

    /** Plain text description from pack.mcmeta (formatting codes removed). */
    static String description(String pack) {
        byte[] b = read(pack, "pack.mcmeta");
        if (b == null) return "";
        try {
            JsonObject o = JsonParser.parseString(new String(b, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("pack");
            return clean(text(o.get("description")));
        } catch (Exception e) {
            return "";
        }
    }

    private static String text(JsonElement e) {
        if (e == null || e.isJsonNull()) return "";
        if (e.isJsonPrimitive()) return e.getAsString();
        if (e.isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonElement x : (JsonArray) e) sb.append(text(x));
            return sb.toString();
        }
        JsonObject o = e.getAsJsonObject();
        StringBuilder sb = new StringBuilder(o.has("text") ? o.get("text").getAsString() : "");
        if (o.has("extra")) sb.append(text(o.get("extra")));
        return sb.toString();
    }

    static String clean(String s) {
        return s.replaceAll("§.", "").replace('\n', ' ').trim();
    }
}
