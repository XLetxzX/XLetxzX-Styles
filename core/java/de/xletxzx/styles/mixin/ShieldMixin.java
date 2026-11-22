package de.xletxzx.styles.mixin;

import de.xletxzx.styles.ShieldFx;
import de.xletxzx.styles.ThirdItem;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import de.xletxzx.styles.Config;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Tints the held shield by its state and applies the Opacity Editor opacity. */
@Mixin(targets = "net.minecraft.class_10509")
public abstract class ShieldMixin {
    private static int xletxzx$logs;

    /** Small shield: scales the shield model itself (works in hand and from outside). */
    @Inject(method = "method_65707", at = @At("HEAD"), require = 0)
    private void xletxzx$smallHead(DataComponentMap m, ItemDisplayContext c, PoseStack p, SubmitNodeCollector n, int l, int o, boolean f, int out, CallbackInfo ci) {
        p.pushPose();
        try {
            Config.Tweaks t = Config.get().tweaks;
            if (t.smallShield) {
                float s = t.shieldScale / 100f;
                p.scale(s, s, s);
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "method_65707", at = @At("RETURN"), require = 0)
    private void xletxzx$smallTail(DataComponentMap m, ItemDisplayContext c, PoseStack p, SubmitNodeCollector n, int l, int o, boolean f, int out, CallbackInfo ci) {
        p.popPose();
    }

    @ModifyArgs(method = "method_65707",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/class_11659;method_73494(Lnet/minecraft/class_630;Lnet/minecraft/class_4587;Lnet/minecraft/class_1921;IILnet/minecraft/class_1058;ZZILnet/minecraft/class_11683$class_11792;I)V"),
        require = 0)
    private void xletxzx$shield(Args args) {
        try {
            int tint = args.get(8);
            tint = ShieldFx.tint(tint);
            int al = ThirdItem.alpha;
            if (al < 100) {
                int a = Math.max(0, Math.min(255, al * 255 / 100));
                tint = (a << 24) | (tint & 0xFFFFFF);
                TextureAtlasSprite sp = args.get(5);
                args.set(2, RenderTypes.entityTranslucent(sp.atlasLocation()));
                if (xletxzx$logs++ < 3) System.out.println("[XLetxzX] shield opacity applied, alpha=" + al);
            }
            args.set(8, tint);
        } catch (Throwable ex) {
            if (xletxzx$logs++ < 3) System.out.println("[XLetxzX] shield opacity error: " + ex);
        }
    }
}
