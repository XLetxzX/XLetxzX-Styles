package de.xletxzx.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Real textures (from resource packs) for the menu: sharp pixels, animations,
 * and fake 3D blocks. Called by the feature part through reflection.
 */
public final class Tex {
    private static final class E {
        Identifier id;
        int w, h, frameH, frames, ft;
        NativeImage img;   // the pixels (kept to build pixel-exact blocks)
    }

    private static final Map<String, E> MAP = new HashMap<>();
    private static int counter;

    private Tex() {}

    public static boolean has(String key) { return MAP.containsKey(key); }

    /** Creates a texture from PNG bytes. frametime = ticks per animation frame. */
    public static void load(String key, byte[] png, int frametime) {
        if (MAP.containsKey(key)) return;
        E e = new E();
        try {
            if (png != null) {
                NativeImage img = NativeImage.read(new ByteArrayInputStream(smooth(png)));
                e.w = img.getWidth();
                e.h = img.getHeight();
                e.frameH = (e.h > e.w && e.h % e.w == 0) ? e.w : e.h;
                e.frames = Math.max(1, e.h / Math.max(1, e.frameH));
                e.ft = Math.max(1, frametime);
                e.id = Identifier.of("xletxzx", "dyn/t" + (counter++));
                e.img = img;
                NativeImageBackedTexture t = new NativeImageBackedTexture(() -> "xletxzx " + key, img);
                MinecraftClient.getInstance().getTextureManager().registerTexture(e.id, t);
            }
        } catch (Throwable t) {
            e.id = null;
        }
        MAP.put(key, e);
    }

    /** Small textures are enlarged with smooth interpolation so previews do not look blocky. */
    private static byte[] smooth(byte[] png) {
        if (true) return png;   // keep the pack pixels exactly as drawn: no blur
        try {
            java.awt.image.BufferedImage in = javax.imageio.ImageIO.read(new ByteArrayInputStream(png));
            if (in == null || in.getWidth() >= 64) return png;
            int k = Math.min(8, Math.max(1, 64 / in.getWidth()));
            int w = in.getWidth() * k, h = in.getHeight() * k;
            if (h > 8192) return png;
            java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = out.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(in, 0, 0, w, h, null);
            g.dispose();
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(out, "png", bo);
            return bo.toByteArray();
        } catch (Throwable t) {
            return png;
        }
    }

    private static int guiScale() {
        try { return (int) Math.max(1, Math.round(MinecraftClient.getInstance().getWindow().getScaleFactor())); } catch (Throwable t) { return 1; }
    }

    private static int frame(E e) {
        if (e.frames <= 1) return 0;
        return (int) ((System.currentTimeMillis() / (50L * e.ft)) % e.frames);
    }

