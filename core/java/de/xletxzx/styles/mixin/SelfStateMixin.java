package de.xletxzx.styles.mixin;

import de.xletxzx.styles.Bridge;
import de.xletxzx.styles.SelfArmor;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import de.xletxzx.styles.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks the render state of the local player after it was filled. */
@Mixin(LivingEntityRenderer.class)
public abstract class SelfStateMixin {
    @Inject(method = "method_62355", at = @At("TAIL"))
    private void xletxzx$mark(LivingEntity entity, LivingEntityRenderState state, float partial, CallbackInfo ci) {
        boolean self = entity == Minecraft.getInstance().player;
        SelfArmor.mark(state, self);
        if (self && Bridge.menuPreview) {
            // skin preview in the XLetxzX menu: always just the plain skin
            state.displayFireAnimation = false;
            state.nameTag = null;
            state.outlineColor = 0;
            state.isInvisible = false;
            state.isInvisibleToPlayer = false;
            state.hasRedOverlay = false;
            state.deathTime = 0;
            state.isFullyFrozen = false;
            if (state instanceof AvatarRenderState a) {
                a.arrowCount = 0;
                a.stingerCount = 0;
                a.scoreText = null;
            }
            return;
        }
        if (self) {
            Config.Nick n = Config.get().nick;
            if (n.on && n.name != null && !n.name.isBlank() && !Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                state.nameTag = Component.literal(n.name);
                state.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partial));
            }
        }
    }
}
