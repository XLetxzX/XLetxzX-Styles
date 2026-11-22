package de.xletxzx.styles.mixin;

import de.xletxzx.styles.Glow;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Glow Items: chosen dropped items get a coloured outline (only on your screen). */
@Mixin(Entity.class)
public abstract class GlowMixin {
    @Inject(method = "method_5851", at = @At("HEAD"), cancellable = true, require = 0)
    private void xletxzx$glow(CallbackInfoReturnable<Boolean> cir) {
        if (Glow.matches(this)) cir.setReturnValue(true);
    }

    @Inject(method = "method_22861", at = @At("HEAD"), cancellable = true, require = 0)
    private void xletxzx$glowColor(CallbackInfoReturnable<Integer> cir) {
        if (Glow.matches(this)) cir.setReturnValue(Glow.color());
    }
}
