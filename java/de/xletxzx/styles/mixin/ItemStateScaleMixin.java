package de.xletxzx.styles.mixin;

import de.xletxzx.styles.ThirdItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the third person item size while the held item is submitted. */
@Mixin(ItemStackRenderState.class)
public abstract class ItemStateScaleMixin {
    @Inject(method = "method_65604", at = @At("HEAD"), require = 0)
    private void xletxzx$head(PoseStack pose, SubmitNodeCollector nodes, int light, int overlay, int outline, CallbackInfo ci) {
        pose.pushPose();
        float s = ThirdItem.scale;
        if (s != 1f) pose.scale(s, s, s);
    }

    @Inject(method = "method_65604", at = @At("RETURN"), require = 0)
    private void xletxzx$tail(PoseStack pose, SubmitNodeCollector nodes, int light, int overlay, int outline, CallbackInfo ci) {
        pose.popPose();
    }
}
