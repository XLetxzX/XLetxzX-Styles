package de.xletxzx.styles.mixin;

import de.xletxzx.styles.Config;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** CPvP: optionally hides block break particles. */
@Mixin(ClientLevel.class)
public abstract class BlockParticleMixin {
    @Inject(method = "method_31595", at = @At("HEAD"), cancellable = true, require = 0)
    private void xletxzx$noBreak(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (Config.get().cpvp.noBreakParticles) ci.cancel();
    }

    @Inject(method = "method_74254", at = @At("HEAD"), cancellable = true, require = 0)
    private void xletxzx$noCrack(BlockPos pos, Direction dir, CallbackInfo ci) {
        if (Config.get().cpvp.noBreakParticles) ci.cancel();
    }
}
