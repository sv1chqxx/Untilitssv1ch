package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.Random;

/** AimAssist (помогает при движении мыши) и TriggerBot (удар при наведении, с рандомной задержкой). */
public final class Aim {
    private static LivingEntity target;
    private static float lastYaw, lastPitch;
    private static boolean haveLast;
    private static int wait = -1;
    private static long lastAttack;
    private static float need = 0.95f;
    private static final Random R = new Random();

    private Aim() { }

    private static boolean ok(MinecraftClient mc, LivingEntity e, boolean players, boolean mobs) {
        if (e == mc.player || e.isDead() || !e.isAlive() || e instanceof ArmorStandEntity) return false;
        if (e instanceof PlayerEntity p) return !p.isSpectator() && !Cfg.D.friends.contains(p.getName().getString()) && players;
        return mobs;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) { target = null; wait = -1; return; }
        Cfg.Data d = Cfg.D;
        if (d.aimOn) {
            double rsq = (double) d.aimDist * d.aimDist;
            if (target == null || !ok(mc, target, d.aimPlayers, d.aimMobs) || mc.player.squaredDistanceTo(target) > rsq) {
                target = mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(d.aimDist), e -> ok(mc, e, d.aimPlayers, d.aimMobs))
                        .stream().filter(e -> mc.player.squaredDistanceTo(e) <= rsq && mc.player.canSee(e))
                        .min(Comparator.comparingDouble(e -> fovTo(mc, e.getBoundingBox().getCenter()))).orElse(null);
            }
        } else target = null;
        if (d.trigOn) trigger(mc); else wait = -1;
    }

    private static double fovTo(MinecraftClient mc, Vec3d p) {
        float[] a = angles(mc.player.getEyePos(), p);
        return Math.hypot(MathHelper.wrapDegrees(a[0] - mc.player.getYaw()), a[1] - mc.player.getPitch());
    }

    private static float[] angles(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        return new float[]{(float) Math.toDegrees(Math.atan2(-dx, dz)), (float) Math.toDegrees(-Math.atan2(dy, h))};
    }

    /** Каждый кадр: то, на сколько игрок повернул мышь, усиливается в сторону цели. Сам аим цель не наводит. */
    public static void frame(MinecraftClient mc) {
        if (mc.player == null) { haveLast = false; return; }
        float yaw = mc.player.getYaw(), pitch = mc.player.getPitch();
        Cfg.Data d = Cfg.D;
        if (d.aimOn && target != null && haveLast && mc.currentScreen == null && !Freecam.active) {
            float dy = MathHelper.wrapDegrees(yaw - lastYaw), dp = pitch - lastPitch;
            if (Math.abs(dy) > 0.02f || Math.abs(dp) > 0.02f) {
                float[] a = angles(mc.player.getEyePos(), target.getBoundingBox().getCenter());
                float yd = MathHelper.wrapDegrees(a[0] - yaw), pd = a[1] - pitch;
                if (Math.hypot(yd, pd) <= d.aimFov) {
                    float k = d.aimStrength / 100f;
                    float ey = (dy * yd > 0) ? dy * 0.9f * k : -dy * 0.35f * k;
                    ey = Math.signum(yd) * Math.min(Math.abs(ey), Math.abs(yd));
                    if (dy * yd <= 0) ey = -dy * 0.35f * k;
                    mc.player.setYaw(yaw + ey);
                    if (d.aimVert) {
                        float ep = (dp * pd > 0) ? dp * 0.6f * k : -dp * 0.2f * k;
                        if (dp * pd > 0) ep = Math.signum(pd) * Math.min(Math.abs(ep), Math.abs(pd));
                        mc.player.setPitch(MathHelper.clamp(pitch + ep, -90f, 90f));
                    }
                }
            }
        }
        lastYaw = mc.player.getYaw();
        lastPitch = mc.player.getPitch();
        haveLast = true;
    }

    private static void trigger(MinecraftClient mc) {
        Cfg.Data d = Cfg.D;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit) || hit.getType() != HitResult.Type.ENTITY) { wait = -1; return; }
        Entity e = hit.getEntity();
        if (!(e instanceof LivingEntity le) || !ok(mc, le, d.trigPlayers, d.trigMobs)) { wait = -1; return; }
        if (mc.player.squaredDistanceTo(e) > (double) d.trigDist * d.trigDist) { wait = -1; return; }
        if (d.trigCrit && !(mc.player.fallDistance > 0 && !mc.player.isOnGround() && !mc.player.isClimbing() && !mc.player.isTouchingWater())) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < (d.trigRand ? need : 0.92f)) return;
        if (d.trigTps) {
            float tps = Math.max(5f, Math.min(20f, Tps.get()));
            double periodMs = mc.player.getAttackCooldownProgressPerTick() * 50.0 * (20.0 / tps);
            if (System.currentTimeMillis() - lastAttack < periodMs * 0.95) return;
        }
        if (d.trigRand) {
            if (wait < 0) wait = R.nextInt(2);       // 0–1 тик случайной задержки
            if (wait-- > 0) return;
            need = 0.9f + R.nextFloat() * 0.1f;
        }
        wait = -1;
        lastAttack = System.currentTimeMillis();
        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(Hand.MAIN_HAND);
    }
}
