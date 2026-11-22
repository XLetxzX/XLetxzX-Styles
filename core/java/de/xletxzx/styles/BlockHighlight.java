package de.xletxzx.styles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Custom outline (3 layers) and fill for the block you are looking at (own implementation). */
final class BlockHighlight {
    private static final int[][] STEP = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    private static final double EPS = 0.002;

    private BlockHighlight() {}

    /** Returns true when the vanilla outline should still be drawn. */
    static boolean render(WorldRenderContext ctx, BlockOutlineRenderState st) {
        Config.Hl h = Config.get().hl;
        if (!h.renderOn) return true;
        if (!h.o.on && !h.o2.on && !h.o3.on && !h.fill) return true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return true;
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        PoseStack ps = ctx.matrices();
        MultiBufferSource mb = ctx.consumers();
        BlockPos pos = st.pos();
        boolean collision = h.shape == 0 && !st.shape().isEmpty();
        AABB b = collision ? st.shape().bounds() : new AABB(0, 0, 0, 1, 1, 1);
        double ox = pos.getX() - cam.x, oy = pos.getY() - cam.y, oz = pos.getZ() - cam.z;
        double pulse = h.pulse ? 0.65 + 0.35 * Math.sin(System.currentTimeMillis() / 250.0) : 1.0;
        PoseStack.Pose pose = ps.last();

        boolean[] exposed = new boolean[6];
        for (int i = 0; i < 6; i++) {
            exposed[i] = mc.level.getBlockState(pos.offset(STEP[i][0], STEP[i][1], STEP[i][2])).isAir();
        }

        if (h.fill) {
            AABB fb = h.fMode == 3 ? new AABB(0, 0, 0, 1, 1, 1) : b;
            VertexConsumer vc = mb.getBuffer(RenderTypes.debugQuads());
            for (int f = 0; f < 6; f++) {
                if (h.fMode == 1 && !exposed[f]) continue;
                if (h.fMode == 2 && exposed[f]) continue;
                double[][] q = face(fb, f, ox, oy, oz, EPS);
                for (double[] v : q) {
                    int col = color(h, h.fColor, h.fColor2, h.fGradient, h.fRainbow, h.fOpacity, pulse, v[1] - oy, v[0] + v[2]);
                    vc.addVertex(pose, (float) v[0], (float) v[1], (float) v[2]).setColor(col);
                }
            }
        }

        Config.Layer[] layers = {h.o3, h.o2, h.o};
        for (int li = 0; li < 3; li++) {
            Config.Layer l = layers[li];
            if (!l.on) continue;
            VertexConsumer vc = mb.getBuffer(RenderTypes.lines());
            float w = Math.max(0.5f, l.width / 10f);
            double e = EPS + (2 - li) * 0.0005;
            // scaled box
            double cx = (b.minX + b.maxX) / 2, cy = (b.minY + b.maxY) / 2, cz = (b.minZ + b.maxZ) / 2;
            double hx = (b.maxX - b.minX) / 2, hy = (b.maxY - b.minY) / 2, hz = (b.maxZ - b.minZ) / 2;
            double k = l.sizePct / 100.0, add = l.blocks / 100.0;
            AABB sb = new AABB(cx - (hx * k + add), cy - (hy * k + add), cz - (hz * k + add),
                               cx + (hx * k + add), cy + (hy * k + add), cz + (hz * k + add));
            if (h.oMode == 1 && collision) {
                final double fcx = cx, fcy = cy, fcz = cz;
                final double kx = hx > 0 ? (hx * k + add) / hx : 1, ky = hy > 0 ? (hy * k + add) / hy : 1, kz = hz > 0 ? (hz * k + add) / hz : 1;
                st.shape().forAllEdges((x1, y1, z1, x2, y2, z2) ->
                    seg(vc, pose, h, l, w, pulse,
                        fcx + (x1 - fcx) * kx + ox, fcy + (y1 - fcy) * ky + oy, fcz + (z1 - fcz) * kz + oz,
                        fcx + (x2 - fcx) * kx + ox, fcy + (y2 - fcy) * ky + oy, fcz + (z2 - fcz) * kz + oz, oy));
            } else {
                for (int f = 0; f < 6; f++) {
                    if (h.oMode == 2 && !exposed[f]) continue;
                    if (h.oMode == 3 && exposed[f]) continue;
                    double[][] q = face(sb, f, ox, oy, oz, e);
                    for (int i = 0; i < 4; i++) {
                        double[] a = q[i], c = q[(i + 1) % 4];
                        seg(vc, pose, h, l, w, pulse, a[0], a[1], a[2], c[0], c[1], c[2], oy);
                    }
                }
            }
        }
        return h.keepVanilla;
    }

