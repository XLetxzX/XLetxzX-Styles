package de.xletxzx.styles.mixin;

import de.xletxzx.styles.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FPS boost: freeze animated textures (water, lava, fire ...) so they are not re-uploaded every tick. */
@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$AnimationState")
public abstract class AnimMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void xletxzx$tick(CallbackInfo ci) {
        if (Config.get().perf.noAnim) ci.cancel();
    }
}
