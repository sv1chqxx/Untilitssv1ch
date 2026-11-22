package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.component.DataComponentTypes;

/** Ambience (время/погода), Cooldowns (зелье лечения), Kill Effects. */
public final class VisualFx {
    public static final String[] WEATHER = {"Как есть", "Ясно", "Дождь", "Гроза"};
    public static final String[] KILL = {"Молния", "Частицы", "Молния + частицы", "Астрал"};
    private static final long HEAL_MS = 18500;
    private static long healStart;
    private static ItemStack lastUsing = ItemStack.EMPTY;
    private static int lastUseTicks;
    private static LivingEntity watched;
    private static boolean fired;
    private static double kx, ky, kz;
    private static long kAt;
    private static long thunderAt;

    private VisualFx() { }

    public static void tick(MinecraftClient mc) {
        Cfg.Data d = Cfg.D;
        if (mc.world == null || mc.player == null) { watched = null; return; }

        if (d.ambOn) {
            if (d.ambWeather > 0) {
                mc.world.setRainGradient(d.ambWeather >= 2 ? 1f : 0f);
                mc.world.setThunderGradient(d.ambWeather >= 3 ? 1f : 0f);
            }
            if (d.ambTimeLock) mc.world.getLevelProperties().setTimeOfDay(d.ambTime);
        }

        // Cooldowns: конец питья зелья лечения
        if (d.cdOn) {
            if (mc.player.isUsingItem()) {
                lastUsing = mc.player.getActiveItem().copy();
                lastUseTicks = mc.player.getItemUseTime();
            } else if (!lastUsing.isEmpty()) {
                if (lastUseTicks >= lastUsing.getMaxUseTime(mc.player) - 2 && isHeal(lastUsing)) healStart = System.currentTimeMillis();
                lastUsing = ItemStack.EMPTY;
            }
        }

        // Kill Effects: цель умерла
        if (d.killFx) {
            LivingEntity t = TargetEsp.target;
            if (t != null && t != watched) { watched = t; fired = false; }
            if (watched != null && !fired && (!watched.isAlive() || watched.getHealth() <= 0f)) {
                fired = true;
                boom(mc, watched);
            }
        }
    }

    private static boolean isHeal(ItemStack s) {
        if (!s.isOf(Items.POTION)) return false;
        var pc = s.get(DataComponentTypes.POTION_CONTENTS);
        if (pc == null) return false;
        for (var e : pc.getEffects()) if (e.getEffectType().value() == net.minecraft.entity.effect.StatusEffects.INSTANT_HEALTH.value()) return true;
        return false;
    }

    private static void boom(MinecraftClient mc, LivingEntity e) {
        double x = e.getX(), y = e.getY(), z = e.getZ();
        int m = Cfg.D.killMode;
        if (m == 3) { kx = x; ky = y; kz = z; kAt = System.currentTimeMillis(); }
        Notif.push("Цель убита", true);
        if (m == 0 || m == 2) {
            LightningEntity l = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
            l.setPosition(x, y, z);
            mc.world.addEntity(l);
            mc.world.playSoundClient(x, y, z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 1f, 1f, false);
        }
        if (m == 1 || m == 2) {
            var r = mc.world.random;
            for (int i = 0; i < 40; i++) {
                mc.world.addParticleClient(ParticleTypes.END_ROD, x, y + e.getHeight() * 0.5, z,
                        (r.nextDouble() - .5) * .35, r.nextDouble() * .3, (r.nextDouble() - .5) * .35);
            }
            for (int i = 0; i < 16; i++) {
                mc.world.addParticleClient(ParticleTypes.SOUL, x, y + 0.2, z, (r.nextDouble() - .5) * .1, .1 + r.nextDouble() * .1, (r.nextDouble() - .5) * .1);
            }
        }
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        Cfg.Data d = Cfg.D;
        if (!d.cdOn) return;
        long left = HEAL_MS - (System.currentTimeMillis() - healStart);
        boolean preview = mc.currentScreen != null;
        if (left <= 0 && !preview) return;
        if (left <= 0) left = HEAL_MS / 2;
        float k = d.cdScale / 100f;
        int w = 92, h = 26;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        int x = (int) (sw / 2f - w * k / 2f), y = (int) (sh / 2f + 60);
        var mx = c.getMatrices();
        mx.pushMatrix();
        mx.translate(sw / 2f, y);
        mx.scale(k, k);
        mx.translate(-w / 2f, 0);
        Gfx.glass(c, 0, 0, w, h, 8, true);
        ItemStack st = new ItemStack(Items.SPLASH_POTION);
        c.drawItem(st, 5, 5);
        float frac = Math.max(0f, Math.min(1f, left / (float) HEAL_MS));
        String t = String.format("%.1f c", left / 1000f);
        Gfx.text(c, "Лечение", 26, 5, Gfx.TEXT);
        Gfx.text(c, t, 26, 15, Gfx.DIM);
        Gfx.round(c, 26 + Gfx.tw(t) + 6, 17, Math.max(1, (int) ((w - 34 - Gfx.tw(t)) * frac)), 3, 1, Gfx.withAlpha(Cfg.D.accent, 230));
        mx.popMatrix();
    }

