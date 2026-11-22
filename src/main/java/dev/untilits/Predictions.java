package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;

/** Predictions: траектория и точка падения жемчугов, снарядов, зелий, стрел и трезубцев (своих в руке и летящих в мире). */
public final class Predictions {
    private static final class Path {
        final List<Vec3d> pts = new ArrayList<>(96);
        Vec3d end;
        boolean hit;
        int ticks, color;
        String label;
    }

    private static final List<Path> FLYING = new ArrayList<>();
    private static final float[] A = new float[3], B = new float[3];
    private static int timer;

    private Predictions() { }

    // ---------- расчёт ----------

    private static Path simulate(MinecraftClient mc, Vec3d pos, Vec3d vel, double grav, double drag, int color, String label, int max) {
        Path p = new Path();
        p.color = color; p.label = label;
        p.pts.add(pos);
        double bottom = mc.world.getBottomY() - 8;
        for (int i = 0; i < max; i++) {
            Vec3d next = pos.add(vel);
            var h = mc.world.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, mc.player));
            if (h.getType() != HitResult.Type.MISS) {
                p.pts.add(h.getPos());
                p.end = h.getPos(); p.hit = true; p.ticks = i + 1;
                return p;
            }
            pos = next;
            p.pts.add(pos);
            vel = vel.multiply(drag).add(0, -grav, 0);
            if (pos.y < bottom) break;
        }
        p.end = pos; p.ticks = max;
        return p;
    }

    /** Летящие в мире снаряды — раз в 2 тика. */
    public static void tick(MinecraftClient mc) {
        if (!Cfg.D.predict || !Cfg.D.predictFlying || mc.world == null || mc.player == null) { FLYING.clear(); return; }
        if (timer-- > 0) return;
        timer = 1;
        FLYING.clear();
        int n = 0;
        for (Entity e : mc.world.getEntities()) {
            if (n >= 12) break;
            double g, drag = 0.99;
            int col; String lbl;
            if (e instanceof EnderPearlEntity) { g = 0.03; col = 0xB78CFF; lbl = "Pearl"; }
            else if (e instanceof ThrownItemEntity) { g = 0.03; col = 0x45D0FF; lbl = "Throw"; }
            else if (e instanceof PersistentProjectileEntity) { g = 0.05; col = 0xFFB454; lbl = "Arrow"; }
            else continue;
            if (e.squaredDistanceTo(mc.player) > 96 * 96) continue;
            Vec3d v = e.getVelocity();
            if (v.lengthSquared() < 0.02) continue;
            FLYING.add(simulate(mc, e.getEntityPos(), v, g, drag, col, lbl, 100));
            n++;
        }
    }

    /** Предмет в руке: параметры запуска. @return {скорость, гравитация, поправка угла, цвет} или null. */
    private static double[] launch(ClientPlayerEntity p, ItemStack s) {
        if (s.isEmpty()) return null;
        if (s.isOf(Items.ENDER_PEARL)) return new double[]{1.5, 0.03, 0, 0xB78CFF};
        if (s.isOf(Items.SNOWBALL) || s.isOf(Items.EGG)) return new double[]{1.5, 0.03, 0, 0x45D0FF};
        if (s.isOf(Items.SPLASH_POTION) || s.isOf(Items.LINGERING_POTION)) return new double[]{0.5, 0.05, -20, 0xFF7AC8};
        if (s.isOf(Items.EXPERIENCE_BOTTLE)) return new double[]{0.7, 0.07, -20, 0x37E0A8};
        if (s.isOf(Items.TRIDENT) && p.isUsingItem() && p.getActiveItem() == s) return new double[]{2.5, 0.05, 0, 0x45D0FF};
        if (s.isOf(Items.BOW) && p.isUsingItem() && p.getActiveItem() == s) {
            float pull = BowItem.getPullProgress(p.getItemUseTime());
            if (pull < 0.1f) return null;
            return new double[]{pull * 3.0, 0.05, 0, 0xFFB454};
        }
        if (s.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(s)) return new double[]{3.15, 0.05, 0, 0xFFB454};
        return null;
    }

    private static Path held(MinecraftClient mc, float tp) {
        ClientPlayerEntity p = mc.player;
        double[] l = launch(p, p.getMainHandStack());
        if (l == null) l = launch(p, p.getOffHandStack());
        if (l == null) return null;
        float yaw = p.getYaw(tp), pitch = p.getPitch(tp);
        Vec3d dir = p.getRotationVector(pitch + (float) l[2], yaw);
        Vec3d pv = p.getVelocity();
        Vec3d vel = dir.multiply(l[0]).add(pv.x, p.isOnGround() ? 0.0 : pv.y, pv.z);
        Vec3d pos = p.getLerpedPos(tp).add(0, p.getEyeHeight(p.getPose()) - 0.1, 0);
        return simulate(mc, pos, vel, l[1], 0.99, (int) l[3], "", 90);
    }

    // ---------- отрисовка ----------

    public static void render(DrawContext c, MinecraftClient mc, float tp) {
        if (!Cfg.D.predict || mc.world == null || mc.player == null) return;
        Path h = held(mc, tp);
        if (h != null) draw(c, h);
        for (int i = 0, n = FLYING.size(); i < n; i++) draw(c, FLYING.get(i));
    }

    private static void draw(DrawContext c, Path p) {
        int base = p.color & 0xFFFFFF;
        int n = p.pts.size();
        boolean prev = false;
        float px = 0, py = 0;
        for (int i = 0; i < n; i += 2) {
            Vec3d v = p.pts.get(i);
            boolean ok = Projector.project(v.x, v.y, v.z, A);
            if (ok && prev) {
                int a = (int) (230 - 140f * i / n);
                Tracers.line(c, px, py, A[0], A[1], (a << 24) | base);
            }
            prev = ok; px = A[0]; py = A[1];
        }
        if (p.end == null) return;
        if (!Projector.project(p.end.x, p.end.y, p.end.z, B)) return;
        int col = 0xFF000000 | base;
        Gfx.ring(c, Math.round(B[0]) - 6, Math.round(B[1]) - 6, 12, 12, 6, col);
        Gfx.disc(c, Math.round(B[0]), Math.round(B[1]), 2, col);
        String t = (p.label.isEmpty() ? "" : p.label + " ") + String.format("%.1fs", p.ticks / 20.0).replace(',', '.');
        int tw = Gfx.tw(t), w = tw + 10;
        int x = Math.round(B[0]) - w / 2, y = Math.round(B[1]) - 24;
        Gfx.glass(c, x, y, w, 14, 6, false);
        Gfx.shadow(c, t, x + 5, y + 3, 0xFFFFFFFF);
    }
}
