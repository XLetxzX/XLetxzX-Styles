package de.xletxzx.styles.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.xletxzx.styles.Config;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low Fire: lowers only the fire overlay of your own screen (world and enemy fire stay untouched). Also the fire and underwater opacity. */
@Mixin(ScreenEffectRenderer.class)
public abstract class FireMixin {
    @Inject(method = "method_23070", at = @At("HEAD"))
    private static void xletxzx$fireDown(PoseStack ps, MultiBufferSource buf, TextureAtlasSprite sprite, CallbackInfo ci) {
        Config.Tweaks t = Config.get().tweaks;
        if (!t.lowFire) return;
        ps.pushPose();
        ps.translate(0.0, -t.fireDrop / 16.0, 0.0);
    }

    /** Opacity editor: fire overlay strength. */
    @ModifyConstant(method = "method_23070", constant = @Constant(floatValue = 0.9f), require = 0)
    private static float xletxzx$fireAlpha(float v) {
        return v * Config.get().opac.fire / 100f;
    }

    /** Opacity editor: dark overlay while your head is under water. */
    @ModifyConstant(method = "method_23069", constant = @Constant(floatValue = 0.1f), require = 0)
    private static float xletxzx$waterAlpha(float v) {
        return v * Config.get().opac.under / 100f;
    }

    @Inject(method = "method_23070", at = @At("RETURN"))
    private static void xletxzx$fireUp(PoseStack ps, MultiBufferSource buf, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (Config.get().tweaks.lowFire) ps.popPose();
    }
}
