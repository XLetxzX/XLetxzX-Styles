package com.terraformersmc.modmenu.api;

import net.minecraft.client.gui.screen.Screen;

/** Compile-only copy of the Mod Menu API (not packed into the jar). */
@FunctionalInterface
public interface ConfigScreenFactory<S extends Screen> {
    S create(Screen parent);
}
