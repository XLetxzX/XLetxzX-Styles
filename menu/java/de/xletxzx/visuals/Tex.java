package de.xletxzx.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Real textures (from resource packs) for the menu: sharp pixels, animations,
 * and fake 3D blocks. Called by the feature part through reflection.
 */
public final class Tex {
    private static final class E {
        Identifier id;
        int w, h, frameH, frames, ft;
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
        float s = Math.min(w / (float) fw, h / (float) fh);
        if (s >= 1f) s = (float) Math.floor(s); // whole pixels stay sharp
        int dw = Math.max(1, Math.round(fw * s)), dh = Math.max(1, Math.round(fh * s));
        int dx = x + (w - dw) / 2, dy = y + (h - dh) / 2;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, e.id, dx, dy, 0f, (float) (frame(e) * fh), dw, dh, fw, fh, e.w, e.h);
        return true;
    }

    private static void face(DrawContext ctx, E e, float ox, float oy, float ux, float uy, float vx, float vy, int shade) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().mul(new Matrix3x2f(ux / 16f, uy / 16f, vx / 16f, vy / 16f, ox, oy));
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, e.id, 0, 0, 0f, (float) (frame(e) * e.frameH), 16, 16, e.w, e.frameH, e.w, e.h);
        if (shade != 0) ctx.fill(0, 0, 16, 16, shade);
        ctx.getMatrices().popMatrix();
    }

    /** Draws an isometric block (top + two sides) centered at cx, cy. size = width of the block. */
    public static boolean cube(Object c, String top, String side, int cx, int cy, int size) {
        E t = MAP.get(top), sd = MAP.get(side);
        if (t == null || t.id == null) return false;
        if (sd == null || sd.id == null) sd = t;
        DrawContext ctx = (DrawContext) c;
        float a = size / 1.732f;          // edge length
        float hx = a * 0.866f, hy = a / 2f;
        float x0 = cx, y0 = cy;            // front corner of the top face = center of the block
        // top: from left corner, U -> top corner, V -> front corner
        face(ctx, t, x0 - hx, y0 - hy, hx, -hy, hx, hy, 0);
        // left side
        face(ctx, sd, x0 - hx, y0 - hy, hx, hy, 0, a, 0x40000000);
        // right side
        face(ctx, sd, x0, y0, hx, -hy, 0, a, 0x66000000);
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
