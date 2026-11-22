package de.xletxzx.styles.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import de.xletxzx.styles.Config;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opacity editor: pumpkin overlay and hotbar. */
@Mixin(Gui.class)
public abstract class GuiOpacityMixin {
    private static boolean xletxzx$pumpkin;

    @Inject(method = "method_31977", at = @At("HEAD"), require = 0)
    private void xletxzx$noteOverlay(GuiGraphics g, Identifier id, float a, CallbackInfo ci) {
        xletxzx$pumpkin = id != null && id.getPath().contains("pumpkinblur");
    }

    @ModifyVariable(method = "method_31977", at = @At("HEAD"), argsOnly = true, require = 0)
    private float xletxzx$overlayAlpha(float a) {
        return xletxzx$pumpkin ? a * Config.get().opac.pumpkin / 100f : a;
    }

    @Redirect(method = "method_1759", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/class_332;method_52706(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/class_2960;IIII)V"), require = 0)
    private void xletxzx$hotbar(GuiGraphics g, RenderPipeline p, Identifier id, int x, int y, int w, int h) {
        int o = Config.get().opac.hotbar;
        if (o >= 100) g.blitSprite(p, id, x, y, w, h);
        else if (o > 0) g.blitSprite(p, id, x, y, w, h, ((int) (o * 2.55f) << 24) | 0xFFFFFF);
    }
}