    /** Draws one edge, optionally cut up (corner / center cut) with different thickness near corner and center. */
    private static void seg(VertexConsumer vc, PoseStack.Pose pose, Config.Hl h, Config.Layer l, float w, double pulse,
                            double x1, double y1, double z1, double x2, double y2, double z2, double oy) {
        double cc = Math.min(0.45, l.cutCorner / 100.0), cm = Math.min(0.9, l.cutCenter / 100.0);
        if (cc <= 0 && cm <= 0 && l.outerMul == 100 && l.innerMul == 100) {
            line(vc, pose, h, l, w, pulse, x1, y1, z1, x2, y2, z2, oy);
            return;
        }
        float wo = Math.max(0.5f, w * l.outerMul / 100f), wi = Math.max(0.5f, w * l.innerMul / 100f);
        double a0 = cc, a1 = 0.5 - cm / 2, b0 = 0.5 + cm / 2, b1 = 1 - cc;
        if (a1 > a0) {
            double m = (a0 + a1) / 2;
            part(vc, pose, h, l, wo, pulse, x1, y1, z1, x2, y2, z2, a0, m, oy);
            part(vc, pose, h, l, wi, pulse, x1, y1, z1, x2, y2, z2, m, a1, oy);
        }
        if (b1 > b0) {
            double m = (b0 + b1) / 2;
            part(vc, pose, h, l, wi, pulse, x1, y1, z1, x2, y2, z2, b0, m, oy);
            part(vc, pose, h, l, wo, pulse, x1, y1, z1, x2, y2, z2, m, b1, oy);
        }
    }

    private static void part(VertexConsumer vc, PoseStack.Pose pose, Config.Hl h, Config.Layer l, float w, double pulse,
                             double x1, double y1, double z1, double x2, double y2, double z2, double t0, double t1, double oy) {
        line(vc, pose, h, l, w, pulse,
            x1 + (x2 - x1) * t0, y1 + (y2 - y1) * t0, z1 + (z2 - z1) * t0,
            x1 + (x2 - x1) * t1, y1 + (y2 - y1) * t1, z1 + (z2 - z1) * t1, oy);
    }

    private static void line(VertexConsumer vc, PoseStack.Pose pose, Config.Hl h, Config.Layer l, float w, double pulse,
                             double x1, double y1, double z1, double x2, double y2, double z2, double oy) {
        float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;
        dx /= len; dy /= len; dz /= len;
        int c1 = color(h, l.c1, l.c2, l.grad, l.rainbow, l.opacity, pulse, y1 - oy, x1 + z1);
        int c2 = color(h, l.c1, l.c2, l.grad, l.rainbow, l.opacity, pulse, y2 - oy, x2 + z2);
        vc.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(c1).setNormal(pose, dx, dy, dz).setLineWidth(w);
        vc.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(c2).setNormal(pose, dx, dy, dz).setLineWidth(w);
    }

    /** Four corners of one face of the box, counter-clockwise seen from outside, pushed out by eps. */
    private static double[][] face(AABB b, int f, double ox, double oy, double oz, double e) {
        double x0 = b.minX + ox - e, x1 = b.maxX + ox + e;
        double y0 = b.minY + oy - e, y1 = b.maxY + oy + e;
        double z0 = b.minZ + oz - e, z1 = b.maxZ + oz + e;
        switch (f) {
            case 0: return new double[][]{{x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}, {x1, y0, z1}};
            case 1: return new double[][]{{x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}, {x0, y0, z0}};
            case 2: return new double[][]{{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
            case 3: return new double[][]{{x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}};
            case 4: return new double[][]{{x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}, {x0, y0, z1}};
            default: return new double[][]{{x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}, {x1, y0, z0}};
        }
    }

    private static int color(Config.Hl h, int c1, int c2, boolean grad, boolean rainbow, int opacity, double pulse, double ly, double phase) {
        int rgb;
        if (rainbow) {
            double hue = (System.currentTimeMillis() * h.rainbowSpeed / 6000.0 + phase * h.rbDelay * 0.00048) % 1.0;
            if (hue < 0) hue += 1.0;
            rgb = hsv((float) hue, h.rbSat / 100f, h.rbBri / 100f);
        } else if (grad) {
            float t = (float) Math.max(0, Math.min(1, ly));
            rgb = Ui.mix(0xFF000000 | c1, 0xFF000000 | c2, t) & 0xFFFFFF;
        } else {
            rgb = c1 & 0xFFFFFF;
        }
        int a = (int) Math.max(0, Math.min(255, opacity * 2.55 * pulse));
        return (a << 24) | rgb;
    }

    private static int hsv(float h, float s, float v) {
        float r = clamp(Math.abs(h * 6f - 3f) - 1f), g = clamp(2f - Math.abs(h * 6f - 2f)), bl = clamp(2f - Math.abs(h * 6f - 4f));
        r = v * (1 - s + s * r); g = v * (1 - s + s * g); bl = v * (1 - s + s * bl);
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (bl * 255);
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
