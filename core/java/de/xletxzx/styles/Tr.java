package de.xletxzx.styles;

import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Translates the mod's English texts into the selected game language (de, es, pl, fr, it). */
public final class Tr {
    private Tr() {}

    private static final String[] CODES = {"de", "es", "pl", "fr", "it", "ru"};
    private static final Map<String, String[]> TABLE = new HashMap<>();
    private static boolean loaded;

    private static void load() {
        loaded = true;
        try (InputStream in = Tr.class.getResourceAsStream("/xletxzx_lang.txt")) {
            if (in == null) return;
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\|", -1);
                if (p.length >= 6) TABLE.put(p[0], new String[]{p[1], p[2], p[3], p[4], p[5], p.length > 6 ? p[6] : ""});
            }
        } catch (Exception e) {
            System.err.println("[XLetxzX] Could not load translations: " + e);
        }
    }

    private static int lang() {
        try {
            String c = Minecraft.getInstance().options.languageCode;
            if (c == null || c.length() < 2) return -1;
            String p = c.substring(0, 2).toLowerCase();
            for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(p)) return i;
        } catch (Throwable ignored) {}
        return -1;
    }

    private static String look(String s, int l) {
        String[] v = TABLE.get(s);
        return v == null || v[l].isEmpty() ? null : v[l];
    }

    /** Public entry for the menu part: translated text, or the input if there is no translation. */
    public static String t(String s) {
        if (s == null || s.length() < 2) return s;
        if (!loaded) load();
        int l = lang();
        if (l < 0) return s;
        String r = look(s, l);
        if (r != null) return r;
        // trailing ": " / ":" labels
        String suf = "";
        String core = s;
        if (core.endsWith(": ")) { suf = ": "; core = core.substring(0, core.length() - 2); }
        else if (core.endsWith(":")) { suf = ":"; core = core.substring(0, core.length() - 1); }
        else if (core.endsWith("...")) { suf = "..."; core = core.substring(0, core.length() - 3); }
        else if (core.endsWith(".")) { suf = "."; core = core.substring(0, core.length() - 1); }
        if (!suf.isEmpty()) {
            r = look(core, l);
            if (r != null) return r + suf;
        }
        // leading number: "5 modules on"
        int i = 0;
        while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '/')) i++;
        if (i > 0 && i < s.length()) {
            r = look(s.substring(i).trim(), l);
            if (r != null) return s.substring(0, i) + " " + r;
        }
        // text followed by a value: "Results for ..." and "Scaling 3"
        for (String key : new String[]{"Results for ", "Added texture pack ", "Imported ", "Could not build the pack: ", "Load failed: ", "Save failed: ", "Import failed: ", "Download failed: ", "Could not open the folder: "}) {
            if (s.startsWith(key)) {
                r = look(key.trim(), l);
                if (r == null) r = look(key, l);
                if (r != null) return r + " " + s.substring(key.length());
            }
        }
        return s;
    }
}
