package de.xletxzx.styles.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.xletxzx.styles.Config;
import de.xletxzx.styles.ThirdItem;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** CPvP: small held items (first person). */
@Mixin(ItemInHandRenderer.class)
public abstract class HandScaleMixin {
    @Inject(method = "method_3233", at = @At("HEAD"), require = 0)
    private void xletxzx$head(LivingEntity e, ItemStack st, ItemDisplayContext ctx, PoseStack pose, SubmitNodeCollector nodes, int light, CallbackInfo ci) {
        pose.pushPose();
        ThirdItem.alpha = ThirdItem.alphaOf(st);
        Config.Cpvp c = Config.get().cpvp;
        float s = 1f;
        if (c.smallHand) s = c.handScale / 100f;
        if (c.itemSizes != null && !c.itemSizes.isEmpty() && st != null) {
            Integer v = c.itemSizes.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()).getPath());
            if (v != null) s = v / 100f;
        }
        if (s != 1f) pose.scale(s, s, s);
    }

    @Inject(method = "method_3233", at = @At("RETURN"), require = 0)
    private void xletxzx$tail(LivingEntity e, ItemStack st, ItemDisplayContext ctx, PoseStack pose, SubmitNodeCollector nodes, int light, CallbackInfo ci) {
        pose.popPose();
        ThirdItem.alpha = 100;
    }
}
