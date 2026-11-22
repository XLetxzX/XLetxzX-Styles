package de.xletxzx.styles;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

/** CPvP options: hide explosion particles and totem / crystal sounds (client side only). */
public final class Cpvp {
    private Cpvp() {}

    private static final List<double[]> RECENT = new ArrayList<>();
    private static final int KEEP = 6 * 8;

    static void tick(Minecraft mc) {
        Config.Cpvp c = Config.get().cpvp;
        if (mc.level == null || !(c.noCrystalExpl || c.noCrystalSnd)) { RECENT.clear(); ANCH_NOW = new ArrayList<>(); ANCH_PREV = new ArrayList<>(); return; }
        try {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof EndCrystal) {
                    RECENT.add(new double[]{e.getX(), e.getY() + 0.5, e.getZ()});
                }
            }
        } catch (Throwable ignored) {}
        while (RECENT.size() > KEEP) RECENT.remove(0);
        // respawn anchors near the player (they explode like crystals)
        if (mc.player != null && (mc.level.getGameTime() & 1) == 0) {
            ANCH_PREV = ANCH_NOW;
            ANCH_NOW = new ArrayList<>();
            net.minecraft.core.BlockPos.MutableBlockPos mp = new net.minecraft.core.BlockPos.MutableBlockPos();
            int px = mc.player.getBlockX(), py = mc.player.getBlockY(), pz = mc.player.getBlockZ();
            for (int dx = -8; dx <= 8; dx++) for (int dy = -6; dy <= 6; dy++) for (int dz = -8; dz <= 8; dz++) {
                mp.set(px + dx, py + dy, pz + dz);
                try {
                    if (mc.level.getBlockState(mp).is(net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR))
                        ANCH_NOW.add(new double[]{mp.getX() + 0.5, mp.getY() + 0.5, mp.getZ() + 0.5});
                } catch (Throwable ignored) {}
            }
        }
    }

    private static List<double[]> ANCH_NOW = new ArrayList<>(), ANCH_PREV = new ArrayList<>();

    static boolean nearCrystal(double x, double y, double z) {
        for (double[] p : RECENT) {
            double dx = p[0] - x, dy = p[1] - y, dz = p[2] - z;
            if (dx * dx + dy * dy + dz * dz < 4.0) return true;
        }
        for (List<double[]> l : List.of(ANCH_NOW, ANCH_PREV)) for (double[] p : l) {
            double dx = p[0] - x, dy = p[1] - y, dz = p[2] - z;
            if (dx * dx + dy * dy + dz * dz < 4.0) return true;
        }
        return false;
    }

    /** True when this particle should not be spawned. */
    public static boolean blockParticle(Object options, double x, double y, double z) {
        Config.Cpvp c = Config.get().cpvp;
        if (!(c.noExpl || c.noCrystalExpl)) return false;
        try {
            net.minecraft.core.particles.ParticleOptions po = (net.minecraft.core.particles.ParticleOptions) options;
            String p = BuiltInRegistries.PARTICLE_TYPE.getKey(po.getType()).getPath();
            if (!(p.equals("explosion") || p.equals("explosion_emitter"))) return false;
            return c.noExpl || nearCrystal(x, y, z);
        } catch (Throwable t) {
            return false;
        }
    }

    /** True when this sound should not be played. */
    public static boolean blockSound(String id, double x, double y, double z) {
        Config.Cpvp c = Config.get().cpvp;
        if (c.noTotemSnd && id.endsWith("item.totem.use")) return true;
        if (id.endsWith("entity.generic.explode")) {
            if (c.noExpl) return true;
            if (c.noCrystalSnd) return true;
        }
        return false;
    }
}
