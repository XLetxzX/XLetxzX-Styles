package de.xletxzx.styles.mixin;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import de.xletxzx.styles.Config;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item Physics: dropped items lie flat instead of spinning and bobbing. */
@Mixin(targets = "net.minecraft.class_916")
public abstract class ItemPhysMixin {
    @Inject(method = "method_3996", at = @At("HEAD"), require = 0)
    private void xletxzx$head(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector nodes, CameraRenderState cam, CallbackInfo ci) {
        Config.Phys p = Config.get().phys;
        if (!p.on) return;
        pose.pushPose();
        if (p.flat) {
            state.ageInTicks = 0f;
            pose.translate(0f, 0.12f, 0f);
            pose.mulPose(Axis.XP.rotationDegrees(90f));
        }
    }

    @Inject(method = "method_3996", at = @At("RETURN"), require = 0)
    private void xletxzx$tail(ItemEntityRenderState state, PoseStack pose, SubmitNodeCollector nodes, CameraRenderState cam, CallbackInfo ci) {
        if (Config.get().phys.on) pose.popPose();
    }
}
