package de.xletxzx.styles;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;

/** Session statistics: kills, deaths and totem pops. */
final class Stats {
    private Stats() {}

    static int kills, deaths, pops;

    private static boolean wasDead;
    private static LivingEntity target;
    private static boolean counted;
    private static int lastTotems = -1;

    static void tick(Minecraft mc) {
        if (mc.player == null) { lastTotems = -1; return; }
        boolean dead = mc.player.getHealth() <= 0f;
        if (dead && !wasDead) deaths++;
        wasDead = dead;

        LivingEntity t = mc.player.getLastHurtMob();
        if (t != target) { target = t; counted = false; }
        if (t != null && !counted && t instanceof Player && t.isDeadOrDying()
            && mc.player.tickCount - mc.player.getLastHurtMobTimestamp() < 200) {
            kills++;
            counted = true;
        }

        int n = 0;
        int size = mc.player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack s = mc.player.getInventory().getItem(i);
            if (s.is(Items.TOTEM_OF_UNDYING)) n += s.getCount();
        }
        if (lastTotems >= 0 && n < lastTotems && mc.screen == null && !dead) pops += lastTotems - n;
        lastTotems = n;
    }

    static String kd() {
        return String.format("%.2f", deaths == 0 ? (double) kills : (double) kills / deaths).replace(',', '.');
    }
}
