package de.xletxzx.styles.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Opens the private post-effect setter. */
@Mixin(GameRenderer.class)
public interface GameRendererAccess {
    @Invoker("method_62904")
    void xletxzx$setPost(Identifier id);
}
