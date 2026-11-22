package de.xletxzx.styles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

final class HudRenderer {
    static final String[] KEYS = {
        "fps", "ping", "tps", "coords", "direction", "speed", "cps", "time", "session", "memory", "light", "server",
        "health", "food", "xp", "daycount", "effects", "combo",
        "hand", "durability", "armor", "totem", "crystals", "gapples", "pearls", "pots", "arrows", "inventory",
        "keystrokes",
        "spotify", "lyrics", "glow"
    };
    static final String[] LABELS = {
        "FPS", "Ping", "TPS", "Coordinates", "Direction", "Speed", "CPS", "Clock", "Session Time", "RAM", "Light Level", "Server",
        "Health", "Hunger", "XP Level", "World Day", "Potion Effects", "Combo",
        "Held Item", "Durability", "Armor HUD", "Totem Count", "Crystal Count", "Golden Apple Count", "Pearl Count", "Potion Count", "Arrow Count", "Inventory HUD",
        "Keystrokes",
        "Spotify HUD", "Spotify Lyrics", "Glow Items"
    };
    static final String[] CAT_NAMES = {"Info", "Player", "Items", "Input", "Media"};
    /** Category of each module (same order as KEYS). */
    static final int[] CAT = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 1, 1, 1, 1,
        2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
        3,
        4, 4,
        2
    };
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    static int indexOf(String key) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return i;
        return 0;
    }

    static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.screen instanceof HudEditorScreen) return;
        for (String k : KEYS) {
            if (k.equals("glow")) continue;
            Config.Hud h = Config.get().hud.get(k);
            if (k.equals("lyrics")) continue;   // lyrics are drawn at the crosshair (centerLyrics), there is no box any more
            if (h.on) draw(g, mc.font, k, h.x, h.y);
        }
        Config.Hud ly = Config.get().hud.get("lyrics");
        if (ly.on) centerLyrics(g, mc, ly);
    }

    private static String lastLine = "";
    private static long lastSpawn;
    private static final java.util.ArrayList<double[]> LY = new java.util.ArrayList<>();   // x,y,z, vx,vy,vz, born(ms)
    private static final java.util.ArrayList<String> LY_TXT = new java.util.ArrayList<>();
    private static final java.util.Random LY_RND = new java.util.Random();

    /** Lyrics float in the world: every new line appears somewhere around you, stays where it is and drifts slowly. */
    private static void centerLyrics(GuiGraphics g, Minecraft mc, Config.Hud h) {
        SpotifyService.Info s = SpotifyService.get();
        if (mc.player == null || s == null || !Lyrics.status().isEmpty()) { LY.clear(); LY_TXT.clear(); lastLine = ""; return; }
        long now = System.currentTimeMillis();
        String line = Lyrics.cur(SpotifyService.positionSec(s));
        if (!line.equals(lastLine) && now - lastSpawn > 2600) {
            lastLine = line;
            if (!line.isEmpty()) {
                lastSpawn = now;
                // exactly where you are looking right now; it stays there and does not follow you
                double yaw = Math.toRadians(mc.player.getYRot());
                double pit = Math.toRadians(mc.player.getXRot());
                double dist = 5.0;
                double ex = mc.player.getX(), ey = mc.player.getEyeY(), ez = mc.player.getZ();
                double dx = -Math.sin(yaw) * Math.cos(pit), dy = -Math.sin(pit), dz = Math.cos(yaw) * Math.cos(pit);
                int living = LY.size();
                LY.add(new double[]{ex + dx * dist, ey + dy * dist - 0.4 * living, ez + dz * dist, 0, 0, 0, now, yaw});
                LY_TXT.add(line);
                while (LY.size() > 3) { LY.remove(0); LY_TXT.remove(0); }
            }
        }
        double yawR = Math.toRadians(mc.player.getYRot()), pitR = Math.toRadians(mc.player.getXRot());
        double sy = Math.sin(yawR), cy = Math.cos(yawR), sp = Math.sin(pitR), cp = Math.cos(pitR);
        double fx = -sy * cp, fy = -sp, fz = cy * cp;
        double rx = -cy, rz = -sy;
        double ux = -sy * sp, uy = cp, uz = cy * sp;
        double ex = mc.player.getX(), ey = mc.player.getEyeY(), ez = mc.player.getZ();
        int gw = g.guiWidth(), gh = g.guiHeight();
        double F = (gh / 2.0) / Math.tan(Math.toRadians(Math.max(30, Math.min(110, mc.options.fov().get()))) / 2.0);
        net.minecraft.client.gui.Font f = mc.font;
        for (int i = LY.size() - 1; i >= 0; i--) {
            double[] e = LY.get(i);
            double age = (now - (long) e[6]) / 1000.0;
            if (age > 3.6) { LY.remove(i); LY_TXT.remove(i); continue; }
            // the line is a flat, upright sign in the world; it always faces the spot where it appeared and never turns towards you
            double wy = e[7];
            double wrx = -Math.cos(wy), wrz = -Math.sin(wy);
            double st = 0.04;   // blocks per text pixel
            double[] p0 = project(e[0], e[1], e[2], ex, ey, ez, fx, fy, fz, rx, rz, ux, uy, uz, F, gw, gh);
            double[] pa = project(e[0] + wrx * st, e[1], e[2] + wrz * st, ex, ey, ez, fx, fy, fz, rx, rz, ux, uy, uz, F, gw, gh);
            double[] pb = project(e[0], e[1] - st, e[2], ex, ey, ez, fx, fy, fz, rx, rz, ux, uy, uz, F, gw, gh);
            if (p0 == null || pa == null || pb == null) continue;
            double ax = pa[0] - p0[0], ay = pa[1] - p0[1], bx = pb[0] - p0[0], by = pb[1] - p0[1];
            double det = ax * by - ay * bx;
            double la = Math.hypot(ax, ay), lb = Math.hypot(bx, by);
            if (la < 1e-6 || lb < 1e-6) continue;
            double facing = det / (la * lb);           // 1 = looking straight at it, 0 = edge on, <0 = from behind
            if (facing <= 0.03) continue;
            float fade = (float) Math.min(Math.min(1.0, age / 0.4), Math.min(1.0, (3.6 - age) / 0.9));
            fade *= (float) Math.min(1.0, facing / 0.3);
            int a = (int) (255 * fade * (i == LY.size() - 1 ? 1f : 0.7f));
            if (a < 8) continue;
            String txt = LY_TXT.get(i);
            g.pose().pushMatrix();
            g.pose().translate((float) p0[0], (float) p0[1]);
            g.pose().mul(new org.joml.Matrix3x2f((float) ax, (float) ay, (float) bx, (float) by, 0f, 0f));
            g.drawString(f, txt, -f.width(txt) / 2, -4, Ui.alpha(h.color, a), true);
            g.pose().popMatrix();
        }
    }

    /** World point to GUI pixels; null when it is behind you. */
    private static double[] project(double x, double y, double z, double ex, double ey, double ez,
                                    double fx, double fy, double fz, double rx, double rz,
                                    double ux, double uy, double uz, double F, int gw, int gh) {
        double dx = x - ex, dy = y - ey, dz = z - ez;
        double depth = dx * fx + dy * fy + dz * fz;
        if (depth < 0.3) return null;
        return new double[]{gw / 2.0 + (dx * rx + dz * rz) / depth * F, gh / 2.0 - (dx * ux + dy * uy + dz * uz) / depth * F};
    }

    // ------------------------------------------------------------ data
    private static Item countItem(String key) {
        switch (key) {
            case "totem": return Items.TOTEM_OF_UNDYING;
            case "crystals": return Items.END_CRYSTAL;
            case "gapples": return Items.GOLDEN_APPLE;
            case "pearls": return Items.ENDER_PEARL;
            case "pots": return Items.SPLASH_POTION;
            case "arrows": return Items.ARROW;
            default: return null;
        }
    }

    static int count(Item item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        int n = 0;
        int size = mc.player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    private static String lab(Config.Hud h, String label) { return h.label ? Tr.t(label) : ""; }

    private static String line(String key, Config.Hud h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return "-";
        switch (key) {
            case "fps": return lab(h, "FPS: ") + mc.getFps();
            case "ping": return lab(h, "Ping: ") + XLetxzXStyles.ping() + " ms";
            case "tps": return lab(h, "TPS: ") + String.format("%.1f", Tps.value);
            case "coords": {
                StringBuilder sb = new StringBuilder();
                if (h.cx) sb.append((int) Math.floor(mc.player.getX())).append(' ');
                if (h.cy) sb.append((int) Math.floor(mc.player.getY())).append(' ');
                if (h.cz) sb.append((int) Math.floor(mc.player.getZ())).append(' ');
                if (sb.length() == 0) return "-";
                String name = (h.cx ? "X" : "") + (h.cy ? "Y" : "") + (h.cz ? "Z" : "");
                return lab(h, name + ": ") + sb.toString().trim();
            }
            case "direction": {
                float yaw = ((mc.player.getYRot() % 360f) + 360f) % 360f;
                return DIRS[(int) ((yaw + 22.5f) / 45f) % 8] + " (" + (int) yaw + ")";
            }
            case "speed": return lab(h, "Speed: ") + String.format("%.1f b/s", XLetxzXStyles.speed);
            case "cps": return lab(h, "CPS ") + XLetxzXStyles.cpsL() + " | " + XLetxzXStyles.cpsR();
            case "time": return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            case "session": {
                String[] l = sessionLines();
                return l[0] + " " + l[1];
            }
            case "memory": {
                Runtime r = Runtime.getRuntime();
                return lab(h, "RAM: ") + ((r.totalMemory() - r.freeMemory()) >> 20) + " / " + (r.maxMemory() >> 20) + " MB";
            }
            case "light": {
                int l = mc.level == null ? 0 : mc.level.getMaxLocalRawBrightness(mc.player.blockPosition());
                return lab(h, "Light: ") + l;
            }
            case "server": return mc.getCurrentServer() == null ? "Singleplayer" : mc.getCurrentServer().ip;
            case "health": return lab(h, "HP: ") + String.format("%.1f", mc.player.getHealth());
            case "food": return lab(h, "Food: ") + mc.player.getFoodData().getFoodLevel();
            case "xp": return lab(h, "XP: ") + mc.player.experienceLevel;
            case "daycount": return mc.level == null ? "Day: -" : lab(h, "Day: ") + (mc.level.getDayTime() / 24000L);
            case "combo": return lab(h, "Combo: ") + XLetxzXStyles.combo;
            case "hand": {
                ItemStack s = mc.player.getMainHandItem();
                return s.isEmpty() ? lab(h, "Hand: ") + "-" : s.getHoverName().getString() + (s.getCount() > 1 ? " x" + s.getCount() : "");
            }
            case "durability": {
                ItemStack s = mc.player.getMainHandItem();
                return s.isDamageableItem() ? lab(h, "Durability: ") + (s.getMaxDamage() - s.getDamageValue()) + "/" + s.getMaxDamage() : lab(h, "Durability: ") + "-";
            }
            default: return "";
        }
    }

    /** Rows: name, time, icon sprite, beneficial, running out. */
    private static List<Object[]> effectRows() {
        List<Object[]> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return out;
        for (MobEffectInstance e : mc.player.getActiveEffects()) {
            String name = e.getEffect().value().getDisplayName().getString();
            if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
            boolean inf = e.isInfiniteDuration();
            String time = inf ? "inf" : String.format("%d:%02d", e.getDuration() / 1200, (e.getDuration() / 20) % 60);
            net.minecraft.resources.Identifier sprite = null;
            try {
                net.minecraft.resources.Identifier id = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value());
                if (id != null) sprite = net.minecraft.resources.Identifier.fromNamespaceAndPath(id.getNamespace(), "mob_effect/" + id.getPath());
            } catch (Throwable t) { }
            out.add(new Object[]{name, time, sprite, e.getEffect().value().isBeneficial(), !inf && e.getDuration() <= 200});
        }
        return out;
    }

    private static List<String> effectLines() {
        List<String> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return out;
        for (MobEffectInstance e : mc.player.getActiveEffects()) {
            String name = e.getEffect().value().getDisplayName().getString();
            if (e.getAmplifier() > 0) name += " " + (e.getAmplifier() + 1);
            String time = e.isInfiniteDuration() ? "inf" : String.format("%d:%02d", e.getDuration() / 1200, (e.getDuration() / 20) % 60);
            out.add(name + "  " + time);
        }
        return out;
    }

    /** {label, value} pairs flattened: Time, Kills, Deaths, K/D, Pops. */
    private static String[][] sessionRows() {
        long s = (System.currentTimeMillis() - XLetxzXStyles.START) / 1000;
        String time = s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60) : String.format("%d:%02d", s / 60, s % 60);
        return new String[][]{
            {"Time", time}, {"Kills", String.valueOf(Stats.kills)}, {"Deaths", String.valueOf(Stats.deaths)},
            {"K/D", Stats.kd()}, {"Pops", String.valueOf(Stats.pops)}};
    }

    private static String[] sessionLines() { String[] r = sessionRows()[0]; return r; }

    private static int durColor(ItemStack st) {
        int max = st.getMaxDamage();
        float f = max <= 0 ? 1f : Math.max(0f, (max - st.getDamageValue()) / (float) max);
        int r = f > 0.5f ? (int) (255 * (1 - f) * 2) : 255;
        int gr = f > 0.5f ? 255 : (int) (255 * f * 2);
        return 0xFF000000 | (Math.min(255, r) << 16) | (Math.min(255, gr) << 8) | 0x40;
    }

    private static String fit(Font f, String s, int maxW) {
        if (f.width(s) <= maxW) return s;
        while (s.length() > 1 && f.width(s + "...") > maxW) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    private static String clock(double sec) {
        int s = (int) sec;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    // ------------------------------------------------------------ size (unscaled)
    static int[] size(String key) {
        Font f = Minecraft.getInstance().font;
        Config.Hud h = Config.get().hud.get(key);
        switch (key) {
            case "keystrokes": { int[] d = keyLayout(null); return new int[]{d[0], d[1]}; }
            case "armor": return new int[]{58, 4 * 18 + 4};
            case "session": {
                int w = 0;
                for (String[] r : sessionRows()) w = Math.max(w, f.width(Tr.t(r[0]) + " " + r[1]));
                return new int[]{w + 12, 5 * (f.lineHeight + 3) + 4};
            }
            case "inventory": return new int[]{9 * 18 + 4, 3 * 18 + 4};
            case "spotify": return new int[]{200, 56};
            case "lyrics": return new int[]{230, 38};
            case "effects": {
                List<Object[]> l = effectRows();
                int w = 90;
                for (Object[] r : l) w = Math.max(w, 30 + f.width((String) r[0]) + 8 + f.width((String) r[1]));
                return new int[]{w, Math.max(1, l.size()) * 21 + 3};
            }
            default:
                if (countItem(key) != null) return new int[]{f.width("x" + 999) + 30, 24};
                return new int[]{f.width(line(key, h)) + 10, f.lineHeight + 7};
        }
    }

    /** Size including scale. */
    static int[] sizeS(String key) {
        int[] s = size(key);
        float sc = Config.get().hud.get(key).scale;
        return new int[]{Math.round(s[0] * sc), Math.round(s[1] * sc)};
    }

    // ------------------------------------------------------------ drawing
    static void draw(GuiGraphics g, Font f, String key, int x, int y) {
        Config.Hud h = Config.get().hud.get(key);
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        g.pose().scale(h.scale, h.scale);
        drawBase(g, f, key, h);
        g.pose().popMatrix();
    }

    private static void drawBase(GuiGraphics g, Font f, String key, Config.Hud h) {
        Minecraft mc = Minecraft.getInstance();
        int[] sz = size(key);
        Item ci = countItem(key);
        int col = h.chroma ? Ui.rainbow(0) : h.color;
        if (key.equals("keystrokes")) {
            java.util.List<Object[]> out = new java.util.ArrayList<>();
            keyLayout(out);
            Config.Keys k = Config.get().keys;
            for (Object[] e : out) {
                drawKey(g, f, (String) e[0], (int) e[1], (int) e[2], (int) e[3], (int) e[4], isDown((String) e[5]), h, k, col);
            }
        } else if (key.equals("armor")) {
            box(g, sz[0], sz[1], h);
            if (mc.player != null) {
                for (int i = 0; i < 4; i++) {
                    ItemStack st = mc.player.getItemBySlot(ARMOR[i]);
                    int ry = 2 + i * 18;
                    if (st.isEmpty()) continue;
                    g.renderItem(st, 2, ry);
                    if (st.isDamageableItem()) {
                        String v = String.valueOf(st.getMaxDamage() - st.getDamageValue());
                        g.drawString(f, v, 22, ry + 5, h.chroma ? Ui.rainbow(i * 30) : durColor(st), h.shadow);
                    }
                }
            }
        } else if (key.equals("session")) {
            box(g, sz[0], sz[1], h);
            int ry = 4;
            for (String[] r : sessionRows()) {
                if (h.label) {
                    g.drawString(f, Tr.t(r[0]), 6, ry, col, h.shadow);
                    g.drawString(f, r[1], 6 + f.width(Tr.t(r[0])) + 4, ry, 0xFFFFFFFF, h.shadow);
                } else {
                    g.drawString(f, r[1], 6, ry, 0xFFFFFFFF, h.shadow);
                }
                ry += f.lineHeight + 3;
            }
        } else if (key.equals("inventory")) {
            box(g, sz[0], sz[1], h);
            if (mc.player != null) {
                for (int i = 0; i < 27; i++) {
                    ItemStack st = mc.player.getInventory().getItem(9 + i);
                    int ix = 2 + (i % 9) * 18, iy = 2 + (i / 9) * 18;
                    g.renderItem(st, ix, iy);
                    g.renderItemDecorations(f, st, ix, iy);
                }
            }
        } else if (key.equals("spotify")) {
            box(g, sz[0], sz[1], h);
            SpotifyService.Info s = SpotifyService.get();
            if (s == null) {
                String msg = SpotifyService.supported() ? "Nothing playing" : "Windows only";
                g.drawString(f, msg, 5, 5, col, h.shadow);
            } else {
                int spAc = h.borderColor | 0xFF000000;
                int spW = sz[0], spTx = 56;
                // cover tile with a note
                g.fillGradient(7, 7, 49, 49, Ui.alpha(spAc, 255), Ui.alpha(Ui.mix(spAc, 0xFF000000, 0.45f), 255));
                g.fill(25, 18, 27, 36, 0xFFFFFFFF);
                g.fill(21, 33, 27, 37, 0xFFFFFFFF);
                g.fill(27, 18, 33, 21, 0xFFFFFFFF);
                g.fill(31, 21, 33, 26, 0xFFFFFFFF);
                // title + artist
                g.drawString(f, fit(f, s.title, spW - spTx - 30), spTx, 8, col, h.shadow);
                g.drawString(f, fit(f, s.artist, spW - spTx - 30), spTx, 20, Ui.alpha(col, 160), h.shadow);
                // little equalizer while playing
                long spNow = System.currentTimeMillis();
                for (int spB = 0; spB < 4; spB++) {
                    int spBh = s.playing ? 3 + (int) (Math.abs(Math.sin(spNow / 230.0 + spB * 1.7)) * 7) : 3;
                    g.fill(spW - 24 + spB * 4, 17 - spBh, spW - 22 + spB * 4, 17, spAc);
                }
                // progress bar with a knob
                double spPos = SpotifyService.positionSec(s);
                double spDur = s.durMs / 1000.0;
                int spBx = spTx, spBw = spW - spTx - 8, spBy = 35;
                float spFr = spDur > 0 ? (float) Math.min(1.0, spPos / spDur) : 0f;
                int spFw = (int) (spBw * spFr);
                g.fill(spBx, spBy, spBx + spBw, spBy + 3, Ui.alpha(col, 50));
                g.fill(spBx, spBy, spBx + spFw, spBy + 3, spAc);
                g.fill(spBx + spFw - 2, spBy - 2, spBx + spFw + 2, spBy + 5, 0xFFFFFFFF);
                // times
                String spL = clock(spPos), spR = spDur > 0 ? "-" + clock(Math.max(0, spDur - spPos)) : "";
                int spTy = 44;
                g.drawString(f, spL, spBx, spTy, Ui.alpha(col, 170), h.shadow);
                g.drawString(f, spR, spBx + spBw - f.width(spR), spTy, Ui.alpha(col, 170), h.shadow);
            }
        } else if (key.equals("lyrics")) {
            box(g, sz[0], sz[1], h);
            SpotifyService.Info s = SpotifyService.get();
            String st = Lyrics.status();
            if (s == null) {
                g.drawString(f, Tr.t("No lyrics"), 5, 5, col, h.shadow);
            } else if (!st.isEmpty()) {
                g.drawString(f, st, 5, 5, col, h.shadow);
            } else {
                double pos = SpotifyService.positionSec(s);
                String a = fit(f, Lyrics.prev(pos), sz[0] - 10), b = fit(f, Lyrics.cur(pos), sz[0] - 10), c = fit(f, Lyrics.next(pos), sz[0] - 10);
                g.drawString(f, a, (sz[0] - f.width(a)) / 2, 4, Ui.alpha(col, 120), h.shadow);
                g.drawString(f, b, (sz[0] - f.width(b)) / 2, 16, col, h.shadow);
                g.drawString(f, c, (sz[0] - f.width(c)) / 2, 28, Ui.alpha(col, 120), h.shadow);
            }
        } else if (key.equals("effects")) {
            List<Object[]> l = effectRows();
            if (l.isEmpty()) {
                // nothing at all while there are no effects; only a placeholder in the HUD editor so it can be moved
                if (Minecraft.getInstance().screen instanceof HudEditorScreen) {
                    box(g, sz[0], sz[1], h);
                    g.drawString(f, Tr.t("Potion Effects"), 5, 4, Ui.alpha(col, 150), h.shadow);
                }
                return;
            }
            box(g, sz[0], sz[1], h);
            for (int i = 0; i < l.size(); i++) {
                Object[] r = l.get(i);
                int ry = 3 + i * 21;
                if (r[2] != null) {
                    g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, (net.minecraft.resources.Identifier) r[2], 4, ry, 18, 18);
                }
                int ty = ry + (18 - 7) / 2;
                String nm = (String) r[0], tm = (String) r[1];
                int nc = h.chroma ? Ui.rainbow(i * 30) : ((boolean) r[3] ? 0xFFC9A0FF : 0xFFFF8A8A);
                g.drawString(f, nm, 26, ty, nc, true);
                g.drawString(f, tm, 26 + f.width(nm) + 8, ty, (boolean) r[4] ? 0xFFFF3B3B : 0xFFFFFFFF, true);
            }
        } else if (ci != null) {
            box(g, sz[0], sz[1], h);
            g.renderItem(new ItemStack(ci), 4, 4);
            g.drawString(f, "x" + count(ci), 24, 8, col, h.shadow);
        } else {
            box(g, sz[0], sz[1], h);
            g.drawString(f, line(key, h), 5, 4, col, h.shadow);
        }
    }

    private static void box(GuiGraphics g, int w, int hh, Config.Hud h) {
        if (h.bg) g.fill(0, 0, w, hh, Ui.alpha(h.bgColor, h.bgAlpha));
        if (h.border) outline(g, 0, 0, w, hh, h.borderColor);
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int hh, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + hh - 1, x + w, y + hh, c);
        g.fill(x, y, x + 1, y + hh, c);
        g.fill(x + w - 1, y, x + w, y + hh, c);
    }

    static final String[] EXTRA_KEYS = {"Shift", "Ctrl", "Alt", "Tab", "Q", "E", "F", "R", "G", "C", "V", "X", "Z", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

    private static int glfw(String id) {
        switch (id) {
            case "Shift": return 340;
            case "Ctrl": return 341;
            case "Alt": return 342;
            case "Tab": return 258;
            default: return id.length() == 1 ? id.charAt(0) : -1;
        }
    }

    private static boolean isDown(String id) {
        Minecraft mc = Minecraft.getInstance();
        switch (id) {
            case "W": return mc.options.keyUp.isDown();
            case "A": return mc.options.keyLeft.isDown();
            case "S": return mc.options.keyDown.isDown();
            case "D": return mc.options.keyRight.isDown();
            case "LMB": return mc.options.keyAttack.isDown();
            case "RMB": return mc.options.keyUse.isDown();
            case "SPACE": return mc.options.keyJump.isDown();
            default: {
                int code = glfw(id);
                long w = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
                return code > 0 && w != 0 && org.lwjgl.glfw.GLFW.glfwGetKey(w, code) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            }
        }
    }

    /** Builds the key layout. Entries: label, x, y, w, h, id. Returns {width, height}. */
    static int[] keyLayout(java.util.List<Object[]> out) {
        Config.Keys k = Config.get().keys;
        final int K = 19, G = 2, W3 = 3 * K + 2 * G;
        int y = 0;
        if (out != null) {
            out.add(new Object[]{"W", K + G, 0, K, K, "W"});
            out.add(new Object[]{"A", 0, K + G, K, K, "A"});
            out.add(new Object[]{"S", K + G, K + G, K, K, "S"});
            out.add(new Object[]{"D", 2 * (K + G), K + G, K, K, "D"});
        }
        y = 2 * K + G;
        if (k.mouse) {
            y += G;
            if (out != null) {
                out.add(new Object[]{"LMB", 0, y, (W3 - G) / 2, K, "LMB"});
                out.add(new Object[]{"RMB", (W3 - G) / 2 + G, y, W3 - (W3 - G) / 2 - G, K, "RMB"});
            }
            y += K;
        }
        if (k.space) {
            y += G;
            if (out != null) out.add(new Object[]{"Space", 0, y, W3, 15, "SPACE"});
            y += 15;
        }
        int n = 0;
        for (String e : k.extra) {
            if (n % 3 == 0) y += G + (n == 0 ? 0 : K);
            if (out != null) out.add(new Object[]{shortName(e), (n % 3) * (K + G), y, K, K, e});
            n++;
        }
        if (n > 0) y += K;
        return new int[]{W3, y};
    }

    private static String shortName(String e) {
        switch (e) {
            case "Shift": return "Sh";
            case "Ctrl": return "Ct";
            default: return e;
        }
    }

    private static void drawKey(GuiGraphics g, Font f, String s, int x, int y, int w, int hh, boolean down, Config.Hud h, Config.Keys k, int col) {
        GuiGraphics oldG = Ui.g;
        Ui.g = g;
        int a = k.opacity;
        int fillc = down ? Ui.alpha(k.down, a) : Ui.alpha(k.idle, a);
        int tc = down ? k.downText : k.text;
        int r = k.style == 0 ? 3 : 0;
        s = Tr.t(s);
        if (k.style == 3) {
            tc = down ? k.down : k.text;
        } else if (k.style == 2) {
            outline(g, x, y, w, hh, down ? k.down : k.idle);
            if (down) g.fill(x + 1, y + 1, x + w - 1, y + hh - 1, Ui.alpha(k.down, 70));
            tc = down ? k.down : k.text;
        } else {
            Ui.rrect(x, y, w, hh, r, fillc);
        }
        if (h.border && k.style != 2 && k.style != 3) outline(g, x, y, w, hh, h.borderColor);
        // glyph box is (width-1) x 7: centre exactly
        int tw = f.width(s) - 1;
        g.drawString(f, s, x + (w - tw + 1) / 2, y + (hh - 7) / 2, tc, h.shadow);
        Ui.g = oldG;
    }

    private static void key(GuiGraphics g, Font f, String s, int x, int y, int w, int hh, boolean down, Config.Hud h, int col) {
        g.fill(x, y, x + w, y + hh, down ? Ui.alpha(h.borderColor, 220) : (h.bg ? Ui.alpha(h.bgColor, h.bgAlpha) : 0x00000000));
        if (h.border) outline(g, x, y, w, hh, h.borderColor);
        g.drawString(f, s, x + (w - f.width(s)) / 2, y + (hh - 8) / 2, col, h.shadow);
    }
}
