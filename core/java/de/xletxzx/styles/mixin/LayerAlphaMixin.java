package de.xletxzx.styles.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.xletxzx.styles.ThirdItem;
import java.util.List;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opacity Editor: the held item is drawn translucent through the normal tint path
 * (every quad gets a tint slot whose alpha is the chosen opacity), so it works with any item renderer.
 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public abstract class LayerAlphaMixin {
    private static java.lang.reflect.Field xletxzx$rtField, xletxzx$tintField;
    private static boolean xletxzx$searched;
    private static int xletxzx$logs;

    @Inject(method = "method_65614", at = @At("HEAD"), require = 0)
    private void xletxzx$head(PoseStack pose, SubmitNodeCollector nodes, int light, int overlay, int outline, CallbackInfo ci) {
        try {
            ItemStackRenderState.LayerRenderState me = (ItemStackRenderState.LayerRenderState) (Object) this;
            if (!xletxzx$searched) {
                xletxzx$searched = true;
                for (java.lang.reflect.Field f : me.getClass().getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                    if (f.getType() == RenderType.class) { f.setAccessible(true); xletxzx$rtField = f; }
                    if (f.getType() == int[].class) { f.setAccessible(true); xletxzx$tintField = f; }
                }
            }
            if (xletxzx$tintField == null) return;
            int al = ThirdItem.alpha;
            int[] t = me.prepareTintLayers(0);
            boolean ext = ThirdItem.EXT.containsKey(t);
            if (al >= 100) {
                if (ext && t.length > 0) t[t.length - 1] = -1;
                return;
            }
            if (!ext) {
                int[] n = java.util.Arrays.copyOf(t, t.length + 1);
                xletxzx$tintField.set(me, n);
                ThirdItem.EXT.put(n, Boolean.TRUE);
                t = n;
            }
            int a = Math.max(0, Math.min(255, al * 255 / 100));
            int n = t.length - 1;
            for (int i = 0; i < n; i++) t[i] = (t[i] & 0xFFFFFF) | (a << 24);
            t[n] = (a << 24) | 0xFFFFFF;
            List<BakedQuad> list = me.prepareQuadList();
            for (int i = 0; i < list.size(); i++) {
                BakedQuad q = list.get(i);
                if (!q.isTinted()) {
                    list.set(i, new BakedQuad(q.position0(), q.position1(), q.position2(), q.position3(),
                        q.packedUV0(), q.packedUV1(), q.packedUV2(), q.packedUV3(), n,
                        q.direction(), q.sprite(), q.shade(), q.lightEmission()));
                }
            }
            if (xletxzx$logs < 6 && (list.size() > 0 ? xletxzx$logs % 2 == 0 : true)) { xletxzx$logs++; System.out.println("[XLetxzX] opacity layer applied, alpha=" + al + ", quads=" + list.size()); }
            if (xletxzx$rtField != null && xletxzx$rtField.get(me) == Sheets.cutoutBlockSheet()) {
                me.setRenderType(Sheets.translucentBlockItemSheet());
            }
        } catch (Throwable ex) {
            if (xletxzx$logs++ < 3) System.out.println("[XLetxzX] opacity layer error: " + ex);
        }
    }
}
