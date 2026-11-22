package de.xletxzx.styles;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Motion blur and colour saturation through a vanilla post-effect chain. */
final class PostFx {
    private PostFx() {}

    private static boolean ours;
    private static boolean failed;

    static void tick(Minecraft mc) {
        if (failed || mc.level == null || mc.gameRenderer == null) return;
        try {
            Config.Post p = Config.get().post;
            int b = p.blurOn ? Math.max(0, Math.min(5, p.blur)) : 0;
            int s = p.satOn ? Math.max(0, Math.min(8, p.sat)) : 4;
            Identifier cur = mc.gameRenderer.currentPostEffect();
            if (b == 0 && s == 4) {
                if (ours) { mc.gameRenderer.clearPostEffect(); ours = false; }
                return;
            }
            Identifier want = Identifier.fromNamespaceAndPath("xletxzx_styles", "fx_b" + b + "_s" + s);
            if (!want.equals(cur)) {
                ((de.xletxzx.styles.mixin.GameRendererAccess) (Object) mc.gameRenderer).xletxzx$setPost(want);
                ours = true;
            }
        } catch (Throwable t) {
            failed = true;
            System.out.println("[XLetxzX Styles] post effects disabled: " + t);
        }
    }
}
