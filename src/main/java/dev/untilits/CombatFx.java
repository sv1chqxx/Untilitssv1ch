package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/** Jump Circles, Hit Particles, Hitmarker (+звук), Zoom, FreeLook. */
public final class CombatFx {
    public static final String[] JC_MODES = {"Кольца", "Волны", "Звезда", "Спираль", "Искры", "Радуга"};
    public static final String[] HIT_MODES = {"Криты", "Искры", "Сердца", "Души"};
    public static final String[] SOUNDS = {"Выкл", "Дзынь", "Колокол", "Пинг", "Хруст"};

    private record Circle(double x, double y, double z, long at) { }
    private static final List<Circle> CIRCLES = new ArrayList<>();
    private static boolean wasGround = true;
    private static long hmAt;
    private static final float[] P = new float[3];

    // Zoom / FreeLook
    public static boolean zoomHeld, lookHeld;
    private static float zoomK = 1f;
    public static boolean flActive;
    public static float flYaw, flPitch;

    private CombatFx() { }

    // ---------- события ----------

    public static void onHit(Entity e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || !(e instanceof LivingEntity)) return;
        Cfg.Data d = Cfg.D;
        if (d.hitmarker) hmAt = System.currentTimeMillis();
        if (d.hitmarker || d.hitSound > 0) sound(mc, d.hitSound);
        if (d.hitFx) {
            ParticleEffect pe = switch (d.hitMode) {
                case 0 -> ParticleTypes.CRIT;
                case 1 -> ParticleTypes.ENCHANTED_HIT;
                case 2 -> ParticleTypes.HEART;
                default -> ParticleTypes.SOUL_FIRE_FLAME;
            };
            var r = mc.world.random;
            double x = e.getX(), y = e.getY() + e.getHeight() * 0.6, z = e.getZ();
            for (int i = 0; i < 14; i++)
                mc.world.addParticleClient(pe, x + (r.nextDouble() - .5) * .5, y + (r.nextDouble() - .5) * .4, z + (r.nextDouble() - .5) * .5,
                        (r.nextDouble() - .5) * .4, r.nextDouble() * .3, (r.nextDouble() - .5) * .4);
        }
    }

    private static void sound(MinecraftClient mc, int mode) {
        switch (mode) {
            case 1 -> mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
            case 2 -> mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 0.9f, 1.3f);
            case 3 -> mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.8f, 1.5f);
            case 4 -> mc.player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 0.9f, 1.1f);
            default -> { }
        }
    }

    // ---------- тик ----------

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) { CIRCLES.clear(); flActive = false; return; }
        Cfg.Data d = Cfg.D;
        boolean g = mc.player.isOnGround();
        if (d.jumpCircles && wasGround && !g && mc.player.getVelocity().y > 0.2) {
            CIRCLES.add(new Circle(mc.player.getX(), mc.player.getY() + 0.02, mc.player.getZ(), System.currentTimeMillis()));
            if (CIRCLES.size() > 6) CIRCLES.remove(0);
        }
        wasGround = g;

        // zoom плавно
        float target = d.zoomOn && zoomHeld && mc.currentScreen == null ? d.zoomFactor : 1f;
        zoomK += (target - zoomK) * 0.3f;
        if (Math.abs(zoomK - target) < 0.01f) zoomK = target;

        // freelook
        boolean want = d.flOn && lookHeld && mc.currentScreen == null && !Freecam.active;
        if (want && !flActive) { flYaw = mc.player.getYaw(); flPitch = mc.player.getPitch(); }
        flActive = want;
    }

    public static float zoom() { return zoomK; }

    public static void look(double dx, double dy) {
        flYaw += (float) dx * 0.15f;
        flPitch = MathHelper.clamp(flPitch + (float) dy * 0.15f, -90f, 90f);
    }

    // ---------- рендер ----------

    public static void renderWorld(DrawContext c, MinecraftClient mc) {
        if (!Cfg.D.jumpCircles || CIRCLES.isEmpty()) return;
        long now = System.currentTimeMillis();
        CIRCLES.removeIf(k -> now - k.at > 1000);
        int col = Cfg.D.accent;
        int mode = Cfg.D.jcMode;
        double sc = Cfg.D.jcSize / 100.0;
        for (Circle k : CIRCLES) {
            float f = (now - k.at) / 1000f;
            int a = (int) (230 * (1f - f));
            switch (mode) {
                case 0 -> { ring(c, k, (0.3 + f * 1.8) * sc, 32, Gfx.withAlpha(col, a), 0); ring(c, k, (0.3 + f * 1.8) * 0.7 * sc, 32, Gfx.withAlpha(col, a / 3), 0); }
                case 1 -> { for (int j = 0; j < 3; j++) { float g = f - j * 0.12f; if (g > 0) ring(c, k, (0.2 + g * 2.2) * sc, 32, Gfx.withAlpha(col, (int) (230 * (1f - g))), 0); } }
                case 2 -> { // вращающийся полигон-звезда
                    double r = (0.5 + f * 1.4) * sc;
                    for (int i = 0; i < 5; i++) {
                        double a1 = f * 4 + i * Math.PI * 2 / 5, a2 = f * 4 + ((i + 2) % 5) * Math.PI * 2 / 5;
                        if (Projector.project(k.x + Math.cos(a1) * r, k.y, k.z + Math.sin(a1) * r, P)) {
                            float x1 = P[0], y1 = P[1];
                            if (Projector.project(k.x + Math.cos(a2) * r, k.y, k.z + Math.sin(a2) * r, P)) TargetEsp.seg(c, x1, y1, P[0], P[1], Gfx.withAlpha(col, a));
                        }
                    }
                    ring(c, k, r, 32, Gfx.withAlpha(col, a / 2), 0);
                }
                case 3 -> { // спираль вверх
                    float px = 0, py = 0; boolean prev = false;
                    for (int i = 0; i <= 48; i++) {
                        double ang = i * Math.PI * 2 * 2 / 48 + f * 6, h = i / 48.0 * (0.3 + f * 1.6);
                        boolean ok = Projector.project(k.x + Math.cos(ang) * 0.9 * sc, k.y + h, k.z + Math.sin(ang) * 0.9 * sc, P);
                        if (ok && prev) TargetEsp.seg(c, px, py, P[0], P[1], Gfx.withAlpha(col, a));
                        px = P[0]; py = P[1]; prev = ok;
                    }
                }
                case 4 -> { // искры по кругу
                    double r = (0.4 + f * 1.6) * sc;
                    for (int i = 0; i < 16; i++) {
                        double ang = i * Math.PI * 2 / 16 + f * 2;
                        if (!Projector.project(k.x + Math.cos(ang) * r, k.y + f * 0.4, k.z + Math.sin(ang) * r, P)) continue;
                        float ppb = TargetEsp.pxPerBlock(k.x, k.y, k.z);
                        TargetEsp.spr(c, "glow", P[0], P[1], ppb * 0.35f, 0, Gfx.withAlpha(col, a));
                    }
                }
                default -> { // радужное кольцо
                    ring(c, k, (0.3 + f * 1.8) * sc, 48, Gfx.withAlpha(0xFFFFFF, a), 1);
                }
            }
        }
    }

    private static void ring(DrawContext c, Circle k, double r, int n, int color, int rainbow) {
        float px = 0, py = 0;
        boolean prev = false;
        for (int i = 0; i <= n; i++) {
            double ang = i * Math.PI * 2 / n;
            boolean ok = Projector.project(k.x + Math.cos(ang) * r, k.y, k.z + Math.sin(ang) * r, P);
            int col = color;
            if (rainbow == 1) col = Gfx.withAlpha(Gfx.hsv(i / (float) n, 0.8f, 1f), color >>> 24);
            if (ok && prev) TargetEsp.seg(c, px, py, P[0], P[1], col);
            px = P[0]; py = P[1]; prev = ok;
        }
    }

    public static void renderHud(DrawContext c, MinecraftClient mc) {
        if (!Cfg.D.hitmarker) return;
        long age = System.currentTimeMillis() - hmAt;
        if (age > 320) return;
        float f = age / 320f;
        int a = (int) (255 * (1f - f));
        int cx = c.getScaledWindowWidth() / 2, cy = c.getScaledWindowHeight() / 2;
        int in = 4 + (int) (f * 3), len = 6;
        int col = Gfx.withAlpha(0xFFFFFF, a);
        for (int sx = -1; sx <= 1; sx += 2) for (int sy = -1; sy <= 1; sy += 2) {
            var m = c.getMatrices();
            m.pushMatrix();
            m.translate(cx + sx * in, cy + sy * in);
            m.rotate((float) Math.atan2(sy, sx));
            c.fill(0, -1, len, 1, col);
            m.popMatrix();
        }
    }
}
