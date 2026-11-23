package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.Comparator;

/** AimAssist (подтягивание движения мыши к цели) и TriggerBot (удар при наведении). Без рандомизации и обхода античита. */
public final class Aim {
    private static LivingEntity target;

    private Aim() { }

    private static boolean valid(MinecraftClient mc, LivingEntity e) {
        if (e == mc.player || e.isDead() || !e.isAlive() || e instanceof ArmorStandEntity) return false;
        if (e instanceof PlayerEntity p) {
            if (p.isSpectator() || Cfg.D.friends.contains(p.getName().getString())) return false;
            return Cfg.D.aimPlayers;
        }
        return Cfg.D.aimMobs;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) { target = null; return; }
        Cfg.Data d = Cfg.D;
        if (d.aimOn) {
            double range = d.aimRange / 10.0, rsq = range * range;
            if (target == null || !valid(mc, target) || mc.player.squaredDistanceTo(target) > rsq) {
                target = mc.world.getEntitiesByClass(LivingEntity.class, mc.player.getBoundingBox().expand(range), e -> valid(mc, e))
                        .stream().filter(e -> mc.player.squaredDistanceTo(e) <= rsq && mc.player.canSee(e))
                        .min(Comparator.comparingDouble(e -> fovTo(mc, e.getBoundingBox().getCenter()))).orElse(null);
            }
        } else target = null;

        if (d.trigOn) trigger(mc);
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

    private static void trigger(MinecraftClient mc) {
        Cfg.Data d = Cfg.D;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit) || hit.getType() != HitResult.Type.ENTITY) return;
        Entity e = hit.getEntity();
        if (!(e instanceof LivingEntity le) || !valid2(mc, le)) return;
        if (mc.player.squaredDistanceTo(e) > (d.trigRange / 10.0) * (d.trigRange / 10.0)) return;
        if (mc.player.getAttackCooldownProgress(0.5f) < 0.92f) return;
        if (d.trigCrit && !(mc.player.fallDistance > 0 && !mc.player.isOnGround() && !mc.player.isClimbing() && !mc.player.isTouchingWater())) return;
        mc.interactionManager.attackEntity(mc.player, e);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
    }

    private static boolean valid2(MinecraftClient mc, LivingEntity e) {
        if (e == mc.player || e.isDead() || !e.isAlive() || e instanceof ArmorStandEntity) return false;
        if (e instanceof PlayerEntity p) return !p.isSpectator() && !Cfg.D.friends.contains(p.getName().getString()) && Cfg.D.trigPlayers;
        return Cfg.D.trigMobs;
    }

    /** Вызывается из миксина: корректирует горизонтальное/вертикальное смещение мыши. Возвращает {dx, dy}. */
    public static double[] adjust(double dx, double dy) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Cfg.D.aimOn || target == null || mc.player == null || mc.currentScreen != null) return new double[]{dx, dy};
        float[] a = angles(mc.player.getEyePos(), target.getBoundingBox().getCenter());
        float yawDiff = MathHelper.wrapDegrees(a[0] - mc.player.getYaw());
        float pitchDiff = a[1] - mc.player.getPitch();
        if (Math.hypot(yawDiff, pitchDiff) > Cfg.D.aimFov) return new double[]{dx, dy};
        float k = Cfg.D.aimStrength / 100f;
        // движение к цели усиливается, от цели ослабляется; сам вектор движения не создаётся — только когда игрок двигает мышь
        if (dx != 0) dx *= (dx * yawDiff > 0) ? 1 + 0.8 * k : 1 - 0.5 * k;
        if (dy != 0 && Cfg.D.aimVert) dy *= (dy * pitchDiff > 0) ? 1 + 0.5 * k : 1 - 0.3 * k;
        return new double[]{dx, dy};
    }
}
