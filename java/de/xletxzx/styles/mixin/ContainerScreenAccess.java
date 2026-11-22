package de.xletxzx.styles.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gives access to the position of an inventory window. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
    @Accessor("field_2776")
    int xletxzx$left();

    @Accessor("field_2800")
    int xletxzx$top();
}
