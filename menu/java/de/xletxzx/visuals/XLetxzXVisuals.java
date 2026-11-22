package de.xletxzx.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** XLetxzX menu: open with the "Open XLetxzX Menu" key (Right Shift, changeable in Controls). */
public class XLetxzXVisuals implements ClientModInitializer {
    public static final String MOD_ID = "xletxzx";
    private static KeyBinding openKey;
    private static boolean prevTitleKey = true;

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
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.xletxzx.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, KeyBinding.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            int code = KeyBindingHelper.getBoundKeyOf(openKey).getCode();
            Core.call("setMenuKey", code);
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
}
