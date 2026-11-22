package de.xletxzx.styles;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Reads the currently playing song from the Windows media controls (the same source the Windows
 * volume flyout uses). Works with the Spotify app, Spotify in a browser, YouTube, etc.
 * No login and no API key needed. Windows only.
 */
final class SpotifyService {
    static final class Info {
        String title = "", artist = "", album = "", app = "";
        boolean playing;
        long posMs, durMs, updMs, recv;
    }

    private static volatile Info info;
    private static Process proc;
    private static boolean wanted;

    private static final String SCRIPT = """
        [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false
        Add-Type -AssemblyName System.Runtime.WindowsRuntime
        $asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
        function Await($op, $type) {
            $task = $asTaskGeneric.MakeGenericMethod($type).Invoke($null, @($op))
            $task.Wait(-1) | Out-Null
            $task.Result
        }
        [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime] | Out-Null
        $mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
        while ($true) {
            $line = '{}'
            try {
                $s = $mgr.GetCurrentSession()
                if ($s -ne $null) {
                    $p = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
                    $t = $s.GetTimelineProperties()
                    $i = $s.GetPlaybackInfo()
                    $o = @{ title = [string]$p.Title; artist = [string]$p.Artist; album = [string]$p.AlbumTitle;
                            pos = [long]$t.Position.TotalMilliseconds; dur = [long]$t.EndTime.TotalMilliseconds;
                            upd = [long]$t.LastUpdatedTime.ToUnixTimeMilliseconds();
                            status = [string]$i.PlaybackStatus; app = [string]$s.SourceAppUserModelId }
                    $line = ($o | ConvertTo-Json -Compress)
                }
            } catch { }
            try { [Console]::Out.WriteLine($line); [Console]::Out.Flush() } catch { exit }
            Start-Sleep -Milliseconds 1000
        }
        """;

    static boolean supported() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /** Starts or stops the background reader. Called every tick, cheap when nothing changes. */
    static synchronized void setWanted(boolean w) {
        if (w == wanted) return;
        wanted = w;
        if (w) start(); else stop();
    }

    static Info get() {
        Info i = info;
        if (i == null || i.title.isEmpty()) return null;
        if (System.currentTimeMillis() - i.recv > 6000) return null;
        return i;
    }

    static double positionSec(Info i) {
        long now = System.currentTimeMillis();
        long pos = i.posMs;
        if (i.playing) {
            long base = (i.updMs > 0 && Math.abs(now - i.updMs) < 600_000) ? i.updMs : i.recv;
            pos += Math.max(0, now - base);
        }
        if (i.durMs > 0) pos = Math.min(pos, i.durMs);
        return Math.max(0, pos) / 1000.0;
    }

    private static void start() {
        if (!supported()) return;
        try {
            java.nio.file.Path script = java.nio.file.Files.createTempFile("xletxzx_media", ".ps1");
            script.toFile().deleteOnExit();
            byte[] body = SCRIPT.getBytes(StandardCharsets.UTF_8);
            byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
            byte[] all = new byte[bom.length + body.length];
            System.arraycopy(bom, 0, all, 0, bom.length);
            System.arraycopy(body, 0, all, bom.length, body.length);
            java.nio.file.Files.write(script, all);
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-File", script.toAbsolutePath().toString());
            pb.redirectErrorStream(false);
            proc = pb.start();
            final Process p = proc;
            Thread t = new Thread(() -> read(p), "XLetxzX-Spotify");
            t.setDaemon(true);
            t.start();
            Runtime.getRuntime().addShutdownHook(new Thread(SpotifyService::stop));
        } catch (Exception e) {
            System.err.println("[XLetxzX] Media reader failed: " + e);
        }
    }

    private static synchronized void stop() {
        if (proc != null) { proc.destroyForcibly(); proc = null; }
        info = null;
    }

    private static void read(Process p) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replace("﻿", "").trim();
                if (line.isEmpty()) continue;
                try {
                    JsonObject o = JsonParser.parseString(line).getAsJsonObject();
                    if (!o.has("title")) { info = null; continue; }
                    Info i = new Info();
                    i.title = str(o, "title");
                    i.artist = str(o, "artist");
                    i.album = str(o, "album");
                    i.app = str(o, "app");
                    i.playing = "Playing".equalsIgnoreCase(str(o, "status"));
                    i.posMs = o.has("pos") ? o.get("pos").getAsLong() : 0;
                    i.durMs = o.has("dur") ? o.get("dur").getAsLong() : 0;
                    i.updMs = o.has("upd") ? o.get("upd").getAsLong() : 0;
                    i.recv = System.currentTimeMillis();
                    info = i;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }
}
