package de.xletxzx.styles;

import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** The Presets page: save a setup, share it, apply it. */
final class PresetsPanel {
    static String name = "";
    private static int colorIdx;
    private static boolean keepTex;
    private static String msg = "";
    private static long msgUntil;
    private static int scroll, maxScroll;
    private static boolean opened;

    static final int[] COLORS = {0xFF8B3DFF, 0xFF5B8DEF, 0xFF1FB5A8, 0xFF4CC94C, 0xFFF0A030, 0xFFE04848, 0xFFE8608A, 0xFFE6E6E6};

    private PresetsPanel() {}

    private static void say(String s) { msg = s; msgUntil = System.currentTimeMillis() + 3500; }

    static void scrollBy(double amount) { scroll = (int) Math.max(0, Math.min(maxScroll, scroll - amount * 24)); }

    static void render(int x, int y, int w, int h) {
        if (!opened) { opened = true; Presets.refresh(); }
        ExtrasPanel.ctxOn = false;
        Config c = Config.get();
        int lw = Math.max(210, Math.min(260, w / 3));
        Ui.card(x, y + 4, lw, h - 8, false, false);
        int px = x + 10, pw = lw - 20, py = y + 14;
        Ui.text("Save your setup", px, py, Ui.text());
        py += 20;
        ExtrasPanel.ctxOn = true; ExtrasPanel.ctxId = "preset"; ExtrasPanel.ctxHint = "Name of the preset";
        ExtrasPanel.ctxX = px; ExtrasPanel.ctxY = py; ExtrasPanel.ctxW = pw;
        py += 28;
        for (int i = 0; i < COLORS.length; i++) {
            int cx = px + 6 + i * 17, cy = py + 6;
            boolean hv = Ui.hover(cx - 7, cy - 7, 14, 14);
            if (i == colorIdx) Ui.rrect(cx - 7, cy - 7, 14, 14, 7, 0xFFFFFFFF);
            Ui.rrect(cx - 5, cy - 5, 10, 10, 5, COLORS[i]);
            if (hv && Ui.pressed) { Ui.pressed = false; colorIdx = i; }
        }
        py += 22;
        Ui.text("Also keep textures", px, py + 3, Ui.text());
        if (Ui.toggle(px + pw - 26, py, keepTex)) keepTex = !keepTex;
        py += 22;
        boolean ok = !name.trim().isEmpty();
        if (Ui.button("Save preset", px, py, pw, 18, ok) && ok) {
            String n = Presets.save(name, COLORS[colorIdx], keepTex);
            if (n != null) { say("Saved: " + n); name = ""; } else say("Could not save.");
        }
        py += 28;
        Ui.fill(px, py, pw, 1, Ui.line());
        py += 10;
        if (Ui.button("Paste a share code", px, py, pw, 18, true)) {
            String n = Presets.importText(Minecraft.getInstance().keyboardHandler.getClipboard());
            say(n != null ? "Imported: " + n : "That is not a valid code.");
        }
        py += 24;
        int hw = (pw - 6) / 2;
        if (Ui.button("File", px, py, hw, 18, false)) importFile();
        if (Ui.button("Folder", px + hw + 6, py, hw, 18, false)) OpenUtil.open(Presets.dir());
        py += 28;
        Ui.text("At game start", px, py, Ui.text());
        py += 14;
        String[] modes = {"Nothing", "Last", "Default"};
        int bx = px;
        for (int i = 0; i < 3; i++) {
            int bw = Ui.w(modes[i]) + 16;
            if (Ui.button(modes[i], bx, py, bw, 16, c.presetStart == i)) { c.presetStart = i; Config.save(); }
            bx += bw + 2;
        }
        py += 24;
        if (System.currentTimeMillis() < msgUntil) Ui.text(Ui.fit(msg, pw), px, py, Ui.accent());

        // list
        int rx = x + lw + 12, rw = w - lw - 12;
        List<Presets.Entry> list = Presets.list();
        if (list.isEmpty()) {
            Ui.textC("No presets yet", rx + rw / 2, y + h / 2 - 8, Ui.text());
            return;
        }
        int cols = rw >= 400 ? 2 : 1, gap = 8;
        int cw = (rw - gap * (cols - 1)) / cols, ch = 58;
        int rows = (list.size() + cols - 1) / cols;
        maxScroll = Math.max(0, rows * (ch + gap) - gap - (h - 8));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        Ui.clip(rx - 2, y + 2, rw + 4, h - 4);
        String del = null, app = null, shr = null;
        for (int i = 0; i < list.size(); i++) {
            Presets.Entry e = list.get(i);
            int cx = rx + (i % cols) * (cw + gap), cy = y + 4 + (i / cols) * (ch + gap) - scroll;
            if (cy + ch < y || cy > y + h) continue;
            boolean last = e.name.equals(c.presetLast);
            Ui.card(cx, cy, cw, ch, false, last);
            Ui.fill(cx + 1, cy + 1, 3, ch - 2, e.color);
            Ui.text(Ui.fit(e.name, cw - 20), cx + 12, cy + 8, 0xFFFFFFFF);
            Ui.text(e.modules + " HUD on" + (e.textures ? " + textures" : ""), cx + 12, cy + 21, Ui.dim());
            int by = cy + 35, bx2 = cx + 10;
            if (Ui.button("Apply", bx2, by, 44, 16, true)) app = e.name;
            bx2 += 48;
            boolean def = e.name.equals(c.presetDefault);
            if (Ui.button("Default", bx2, by, 50, 16, def)) { c.presetDefault = def ? "" : e.name; Config.save(); }
            bx2 += 54;
            if (Ui.button("Share", bx2, by, 40, 16, false)) shr = e.name;
            if (Ui.button("X", cx + cw - 26, by, 20, 16, false)) del = e.name;
        }
        Ui.unclip();
        if (app != null) say(Presets.apply(app) ? "Applied: " + app : "Could not apply.");
        if (shr != null) {
            String code = Presets.share(shr);
            if (code != null) { Minecraft.getInstance().keyboardHandler.setClipboard(code); say("Share code copied."); }
        }
        if (del != null) Presets.delete(del);
    }

    private static void importFile() {
        Thread t = new Thread(() -> {
            try {
                java.awt.FileDialog d = new java.awt.FileDialog((java.awt.Frame) null, "Import preset", java.awt.FileDialog.LOAD);
                d.setVisible(true);
                String f = d.getFile();
                if (f == null) return;
                String s = Files.readString(Path.of(d.getDirectory(), f));
                Minecraft.getInstance().execute(() -> {
                    String n = Presets.importText(s);
                    say(n != null ? "Imported: " + n : "That file is not a preset.");
                });
            } catch (Throwable e) {
                Minecraft.getInstance().execute(() -> say("The file window is not available."));
            }
        }, "XLetxzX-PresetFile");
        t.setDaemon(true);
        t.start();
    }
}
