package de.xletxzx.styles;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;

/** Glow outline on chosen dropped items. */
public final class Glow {
    private Glow() {}

    private static final java.util.Map<Integer, double[]> TRACK = new java.util.HashMap<>();
    private static final java.util.Random RND = new java.util.Random();

    private static boolean chosen(ItemEntity ie) {
        Config c = Config.get();
        if (c.glow.all) return true;
        Boolean on = c.glow.items.get(BuiltInRegistries.ITEM.getKey(ie.getItem().getItem()).getPath());
        return on != null && on;
    }

    /** Soft coloured haze around dropped items and a small cloud when one disappears. */
    static void tick(net.minecraft.client.Minecraft mc) {
        Config c = Config.get();
        Config.Hud h = c.hud.get("glow");
        if (mc.level == null || mc.player == null || h == null || !h.on) { TRACK.clear(); return; }
        java.util.Set<Integer> now = new java.util.HashSet<>();
        int count = 0;
        int rgb = color();
        float scale = 0.8f + c.glow.power / 100f;
        int per = 1 + c.glow.power / 50;
        try {
            for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                if (!(e instanceof ItemEntity ie) || ie.isRemoved()) continue;
                if (e.distanceToSqr(mc.player) > 40 * 40 || !chosen(ie)) continue;
                if (count++ > 48) break;
                now.add(e.getId());
                TRACK.put(e.getId(), new double[]{e.getX(), e.getY(), e.getZ(), rgb});
                for (int i = 0; i < per; i++) {
                    double a = RND.nextDouble() * Math.PI * 2, r = Math.sqrt(RND.nextDouble()) * 0.4;
                    mc.level.addParticle(new net.minecraft.core.particles.DustParticleOptions(rgb, scale),
                        e.getX() + Math.cos(a) * r, e.getY() + 0.03 + RND.nextDouble() * 0.2, e.getZ() + Math.sin(a) * r, 0, 0.003, 0);
                }
            }
            java.util.Iterator<java.util.Map.Entry<Integer, double[]>> it = TRACK.entrySet().iterator();
            while (it.hasNext()) {
                java.util.Map.Entry<Integer, double[]> en = it.next();
                if (now.contains(en.getKey())) continue;
                double[] p = en.getValue();
                it.remove();
                if (!c.glow.poof) continue;
                double dx = p[0] - mc.player.getX(), dy = p[1] - mc.player.getY(), dz = p[2] - mc.player.getZ();
                if (dx * dx + dy * dy + dz * dz > 30 * 30) continue;
                int col = (int) p[3];
                for (int i = 0; i < 8; i++) {
                    mc.level.addParticle(new net.minecraft.core.particles.DustParticleOptions(col, 1.3f),
                        p[0], p[1] + 0.15, p[2], (RND.nextDouble() - 0.5) * 0.08, 0.02 + RND.nextDouble() * 0.03, (RND.nextDouble() - 0.5) * 0.08);
                }
                for (int i = 0; i < 2; i++) {
                    mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.POOF,
                        p[0], p[1] + 0.15, p[2], (RND.nextDouble() - 0.5) * 0.03, 0.01, (RND.nextDouble() - 0.5) * 0.03);
                }
            }
        } catch (Throwable t) {
            TRACK.clear();
        }
    }

    public static boolean matches(Object entity) {
        if (!(entity instanceof ItemEntity ie)) return false;
        Config c = Config.get();
        if (!c.glow.outline) return false;
        Config.Hud h = c.hud.get("glow");
        if (h == null || !h.on) return false;
        if (c.glow.all) return true;
        if (c.glow.items.isEmpty()) return false;
        Boolean on = c.glow.items.get(BuiltInRegistries.ITEM.getKey(ie.getItem().getItem()).getPath());
        return on != null && on;
    }

    public static int color() {
        Config.Glow g = Config.get().glow;
        int col = g.rainbow ? Ui.rainbow(0) : g.color;
        if (g.pulse) {
            float t = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 220.0);
            col = Ui.mix(0xFF000000, col, t);
        }
        col = Ui.mix(0xFF000000, col, Math.max(0.1f, Math.min(1f, g.power / 100f)));
        return col & 0xFFFFFF;
    }
}
