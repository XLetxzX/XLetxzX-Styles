package de.xletxzx.styles;

import net.minecraft.client.Minecraft;

/** Server TPS: exact in singleplayer, an estimate from the world clock on servers. */
final class Tps {
    static double value = 20.0;
    private static long winStartMs = 0, winStartTime = -1;

    private Tps() {}

    static void tick(Minecraft mc) {
        if (mc.level == null) { winStartTime = -1; return; }
        if (mc.getSingleplayerServer() != null) {
            double ms = mc.getSingleplayerServer().getAverageTickTimeNanos() / 1_000_000.0;
            value = Math.min(20.0, 1000.0 / Math.max(50.0, ms));
            winStartTime = -1;
            return;
        }
        long now = System.currentTimeMillis();
        long gt = mc.level.getGameTime();
        if (winStartTime < 0) { winStartTime = gt; winStartMs = now; return; }
        if (now - winStartMs >= 5000) {
            double secs = (now - winStartMs) / 1000.0;
            value = Math.max(0.0, Math.min(20.0, (gt - winStartTime) / secs));
            winStartTime = gt;
            winStartMs = now;
        }
    }
}
