package de.xletxzx.styles.mixin;

import de.xletxzx.styles.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opacity Editor: no flames drawn on your own body while you burn. */
@Mixin(EntityRenderer.class)
public abstract class OwnFireMixin {
    @Inject(method = "method_62354", at = @At("RETURN"), require = 0)
    private void xletxzx$noFire(Entity e, EntityRenderState st, float pt, CallbackInfo ci) {
        try {
            if (Config.get().opac.hideOwnFire && e == Minecraft.getInstance().player) st.displayFireAnimation = false;
        } catch (Throwable ignored) {
        }
    }
}
