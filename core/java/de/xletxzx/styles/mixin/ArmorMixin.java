package de.xletxzx.styles.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.xletxzx.styles.SelfArmor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide armor: skips the armor layer, but only for the local player. */
@Mixin(HumanoidArmorLayer.class)
public abstract class ArmorMixin {
    @Inject(method = "method_17157", at = @At("HEAD"), cancellable = true)
    private void xletxzx$hide(PoseStack ps, SubmitNodeCollector nodes, int light, HumanoidRenderState state, float a, float b, CallbackInfo ci) {
        if (SelfArmor.hide(state)) ci.cancel();
    }
}