    /** Draws a texture fitted into the box (keeps the aspect ratio). False if it is not loaded (or broken). */
    public static boolean draw(Object c, String key, int x, int y, int w, int h) {
        E e = MAP.get(key);
        if (e == null || e.id == null) return false;
        DrawContext ctx = (DrawContext) c;
        int fw = e.w, fh = e.frameH;
        // draw in whole SCREEN pixels: every texture pixel becomes exactly m x m screen pixels
        int gui = guiScale();
        int m = Math.min(w * gui / fw, h * gui / fh);
        if (m >= 1) {
            int pw = fw * m, ph = fh * m;                                   // size in screen pixels
            int sx = Math.round((x + w / 2f) * gui - pw / 2f), sy = Math.round((y + h / 2f) * gui - ph / 2f);
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().scale(1f / gui, 1f / gui);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, e.id, sx, sy, 0f, (float) (frame(e) * fh), pw, ph, fw, fh, e.w, e.h);
            ctx.getMatrices().popMatrix();
            return true;
        }
        // texture bigger than the box even at 1:1 - scale it down
        float s = Math.min(w / (float) fw, h / (float) fh);
        int dw = Math.max(1, Math.round(fw * s)), dh = Math.max(1, Math.round(fh * s));
        int dx = x + (w - dw) / 2, dy = y + (h - dh) / 2;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, e.id, dx, dy, 0f, (float) (frame(e) * fh), dw, dh, fw, fh, e.w, e.h);
        return true;
    }

    private static void face(DrawContext ctx, E e, float ox, float oy, float ux, float uy, float vx, float vy, int shade) {
        face(ctx, e, ox, oy, ux, uy, vx, vy, shade, 0xFFFFFFFF);
    }

    private static void face(DrawContext ctx, E e, float ox, float oy, float ux, float uy, float vx, float vy, int shade, int tint) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().mul(new Matrix3x2f(ux / 16f, uy / 16f, vx / 16f, vy / 16f, ox, oy));
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, e.id, 0, 0, 0f, (float) (frame(e) * e.frameH), 16, 16, e.w, e.frameH, e.w, e.h, tint);
        if (shade != 0) ctx.fill(0, 0, 16, 16, shade);
        ctx.getMatrices().popMatrix();
    }

    /** Draws an isometric block (top + two sides) centered at cx, cy. size = width of the block. */
    public static boolean cube(Object c, String top, String side, int cx, int cy, int size) {
        return cube(c, top, side, cx, cy, size, 0xFFFFFFFF);
    }

    // ---------------------------------------------------------------- pixel-exact blocks
    private static final Map<String, E> ISO = new java.util.LinkedHashMap<>();

    /**
     * Builds the block as its own picture in the exact screen resolution: every texture pixel becomes the
     * same clean pixel-art step (2:1 iso), nothing is stretched afterwards. Returns false if it cannot.
     */
    private static boolean isoCube(DrawContext ctx, E t, E sd, String top, String side, int cx, int cy, int size, int topTint) {
        if (t.img == null || sd.img == null) return false;
        int gui = guiScale();
        int n = size * gui;                         // picture size = exact screen pixels of the box
        if (n < 48) return false;
        double k = n / 64.0;                        // screen pixels per "half texel step"
        String key = top + "|" + side + "|" + Integer.toHexString(topTint) + "|" + n;
        E iso = ISO.get(key);
        if (iso == null) {
            iso = buildIso(t, sd, topTint, n, k);
            ISO.put(key, iso);
            if (ISO.size() > 160) {   // forget the oldest pictures
                java.util.Iterator<Map.Entry<String, E>> it = ISO.entrySet().iterator();
                Map.Entry<String, E> old = it.next();
                it.remove();
                if (old.getValue().id != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(old.getValue().id);
            }
        }
        if (iso.id == null) return false;
        // draw 1:1 in screen pixels so nothing is stretched again
        int sx = Math.round(cx * (float) gui - n / 2f), sy = Math.round(cy * (float) gui - n / 2f);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(1f / gui, 1f / gui);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, iso.id, sx, sy, 0f, 0f, n, n, n, n, n, n);
        ctx.getMatrices().popMatrix();
        return true;
    }

    private static E buildIso(E t, E sd, int topTint, int n, double k) {
        E e = new E();
        try {
            NativeImage out = new NativeImage(n, n, true);
            // Minecraft's own inventory view of a block: turned 45 degrees, looked at from 30 degrees above.
            // A block edge of length L then shows: top face 1.414 L wide and 0.707 L high, sides 0.866 L high.
            double L = n / 1.62;                       // block height 1.573 L fits into the picture
            double hx = 0.7071 * L, hy = 0.3536 * L, vz = 0.8660 * L;
            double tx = n / 2.0, ty = (n - (2 * hy + vz)) / 2.0;   // top corner of the block
            // smooth edges: every screen pixel is the average of 4 x 4 points, so slanted edges (the outline and
            // the borders between texture pixels) get no stairs, while the texture itself stays sharp
            final int ss = 4;
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    long sr = 0, sg = 0, sb = 0, sa = 0;
                    for (int j = 0; j < ss; j++) {
                        for (int i = 0; i < ss; i++) {
                            int c = cubeAt(t, sd, topTint, x + (i + 0.5) / ss, y + (j + 0.5) / ss, tx, ty, hx, hy, vz);
                            int ca = (c >>> 24) & 255;
                            sa += ca;
                            sr += (long) ((c >> 16) & 255) * ca;
                            sg += (long) ((c >> 8) & 255) * ca;
                            sb += (long) (c & 255) * ca;
                        }
                    }
                    int argb = 0;
                    if (sa > 0) {
                        int a = (int) (sa / (ss * ss));
                        argb = (a << 24) | ((int) (sr / sa) << 16) | ((int) (sg / sa) << 8) | (int) (sb / sa);
                    }
                    out.setColorArgb(x, y, argb);
                }
            }
            e.w = n; e.h = n; e.frameH = n; e.frames = 1; e.ft = 1;
            e.id = Identifier.of("xletxzx", "dyn/iso" + (counter++));
            MinecraftClient.getInstance().getTextureManager().registerTexture(e.id, new NativeImageBackedTexture(() -> "xletxzx iso", out));
        } catch (Throwable ex) {
            e.id = null;
        }
        return e;
    }

    /** Colour of the block at one point of the picture (Minecraft's inventory view), 0 = outside. */
    private static int cubeAt(E t, E sd, int topTint, double px, double py, double tx, double ty, double hx, double hy, double vz) {
        // top face: from the top corner, a -> right corner, b -> left corner
        double sum = (py - ty) / hy, diff = (px - tx) / hx;
        double a = (sum + diff) / 2, b = (sum - diff) / 2;
        if (a >= 0 && a < 1 && b >= 0 && b < 1) return shade(mul(sample(t, b, a), topTint), 1.0);
        if (px < tx) {
            // left face: from the left corner, a -> front corner, c -> down
            double la = (px - (tx - hx)) / hx;
            double lc = (py - (ty + hy) - la * hy) / vz;
            if (la >= 0 && la < 1 && lc >= 0 && lc < 1) return shade(sample(sd, la, lc), 0.80);
        } else {
            // right face: from the front corner, a -> right corner, c -> down
            double ra = (px - tx) / hx;
            double rc = (py - (ty + 2 * hy) + ra * hy) / vz;
            if (ra >= 0 && ra < 1 && rc >= 0 && rc < 1) return shade(sample(sd, ra, rc), 0.60);
        }
        return 0;
    }

    /** Colour of the texture at u, v (0..1) in frame 0, nearest pixel. */
    private static int sample(E e, double u, double v) {
        int px = Math.min(e.w - 1, Math.max(0, (int) (u * e.w)));
        int py = Math.min(e.frameH - 1, Math.max(0, (int) (v * e.frameH)));
        return e.img.getColorArgb(px, py);
    }

    private static int mul(int argb, int tint) {
        if (tint == 0xFFFFFFFF) return argb;
        int r = ((argb >> 16) & 255) * ((tint >> 16) & 255) / 255;
        int g = ((argb >> 8) & 255) * ((tint >> 8) & 255) / 255;
        int b = (argb & 255) * (tint & 255) / 255;
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static int shade(int argb, double f) {
        int a = (argb >>> 24) & 255;
        if (a == 0) return 0;
        int r = (int) (((argb >> 16) & 255) * f), g = (int) (((argb >> 8) & 255) * f), b = (int) ((argb & 255) * f);
        return (255 << 24) | (r << 16) | (g << 8) | b;   // blocks are solid
    }

    /** Same, with a colour multiplied onto the top face (grass is grey in the texture and coloured by the game). */
    public static boolean cube(Object c, String top, String side, int cx, int cy, int size, int topTint) {
        E t = MAP.get(top), sd = MAP.get(side);
        if (t == null || t.id == null) return false;
        if (sd == null || sd.id == null) sd = t;
        if (isoCube((DrawContext) c, t, sd, top, side, cx, cy, size, topTint)) return true;
        DrawContext ctx = (DrawContext) c;
        // whole pixels per texel along the edges keep the pack pixels even
        float a = Math.max(16, Math.round(size / 1.732f / 8f) * 8);   // edge length
        float hx = a * 0.866f, hy = a / 2f;
        float x0 = cx, y0 = cy;            // front corner of the top face = center of the block
        // soft shadow under the block
        ctx.fill((int) (x0 - hx * 0.7f), (int) (y0 + a) + 1, (int) (x0 + hx * 0.7f), (int) (y0 + a) + 3, 0x28000000);
        // top: from left corner, U -> top corner, V -> front corner
        face(ctx, t, x0 - hx, y0 - hy, hx, -hy, hx, hy, 0, topTint);
        // left side (a bit darker), right side (darker) - like Minecraft's block light
        face(ctx, sd, x0 - hx, y0 - hy, hx, hy, 0, a, 0x33000000);
        face(ctx, sd, x0, y0, hx, -hy, 0, a, 0x5A000000);
        return true;
    }

    private static void faceUv(DrawContext ctx, E e, float ox, float oy, float ux, float uy, float vx, float vy, int shade, int u, int v, int rw, int rh) {
        float k = e.w / 64f;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().mul(new Matrix3x2f(ux / 16f, uy / 16f, vx / 16f, vy / 16f, ox, oy));
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, e.id, 0, 0, u * k, v * k, 16, 16, Math.round(rw * k), Math.round(rh * k), e.w, e.h);
        if (shade != 0) ctx.fill(0, 0, 16, 16, shade);
        ctx.getMatrices().popMatrix();
    }

    // ---------------------------------------------------------------- crystal on obsidian (animated 3D scene)
    private static final double YAW = Math.toRadians(45), PITCH = Math.toRadians(30);

    /** Orthographic camera: returns {screenX, screenY, depth} (depth grows towards the viewer). */
    private static double[] project(double[] p) {
        double x1 = p[0] * Math.cos(YAW) - p[2] * Math.sin(YAW);
        double z1 = p[0] * Math.sin(YAW) + p[2] * Math.cos(YAW);
        double sy = -p[1] * Math.cos(PITCH) + z1 * Math.sin(PITCH);
        double depth = z1 * Math.cos(PITCH) + p[1] * Math.sin(PITCH);
        return new double[]{x1, sy, depth};
    }

    private static double[] rotY(double[] p, double a) {
        double c = Math.cos(a), s = Math.sin(a);
        return new double[]{p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c};
    }

    /** Rotation around the normalised axis (1, 0, 1) like the vanilla crystal. */
    private static double[] rotDiag(double[] p, double a) {
        double k = Math.sqrt(0.5), kx = k, ky = 0, kz = k;
        double c = Math.cos(a), s = Math.sin(a);
        double dot = kx * p[0] + ky * p[1] + kz * p[2];
        double cx = ky * p[2] - kz * p[1], cy = kz * p[0] - kx * p[2], cz = kx * p[1] - ky * p[0];
        return new double[]{
            p[0] * c + cx * s + kx * dot * (1 - c),
            p[1] * c + cy * s + ky * dot * (1 - c),
            p[2] * c + cz * s + kz * dot * (1 - c)};
    }

    private static final class Face {
        double[] p0, pu, pv; double depth; E tex; int u, v, size, shade;
    }

    /** Adds the 6 faces of a cube (half size h, centre c) whose corners go through rot. uv = box layout origin in skin pixels. */
    private static void cube(List<Face> out, E tex, double h, double[] c, java.util.function.UnaryOperator<double[]> rot,
                             int u0, int v0, int d, boolean shaded) {
        double[][][] f = {
            {{-h, h, -h}, {h, h, -h}, {-h, h, h}},     // up
            {{-h, -h, h}, {h, -h, h}, {-h, -h, -h}},   // down
            {{-h, h, h}, {-h, h, -h}, {-h, -h, h}},    // west
            {{-h, h, -h}, {h, h, -h}, {-h, -h, -h}},   // north
            {{h, h, -h}, {h, h, h}, {h, -h, -h}},      // east
            {{h, h, h}, {-h, h, h}, {h, -h, h}},       // south
        };
        int[][] uv = {{u0 + d, v0}, {u0 + 2 * d, v0}, {u0, v0 + d}, {u0 + d, v0 + d}, {u0 + 2 * d, v0 + d}, {u0 + 3 * d, v0 + d}};
        int[] shade = {0, 0x55000000, 0x30000000, 0x40000000, 0x30000000, 0x40000000};
        for (int i = 0; i < 6; i++) {
            Face fc = new Face();
            double[] a = rot.apply(f[i][0]), b = rot.apply(f[i][1]), e = rot.apply(f[i][2]);
            fc.p0 = add(a, c); fc.pu = add(b, c); fc.pv = add(e, c);
            double[] far = add(rot.apply(new double[]{f[i][1][0] + f[i][2][0] - f[i][0][0], f[i][1][1] + f[i][2][1] - f[i][0][1], f[i][1][2] + f[i][2][2] - f[i][0][2]}), c);
            double[] mid = {(fc.p0[0] + far[0]) / 2, (fc.p0[1] + far[1]) / 2, (fc.p0[2] + far[2]) / 2};
            fc.depth = project(mid)[2];
            fc.tex = tex; fc.u = uv[i][0]; fc.v = uv[i][1]; fc.size = d;
            fc.shade = shaded ? shade[i] : 0;
            out.add(fc);
        }
    }

    private static double[] add(double[] a, double[] b) { return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]}; }

    /**
     * Obsidian block (normal texture) with an end crystal floating and turning above it, like in the game.
     * crystalKey = entity skin (64x32), obsidianKey = 16x16 block texture.
     */
    public static boolean crystalScene(Object c, String crystalKey, String obsidianKey, int cx, int cy, int size) {
        E cr = MAP.get(crystalKey), ob = MAP.get(obsidianKey);
        if (cr == null || cr.id == null || ob == null || ob.id == null) return false;
        DrawContext ctx = (DrawContext) c;
        double ticks = System.currentTimeMillis() / 50.0;
        double spin = Math.toRadians(ticks * 3.0);
        double bob = Math.sin(ticks * 0.2) * 0.12;
        double tilt = Math.toRadians(60);

        List<Face> faces = new ArrayList<>();
        // obsidian: block from y -1 to 0 (16x16 texture, same on every side)
        int obSize = ob.w;
        for (Face f : cubeList(ob, 0.5, new double[]{0, -0.5, 0}, p -> p, obSize)) faces.add(f);
        // crystal: outer glass, inner glass, core - each turned once more
        double[] centre = {0, 0.62 + bob, 0};
        java.util.function.UnaryOperator<double[]> r1 = p -> rotY(rotDiag(p, tilt), spin);
        java.util.function.UnaryOperator<double[]> r2 = p -> r1.apply(rotDiag(rotY(p, spin), tilt));
        java.util.function.UnaryOperator<double[]> r3 = p -> r2.apply(rotDiag(rotY(p, spin), tilt));
        cube(faces, cr, 0.36, centre, r1, 0, 0, 8, false);
        cube(faces, cr, 0.36 * 0.875, centre, r2, 0, 0, 8, false);
        cube(faces, cr, 0.36 * 0.875 * 0.875, centre, r3, 32, 0, 8, true);
        faces.sort((a, b) -> Double.compare(a.depth, b.depth));

        double scale = size / 2.3;
        double oy = cy + size * 0.06;   // world y 0 sits a little below the middle
        for (Face f : faces) {
            double[] s0 = project(f.p0), su = project(f.pu), sv = project(f.pv);
            float ox = (float) (cx + s0[0] * scale), oyy = (float) (oy + s0[1] * scale);
            float ux = (float) ((su[0] - s0[0]) * scale), uy = (float) ((su[1] - s0[1]) * scale);
            float vx = (float) ((sv[0] - s0[0]) * scale), vy = (float) ((sv[1] - s0[1]) * scale);
            // the GUI drops quads that are turned the "wrong" way: always hand it a positively oriented face
            if (ux * vy - uy * vx < 0) {
                float tx = ux, ty = uy;
                ux = vx; uy = vy;
                vx = tx; vy = ty;
            }
            if (Math.abs(ux * vy - uy * vx) < 0.01f) continue;   // seen exactly edge-on
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().mul(new Matrix3x2f(ux / 16f, uy / 16f, vx / 16f, vy / 16f, ox, oyy));
            E e = f.tex;
            if (e == ob) {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, e.id, 0, 0, 0f, (float) (frame(e) * e.frameH), 16, 16, e.w, e.frameH, e.w, e.h);
            } else {
                float k = e.w / 64f;
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, e.id, 0, 0, f.u * k, f.v * k, 16, 16,
                        Math.round(f.size * k), Math.round(f.size * k), e.w, e.h);
            }
            if (f.shade != 0) ctx.fill(0, 0, 16, 16, f.shade);
            ctx.getMatrices().popMatrix();
        }
        return true;
    }

    /** Block cube: every face shows the whole texture (frame 0 of animations). */
    private static List<Face> cubeList(E tex, double h, double[] c, java.util.function.UnaryOperator<double[]> rot, int texSize) {
        List<Face> out = new ArrayList<>();
        cube(out, tex, h, c, rot, 0, 0, texSize, true);
        return out;
    }

    /** End crystal drawn from the entity skin (64x32): a glass cube with its real faces. */
    public static boolean crystal(Object c, String key, int cx, int cy, int size) {
        E t = MAP.get(key);
        if (t == null || t.id == null) return false;
        DrawContext ctx = (DrawContext) c;
        float a = size / 1.732f, hx = a * 0.866f, hy = a / 2f;
        float x0 = cx, y0 = cy;
        faceUv(ctx, t, x0 - hx, y0 - hy, hx, -hy, hx, hy, 0, 8, 0, 8, 8);
        faceUv(ctx, t, x0 - hx, y0 - hy, hx, hy, 0, a, 0x30000000, 8, 8, 8, 8);
        faceUv(ctx, t, x0, y0, hx, -hy, 0, a, 0x55000000, 16, 8, 8, 8);
        return true;
    }
}
