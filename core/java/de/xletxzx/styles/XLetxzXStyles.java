/*
 * XLetxzX Styles - All Rights Reserved. Copyright (c) 2026 XLetxzX.
 * YOU MAY NOT COPY, REDISTRIBUTE, RE-UPLOAD, SELL OR GIVE AWAY THIS MOD OR ITS SOURCE CODE,
 * IN WHOLE OR IN PART. See LICENSE.txt.
 */
package de.xletxzx.styles;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Random;

/** XLetxzX Styles - entry point. */
public class XLetxzXStyles implements ClientModInitializer {
    static final long START = System.currentTimeMillis();
    static final String[] KILL_TYPES = {"Lightning", "Totem Burst", "Flames", "Souls", "Hearts", "Fireworks"};
    static volatile double speed;
    static volatile int combo;

    private static boolean prevL, prevR;
    private static double px, pz;
    private static boolean hasPos;
    private static final ArrayDeque<Long> clicks = new ArrayDeque<>();
    private static final ArrayDeque<Long> clicksL = new ArrayDeque<>(), clicksR = new ArrayDeque<>();

    private static boolean zooming, fbApplied;
    private static int savedFov = 70;
    private static int prevHurt;
    private static LivingEntity lastTarget;
    private static long lastHit;
    private static final Random RNG = new Random();
    private static boolean englishDone;

    /** True while the configured menu key is held. */
    static boolean menuKeyDown(long w) {
        int k = Bridge.menuKey;
        return w != 0 && k > 0 && GLFW.glfwGetKey(w, k) == GLFW.GLFW_PRESS;
    }

    /** Switches the game language to English (en_us) if it is not already. */
    static void forceEnglish(Minecraft mc) { setLang(mc, "en_us"); }

    /** Switches the game language (e.g. de_de) if it is not already selected. */
    static void setLang(Minecraft mc, String code) {
        try {
            if (code.equals(mc.options.languageCode)) return;
            mc.options.languageCode = code;
            mc.getLanguageManager().setSelected(code);
            mc.options.save();
            mc.reloadResourcePacks();
        } catch (Exception e) {
            System.err.println("[XLetxzX] Could not switch language: " + e);
        }
    }

