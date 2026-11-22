package com.terraformersmc.modmenu.api;

/** Compile-only copy of the Mod Menu API (not packed into the jar). */
public interface ModMenuApi {
    default ConfigScreenFactory<?> getModConfigScreenFactory() { return screen -> null; }
}
