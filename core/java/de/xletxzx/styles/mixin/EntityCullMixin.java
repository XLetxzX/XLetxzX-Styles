package de.xletxzx.styles.mixin;

import de.xletxzx.styles.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FPS boost: do not render entities that are further away than the configured distance. */
@Mixin(EntityRenderer.class)
public abstract class EntityCullMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void xletxzx$cull(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        Config.Perf p = Config.get().perf;
        if (!p.cullEntities) return;
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player || entity == mc.getCameraEntity()) return;
        double d = p.cullDist;
        if (entity.distanceToSqr(x, y, z) > d * d) cir.setReturnValue(false);
    }
}
