package de.xletxzx.styles;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fetches synced lyrics for the current song from LRCLIB (free, no account needed). */
final class Lyrics {
    private static volatile String key = "";
    private static volatile int[] times = new int[0];
    private static volatile String[] texts = new String[0];
    private static volatile String status = "";
    private static final Pattern STAMP = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)]");

    /** Call regularly with the current song (or null). Starts a download when the song changes. */
    static void tick(SpotifyService.Info i) {
        if (i == null) {
            if (!key.isEmpty()) { key = ""; times = new int[0]; texts = new String[0]; status = ""; }
            return;
        }
        String k = i.artist + "|" + i.title;
        if (k.equals(key)) return;
        key = k;
        times = new int[0];
        texts = new String[0];
        status = "Loading lyrics...";
        final String artist = i.artist, title = i.title, album = i.album;
        final int dur = (int) (i.durMs / 1000);
        Thread t = new Thread(() -> load(k, artist, title, album, dur), "XLetxzX-Lyrics");
        t.setDaemon(true);
        t.start();
    }

    private static void load(String k, String artist, String title, String album, int dur) {
        String lrc = null;
        try {
            String url = "https://lrclib.net/api/get?artist_name=" + enc(artist) + "&track_name=" + enc(title)
                + (album.isEmpty() ? "" : "&album_name=" + enc(album)) + (dur > 0 ? "&duration=" + dur : "");
            String body = http(url);
            if (body != null) lrc = synced(JsonParser.parseString(body));
            if (lrc == null) {
                body = http("https://lrclib.net/api/search?track_name=" + enc(title) + "&artist_name=" + enc(artist));
                if (body != null) {
                    JsonElement e = JsonParser.parseString(body);
                    if (e.isJsonArray()) {
                        JsonArray a = e.getAsJsonArray();
                        for (JsonElement x : a) {
                            lrc = synced(x);
                            if (lrc != null) break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        if (!k.equals(key)) return; // song changed meanwhile
        if (lrc == null) { status = "No lyrics found"; return; }
        parse(lrc);
        status = times.length == 0 ? "No lyrics found" : "";
    }

    private static String synced(JsonElement e) {
        if (e == null || !e.isJsonObject()) return null;
        JsonObject o = e.getAsJsonObject();
        if (o.has("syncedLyrics") && !o.get("syncedLyrics").isJsonNull()) {
            String s = o.get("syncedLyrics").getAsString();
            if (!s.isBlank()) return s;
        }
        return null;
    }

    private static void parse(String lrc) {
        List<int[]> t = new ArrayList<>();
        List<String> s = new ArrayList<>();
        for (String line : lrc.split("\\R")) {
            Matcher m = STAMP.matcher(line);
            List<Integer> stamps = new ArrayList<>();
            int end = 0;
            while (m.find()) {
                stamps.add((int) ((Integer.parseInt(m.group(1)) * 60 + Double.parseDouble(m.group(2))) * 1000));
                end = m.end();
            }
            if (stamps.isEmpty()) continue;
            String txt = line.substring(end).trim();
            for (int st : stamps) { t.add(new int[]{st}); s.add(txt); }
        }
        Integer[] order = new Integer[t.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Integer.compare(t.get(a)[0], t.get(b)[0]));
        int[] tt = new int[order.length];
        String[] ss = new String[order.length];
        for (int i = 0; i < order.length; i++) { tt[i] = t.get(order[i])[0]; ss[i] = s.get(order[i]); }
        texts = ss;
        times = tt;
    }

    private static final java.net.http.HttpClient CLIENT = java.net.http.HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(6)).build();

    private static String http(String url) {
        try {
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                .timeout(java.time.Duration.ofSeconds(8))
                .header("User-Agent", "XLetxzX-Styles/1.0 (Minecraft mod)").build();
            java.net.http.HttpResponse<String> r = CLIENT.send(req, java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return r.statusCode() == 200 ? r.body() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    // ---------------------------------------------------------------- query
    static String status() { return status; }

    private static int index(double posSec) {
        int[] t = times;
        int ms = (int) (posSec * 1000);
        int idx = -1;
        for (int i = 0; i < t.length; i++) { if (t[i] <= ms) idx = i; else break; }
        return idx;
    }

    static String prev(double pos) { int i = index(pos) - 1; String[] x = texts; return i >= 0 && i < x.length ? x[i] : ""; }
    static String cur(double pos) { int i = index(pos); String[] x = texts; return i >= 0 && i < x.length ? x[i] : ""; }
    static String next(double pos) { int i = index(pos) + 1; String[] x = texts; return i >= 0 && i < x.length ? x[i] : ""; }
}