    /** Астрал: расширяющееся кольцо и поднимающиеся огоньки на месте смерти (в проекции). */
    public static void renderWorld(DrawContext c, MinecraftClient mc, float tp) {
        if (!Cfg.D.killFx || Cfg.D.killMode != 3 || kAt == 0) return;
        long age = System.currentTimeMillis() - kAt;
        if (age > 2600) return;
        float f = age / 2600f, a = 1f - f;
        float[] P = TargetEsp.P;
        int col = Gfx.hsv(Cfg.D.tgtHue / 360f, 0.55f, 1f);
        int n = 40;
        double r = 0.4 + f * 2.4;
        float px = 0, py = 0;
        boolean prev = false;
        for (int i = 0; i <= n; i++) {
            double ang = i * Math.PI * 2 / n;
            boolean ok = Projector.project(kx + Math.cos(ang) * r, ky + 0.05 + f * 0.6, kz + Math.sin(ang) * r, P);
            if (ok && prev) TargetEsp.seg(c, px, py, P[0], P[1], Gfx.withAlpha(col, (int) (230 * a)));
            px = P[0]; py = P[1]; prev = ok;
        }
        for (int i = 0; i < 18; i++) {
            double ang = i * 2.399 + f * 2;
            double rr = 0.3 + 0.8 * ((i * 37) % 10) / 10.0;
            double yy = ky + f * (1.5 + (i % 5) * 0.5);
            if (!Projector.project(kx + Math.cos(ang) * rr, yy, kz + Math.sin(ang) * rr, P)) continue;
            TargetEsp.spr(c, "glow", P[0], P[1], 10 + (i % 4) * 4, 0, Gfx.withAlpha(col, (int) (255 * a)));
        }
    }

    /** Дождь-оверлей: капли-штрихи по экрану, при грозе — вспышки. */
    public static void renderRain(DrawContext c, MinecraftClient mc) {
        Cfg.Data d = Cfg.D;
        if (!d.ambRain || mc.world == null) return;
        boolean wet = d.ambOn && d.ambWeather >= 2 || mc.world.isRaining();
        if (!wet) return;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        long t = System.currentTimeMillis();
        int n = (int) (90 * d.rainDensity / 100f);
        var m = c.getMatrices();
        for (int i = 0; i < n; i++) {
            float seed = i * 0.6180339f;
            float sx = (seed % 1f) * (sw + 80) - 40;
            float spd = 0.9f + ((i * 7) % 5) * 0.15f;
            float ph = ((t / 700f) * spd + (i * 0.37f)) % 1f;
            float x = sx - ph * 40f, y = ph * (sh + 60) - 30;
            int len = 8 + (i % 4) * 4;
            m.pushMatrix();
            m.translate(x, y);
            m.rotate(0.35f);
            c.fill(0, 0, 1, len, Gfx.withAlpha(0xBFD8FF, 70 + (i % 3) * 25));
            m.popMatrix();
        }
        boolean thunder = d.ambOn && d.ambWeather >= 3 || mc.world.isThundering();
        if (thunder) {
            if (t - thunderAt > 7000 && Math.random() < 0.004) thunderAt = t;
            long ag = t - thunderAt;
            if (ag < 260) c.fill(0, 0, sw, sh, Gfx.withAlpha(0xFFFFFF, (int) (90 * (1f - ag / 260f))));
        }
    }
}
