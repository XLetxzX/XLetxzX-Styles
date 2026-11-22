package de.xletxzx.styles.mixin;

import de.xletxzx.styles.Cpvp;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** CPvP: optionally hides explosion particles. */
@Mixin(ParticleEngine.class)
public abstract class ParticleMixin {
    @Inject(method = "method_3056", at = @At("HEAD"), cancellable = true, require = 0)
    private void xletxzx$noExplosion(ParticleOptions o, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<Particle> cir) {
        if (Cpvp.blockParticle(o, x, y, z)) cir.setReturnValue(null);
    }
}
