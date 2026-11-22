package de.xletxzx.styles.mixin;

import de.xletxzx.styles.ThirdItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Item sizes in the hands of players seen from outside (third person, other players). */
@Mixin(ItemInHandLayer.class)
public abstract class ThirdItemMixin {
    @Inject(method = "method_4192", at = @At("HEAD"), require = 0)
    private void xletxzx$head(ArmedEntityRenderState s, ItemStackRenderState r, ItemStack st, HumanoidArm a, PoseStack p, SubmitNodeCollector n, int l, CallbackInfo ci) {
        ThirdItem.scale = ThirdItem.of(st);
        ThirdItem.alpha = ThirdItem.alphaOf(st);
    }

    @Inject(method = "method_4192", at = @At("RETURN"), require = 0)
    private void xletxzx$tail(ArmedEntityRenderState s, ItemStackRenderState r, ItemStack st, HumanoidArm a, PoseStack p, SubmitNodeCollector n, int l, CallbackInfo ci) {
        ThirdItem.scale = 1f;
        ThirdItem.alpha = 100;
    }
}
