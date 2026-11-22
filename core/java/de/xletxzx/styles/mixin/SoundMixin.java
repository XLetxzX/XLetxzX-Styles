package de.xletxzx.styles.mixin;

import de.xletxzx.styles.Cpvp;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** CPvP: optionally mutes totem and explosion sounds. */
@Mixin(SoundEngine.class)
public abstract class SoundMixin {
    @Inject(method = "method_4854", at = @At("HEAD"), cancellable = true, require = 0)
    private void xletxzx$mute(SoundInstance s, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        try {
            if (Cpvp.blockSound(s.getIdentifier().getPath(), s.getX(), s.getY(), s.getZ())) cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        } catch (Throwable ignored) {}
    }
}
