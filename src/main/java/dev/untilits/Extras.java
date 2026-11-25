package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.EntityHitResult;

/** Мелкие визуалы: Fullbright, BPS, часы, дистанция до цели, счётчик комбо. */
public final class Extras {
    private static boolean fb;
    private static int combo;
    private static long lastHit;
    private static double lx, lz, bps;
    private static long lt;

    private Extras() { }

    public static void hit() { long n = System.currentTimeMillis(); combo = n - lastHit > 3000 ? 1 : combo + 1; lastHit = n; }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null) { fb = false; return; }
        if (Cfg.D.fullbright) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
            fb = true;
        } else if (fb) { mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION); fb = false; }
        long n = System.currentTimeMillis();
        if (n - lt >= 250) {
            double dx = mc.player.getX() - lx, dz = mc.player.getZ() - lz;
            bps = Math.sqrt(dx * dx + dz * dz) / ((n - lt) / 1000.0);
            lx = mc.player.getX(); lz = mc.player.getZ(); lt = n;
        }
        if (mc.player.hurtTime > 0) combo = 0;
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        Cfg.Data d = Cfg.D;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        int y = sh - 24;
        if (d.bpsHud) { pill(c, String.format("%.1f бл/с", bps), 8, y); y -= 22; }
        if (d.clockHud) { pill(c, java.time.LocalTime.now().toString().substring(0, 5), 8, y); y -= 22; }
        if (d.distHud && mc.crosshairTarget instanceof EntityHitResult h && mc.player != null) {
            String t = String.format("%.1f", mc.player.distanceTo(h.getEntity()));
            Gfx.shadow(c, t, sw / 2 - Gfx.tw(t) / 2, sh / 2 + 12, 0xFFFFFFFF);
        }
        if (d.comboHud && combo > 1 && System.currentTimeMillis() - lastHit < 3000) {
            String t = "Комбо x" + combo;
            Gfx.shadow(c, t, sw / 2 - Gfx.tw(t) / 2, sh / 2 - 26, Gfx.withAlpha(d.accent, 255));
        }
    }

    private static void pill(DrawContext c, String t, int x, int y) {
        Gfx.glass(c, x, y, Gfx.tw(t) + 16, 18, 8, true);
        Gfx.text(c, t, x + 8, y + 5, Gfx.TEXT);
    }
}