    @Override
    public void onInitializeClient() {
        Config.get();
        Tweaks.markApplied();
        PackManager.install();
        PacksPanel.rescanPacks();   // find every resource pack right at game start (in the background)
        DiscordRpc.setEnabled(Config.get().fx.discord);

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide() && entity instanceof LivingEntity le) {
                lastTarget = le;
                lastHit = System.currentTimeMillis();
                combo++;
            }
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(XLetxzXStyles::tick);
        HudRenderCallback.EVENT.register((g, delta) -> HudRenderer.render(g));
        Crosshair.register();
        ItemHighlighter.register();
        ChatMove.register();
        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.BEFORE_BLOCK_OUTLINE
            .register((ctx, state) -> BlockHighlight.render(ctx, state));
        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.AFTER_ENTITIES
            .register(ctx -> { try { PopChams.render(ctx); } catch (Throwable ignored) {} });
    }

    private static void tick(Minecraft mc) {
        Tps.tick(mc);
        HitColor.tick(mc);
        Stats.tick(mc);
        PostFx.tick(mc);
        Cpvp.tick(mc);
        Glow.tick(mc);
        long w = GLFW.glfwGetCurrentContext();
        if (w == 0) return;
        Config c = Config.get();
        long now = System.currentTimeMillis();
        if (!startDone) { startDone = true; try { Presets.startup(); } catch (Throwable ignored) {} }
        if (!c.packMerged && mc.level != null) { c.packMerged = true; Config.save(); Tweaks.rebuild(true); }
        if (c.legacyTweaks && mc.level != null) { c.legacyTweaks = false; if (c.fx != null) { Tweaks.apply(); } Config.save(); }
        if (!autoDone && mc.level != null && c.perf.autoApply) { autoDone = true; applyFpsBoost(mc); }
        if (c.fx.noBob && mc.options.bobView().get()) mc.options.bobView().set(false);
        if (c.fx.noSprintFov && mc.options.fovEffectScale().get() > 0.0) mc.options.fovEffectScale().set(0.0);

        // CPS
        boolean l = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(w, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if ((l && !prevL) || (r && !prevR)) clicks.addLast(now);
        if (l && !prevL) clicksL.addLast(now);
        if (r && !prevR) clicksR.addLast(now);
        prevL = l; prevR = r;
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.pollFirst();
        while (!clicksL.isEmpty() && now - clicksL.peekFirst() > 1000) clicksL.pollFirst();
        while (!clicksR.isEmpty() && now - clicksR.peekFirst() > 1000) clicksR.pollFirst();

        // Speed
        if (mc.player != null) {
            double x = mc.player.getX(), z = mc.player.getZ();
            if (hasPos) speed = speed * 0.7 + Math.hypot(x - px, z - pz) * 20.0 * 0.3;
            px = x; pz = z; hasPos = true;
            if (mc.player.hurtTime > 0 && prevHurt == 0) combo = 0;
            prevHurt = mc.player.hurtTime;
        } else {
            hasPos = false;
        }

        // English everywhere (once, after loading has finished)
        if (!englishDone && c.fx.forceEnglish && mc.getOverlay() == null
                && (mc.screen instanceof TitleScreen || mc.player != null)) {
            englishDone = true;
            forceEnglish(mc);
        }

        zoom(mc, w, c);
        fullbright(mc, c);
        killWatch(mc, c, now);

        // Spotify / lyrics
        Config.Hud lyr = c.hud.get("lyrics");
        boolean lyricsWanted = lyr.on;
        boolean media = c.hud.get("spotify").on || lyricsWanted;
        SpotifyService.setWanted(media);
        Lyrics.tick(lyricsWanted ? SpotifyService.get() : null);
        LyricsOverlay.update(false);
    }

    // ---------------------------------------------------------------- zoom
    private static void zoom(Minecraft mc, long w, Config c) {
        // the key itself is a normal Minecraft key binding (Controls > Key Binds > XLetxzX Styles)
        boolean z = c.fx.zoom && mc.screen == null && Bridge.zoomDown;
        if (z && !zooming) { savedFov = mc.options.fov().get(); zooming = true; }
        if (z) mc.options.fov().set(Math.max(30, Math.min(110, c.fx.zoomFov)));
        if (!z && zooming) { mc.options.fov().set(savedFov); zooming = false; }
    }

    // ---------------------------------------------------------------- fullbright
    private static void fullbright(Minecraft mc, Config c) {
        if (mc.player == null) { fbApplied = false; return; }
        if (c.fx.fullbright) {
            if (!mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false, false));
                fbApplied = true;
            }
        } else if (fbApplied) {
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
            fbApplied = false;
        }
    }

    // ---------------------------------------------------------------- kill animation
    private static void killWatch(Minecraft mc, Config c, long now) {
        if (lastTarget == null) return;
        if (now - lastHit > 5000) { lastTarget = null; return; }
        if (lastTarget.isDeadOrDying()) {
            if (c.fx.killAnim) playKill(mc, lastTarget, c.fx.killType, c.fx.killSound);
            lastTarget = null;
        }
    }

    /** Plays the selected kill effect at an entity (also used for the menu preview). */
    static void playKill(Minecraft mc, Entity e, int type, boolean sound) {
        if (mc.level == null) return;
        double x = e.getX(), y = e.getY() + e.getBbHeight() / 2.0, z = e.getZ();
        int n = 28;
        for (int i = 0; i < n; i++) {
            double dx = (RNG.nextDouble() - 0.5) * 0.5, dy = RNG.nextDouble() * 0.45, dz = (RNG.nextDouble() - 0.5) * 0.5;
            double ox = (RNG.nextDouble() - 0.5) * 0.8, oy = (RNG.nextDouble() - 0.5) * e.getBbHeight(), oz = (RNG.nextDouble() - 0.5) * 0.8;
            switch (type) {
                case 1: mc.level.addParticle(ParticleTypes.TOTEM_OF_UNDYING, x + ox, y + oy, z + oz, dx, dy + 0.2, dz); break;
                case 2: mc.level.addParticle(ParticleTypes.FLAME, x + ox, y + oy, z + oz, dx * 0.4, dy * 0.6, dz * 0.4); break;
                case 3: mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + ox, y + oy, z + oz, dx * 0.2, dy + 0.1, dz * 0.2); break;
                case 4: mc.level.addParticle(ParticleTypes.HEART, x + ox, y + oy + 0.3, z + oz, 0, 0.05, 0); break;
                case 5: mc.level.addParticle(ParticleTypes.FIREWORK, x + ox, y + oy, z + oz, dx, dy, dz); break;
                default: mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + ox, y + oy, z + oz, dx * 2, dy * 2, dz * 2); break;
            }
        }
        if (sound) {
            if (type == 0 || type == 2) mc.level.playLocalSound(x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6f, 1.2f, false);
            else if (type == 1) mc.level.playLocalSound(x, y, z, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6f, 1.0f, false);
            else mc.level.playLocalSound(x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.0f, false);
        }
    }

    // ---------------------------------------------------------------- performance
    private static boolean autoDone;

    private static boolean startDone;

    /**
     * Max FPS: strongest settings without touching render / simulation distance. Also switches off the effects
     * of this mod that cost the most (full screen motion blur / saturation shader, glow outlines).
     */
    static void applyMaxFps(Minecraft mc) {
        Config c = Config.get();
        Config.Perf p = c.perf;
        p.keepDistances = true;
        p.vsyncOff = true; p.unlimitedFps = true; p.shadowsOff = true;
        p.particlesMin = true; p.cloudsOff = true; p.aoOff = true; p.fastLeaves = true;
        p.noImprovedTrans = true; p.noFade = true; p.noMenuBlur = true;
        p.biomeBlend = 0; p.mipmap = 0; p.weather = 3;
        p.entityDistance = Math.min(p.entityDistance, 75);
        p.noAnim = true;
        p.cullEntities = true; p.cullDist = Math.max(48, Math.min(p.cullDist, 64));
        c.post.blurOn = false; c.post.satOn = false;
        c.glow.outline = false;
        Config.save();
        applyFpsBoost(mc);
    }

    static void applyFpsBoost(Minecraft mc) {
        Config.Perf p = Config.get().perf;
        if (p.particlesMin) mc.options.particles().set(net.minecraft.server.level.ParticleStatus.MINIMAL);
        if (p.cloudsOff) mc.options.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
        if (p.aoOff) mc.options.ambientOcclusion().set(false);
        if (p.fastLeaves) mc.options.cutoutLeaves().set(false);
        if (p.noImprovedTrans) mc.options.improvedTransparency().set(false);
        if (p.noFade) mc.options.chunkSectionFadeInTime().set(0.0);
        if (p.noMenuBlur) mc.options.menuBackgroundBlurriness().set(0);
        mc.options.mipmapLevels().set(p.mipmap);
        mc.options.weatherRadius().set(p.weather);
        if (!p.keepDistances) {
            mc.options.renderDistance().set(p.renderDistance);
            mc.options.simulationDistance().set(p.simDistance);
        }
        if (p.vsyncOff) mc.options.enableVsync().set(false);
        if (p.unlimitedFps) mc.options.framerateLimit().set(260);
        mc.options.entityShadows().set(!p.shadowsOff);
        mc.options.biomeBlendRadius().set(p.biomeBlend);
        mc.options.entityDistanceScaling().set(p.entityDistance / 100.0);
        mc.options.save();
    }

    static int cps() { return clicks.size(); }
    static int cpsL() { return clicksL.size(); }
    static int cpsR() { return clicksR.size(); }

    static int ping() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() == null || mc.player == null) return 0;
            net.minecraft.client.multiplayer.PlayerInfo pi = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            return pi == null ? 0 : pi.getLatency();
        } catch (Exception e) { return 0; }
    }
}
