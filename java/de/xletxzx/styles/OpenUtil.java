package de.xletxzx.styles;

import net.minecraft.util.Util;

import java.nio.file.Path;

/** Opens a link or a folder with the game's own helper. */
final class OpenUtil {
    private OpenUtil() {}

    static void open(String target) {
        try {
            if (target.startsWith("http://") || target.startsWith("https://")) Util.getPlatform().openUri(target);
            else Util.getPlatform().openPath(Path.of(target));
        } catch (Exception ignored) {}
    }

    static void open(Path p) {
        try { p.toFile().mkdirs(); } catch (Exception ignored) {}
        try { Util.getPlatform().openPath(p.toAbsolutePath()); } catch (Exception ignored) {}
    }
}
