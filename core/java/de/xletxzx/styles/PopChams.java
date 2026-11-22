package de.xletxzx.styles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Totem pop chams: when a player pops a totem (the totem sound plays at their position), a coloured,
 * see-through copy of their body stays at that spot for a moment and fades out.
 */
final class PopChams {
    private PopChams() {}

    private static final class Ghost {
        double x, y, z;
        float yaw;
        boolean crouch;
        long start;
    }

    private static final List<Ghost> GHOSTS = new ArrayList<>();

    // body parts in model pixels (1/16 block): {minX, minY, minZ, maxX, maxY, maxZ}, feet at y = 0, facing +Z
    private static final float[][] PARTS = {
        {-4, 24, -4, 4, 32, 4},    // head
        {-4, 12, -2, 4, 24, 2},    // body
        {-8, 12, -2, -4, 24, 2},   // right arm
        {4, 12, -2, 8, 24, 2},     // left arm
        {-4, 0, -2, 0, 12, 2},     // right leg
        {0, 0, -2, 4, 12, 2},      // left leg
    };

    /** Called for every sound; a totem sound marks a pop at that position. */
    static void onSound(String id, double x, double y, double z) {
        if (!id.endsWith("item.totem.use")) return;
        Config.Cpvp c = Config.get().cpvp;
        if (!c.popChams) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Player best = null;
        double bestD = 3.0 * 3.0;
        for (Player p : mc.level.players()) {
            if (p == mc.player && !c.popSelf) continue;
            double dx = p.getX() - x, dy = p.getY() + 1 - y, dz = p.getZ() - z;
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bestD) { bestD = d; best = p; }
        }
        if (best == null) return;
        Ghost g = new Ghost();
        g.x = best.getX(); g.y = best.getY(); g.z = best.getZ();
        g.yaw = best.yBodyRot;
        g.crouch = best.isCrouching();
        g.start = System.currentTimeMillis();
        synchronized (GHOSTS) {
            GHOSTS.add(g);
            while (GHOSTS.size() > 24) GHOSTS.remove(0);
        }
    }

    static void render(WorldRenderContext ctx) {
        Config.Cpvp c = Config.get().cpvp;
        if (GHOSTS.isEmpty()) return;
        if (!c.popChams) { synchronized (GHOSTS) { GHOSTS.clear(); } return; }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { synchronized (GHOSTS) { GHOSTS.clear(); } return; }
        PoseStack ps = ctx.matrices();
        MultiBufferSource mb = ctx.consumers();
        if (ps == null || mb == null) return;
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        long now = System.currentTimeMillis();
        long life = Math.max(200, c.popTime * 100L);
        PoseStack.Pose pose = ps.last();
        VertexConsumer fill = mb.getBuffer(RenderTypes.debugQuads());
        synchronized (GHOSTS) {
            Iterator<Ghost> it = GHOSTS.iterator();
            while (it.hasNext()) {
                Ghost g = it.next();
                float t = (now - g.start) / (float) life;
                if (t >= 1f) { it.remove(); continue; }
                float fade = 1f - t;
                int a = (int) Math.max(0, Math.min(255, c.popOpacity * 2.55f * fade));
                int col = (a << 24) | (c.popColor & 0xFFFFFF);
                double rise = c.popRise ? t * 0.6 : 0;
                double ox = g.x - cam.x, oy = g.y - cam.y + rise, oz = g.z - cam.z;
                double rad = Math.toRadians(-g.yaw);
                double cos = Math.cos(rad), sin = Math.sin(rad);
                float grow = 0.4f + t * 0.8f;  // grows a little while fading
                for (int pi = 0; pi < PARTS.length; pi++) {
                    float[] p = PARTS[pi];
                    float lift = g.crouch && pi < 4 ? -3f : 0f;
                    box(fill, pose, p[0] - grow, p[1] - grow + lift, p[2] - grow, p[3] + grow, p[4] + grow + lift, p[5] + grow,
                        ox, oy, oz, cos, sin, col);
                }
            }
        }
    }

    /** One box in model pixels, turned around the feet by the body rotation. */
    private static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                            double ox, double oy, double oz, double cos, double sin, int col) {
        float[][][] faces = {
            {{x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}, {x1, y0, z1}},
            {{x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}, {x0, y0, z0}},
            {{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}},
            {{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}},
            {{x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}, {x0, y0, z1}},
            {{x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}, {x1, y0, z0}},
        };
        for (float[][] f : faces) {
            for (float[] v : f) {
                double lx = v[0] / 16.0, ly = v[1] / 16.0, lz = v[2] / 16.0;
                double wx = lx * cos - lz * sin, wz = lx * sin + lz * cos;
                vc.addVertex(pose, (float) (wx + ox), (float) (ly + oy), (float) (wz + oz)).setColor(col);
            }
        }
    }
}
