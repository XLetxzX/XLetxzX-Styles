package de.xletxzx.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * XLetxzX menu: open with the "Open menu" key (Right Shift). All keys of the mod live in their own
 * category "XLetxzX Styles" under Options > Controls > Key Binds and can be changed there.
 */
public class XLetxzXVisuals implements ClientModInitializer {
    public static final String MOD_ID = "xletxzx";
    private static KeyBinding openKey, zoomKey;
    private static boolean prevTitleKey = true;
    private static boolean zoomMigrated;

    /** Human readable name of the current menu key. */
    static String keyName() {
        try { return openKey.getBoundKeyLocalizedText().getString(); } catch (Throwable t) { return "?"; }
    }

    /** Binds the menu key (same as changing it in Controls) and saves the options. */
    static void rebind(int code) {
        try {
            openKey.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(code));
            KeyBinding.updateKeysByCode();
            net.minecraft.client.MinecraftClient.getInstance().options.write();
        } catch (Throwable t) {
            System.err.println("[XLetxzX] rebind failed: " + t);
        }
    }

    @Override
    public void onInitializeClient() {
        Config.load();
        MenuScreen.snapshotMods();
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.xletxzx.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.xletxzx.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, cat));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            int code = KeyBindingHelper.getBoundKeyOf(openKey).getCode();
            Core.call("setMenuKey", code);
            migrateZoomKey(client);
            Core.call("setZoomState", zoomKey.isPressed(), zoomKey.getBoundKeyLocalizedText().getString());
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new MenuScreen(null));
            }
            // also on the title screen
            long w = GLFW.glfwGetCurrentContext();
            boolean down = w != 0 && code > 0 && GLFW.glfwGetKey(w, code) == GLFW.GLFW_PRESS;
            if (down && !prevTitleKey && client.currentScreen instanceof TitleScreen t) client.setScreen(new MenuScreen(t));
            prevTitleKey = down;
        });
    }

    /**
     * Older versions stored the zoom key in the mod config. Once, take that key over into the
     * Minecraft key binding (only if the binding is still on its default).
     */
    private static void migrateZoomKey(net.minecraft.client.MinecraftClient client) {
        if (zoomMigrated) return;
        zoomMigrated = true;
        try {
            int old = Core.num("zoomKey");
            boolean isDefault = zoomKey.isDefault();
            if (isDefault && old > 0 && old != GLFW.GLFW_KEY_C) {
                zoomKey.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(old));
                KeyBinding.updateKeysByCode();
                client.options.write();
            }
        } catch (Throwable t) {
            System.err.println("[XLetxzX] zoom key migration failed: " + t);
        }
    }
}
