package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/** Метки (waypoints): маркеры в мире, у края экрана — указатель направления; авто-метка смерти. */
public final class Waypoints {
    public static final int[] PALETTE = {0x5B8CFF, 0x45D0FF, 0x37E0A8, 0xB78CFF, 0xFF7AC8, 0xFF6B6B, 0xFFB454, 0xF2F4FA};
    private static final float[] P = new float[4];
    private static boolean wasDead;

    private Waypoints() { }

    public static String dim(MinecraftClient mc) {
        return mc.world == null ? "minecraft:overworld" : mc.world.getRegistryKey().getValue().toString();
    }

    public static List<Cfg.WP> list() { return Cfg.D.waypoints; }

    public static Cfg.WP addHere(MinecraftClient mc, String name) {
        if (mc.player == null) return null;
        return add(mc, name, mc.player.getBlockPos());
    }

    public static Cfg.WP addLook(MinecraftClient mc, String name) {
        if (mc.player == null) return null;
        if (mc.crosshairTarget instanceof BlockHitResult b && b.getType() == HitResult.Type.BLOCK) {
            return add(mc, name, b.getBlockPos().up());
        }
        return null;
    }

    public static Cfg.WP add(MinecraftClient mc, String name, BlockPos pos) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) n = "Метка " + (list().size() + 1);
        Cfg.WP w = new Cfg.WP(n, pos.getX(), pos.getY(), pos.getZ(), dim(mc), PALETTE[list().size() % PALETTE.length]);
        list().add(w);
        Cfg.save();
        return w;
    }

    public static boolean remove(String name) {
        boolean r = list().removeIf(w -> w.name.equalsIgnoreCase(name));
        if (r) Cfg.save();
        return r;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) { wasDead = false; return; }
        boolean dead = mc.player.getHealth() <= 0f || mc.player.isDead();
        if (dead && !wasDead && Cfg.D.wpDeath) {
            list().removeIf(w -> w.name.equals("Смерть"));
            Cfg.WP w = add(mc, "Смерть", mc.player.getBlockPos());
            w.color = 0xFF6B6B;
            Cfg.save();
        }
        wasDead = dead;
    }

    public static String distText(double d) {
        return d >= 1000 ? String.format("%.1fk", d / 1000.0).replace(',', '.') : Math.round(d) + "m";
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        if (!Cfg.D.wpEnabled || list().isEmpty() || mc.player == null) return;
        String dim = dim(mc);
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        double max = Cfg.D.wpMaxDist;
        var pp = mc.player.getEntityPos();
        for (Cfg.WP w : list()) {
            if (!w.visible || !dim.equals(w.dim)) continue;
            double wx = w.x + 0.5, wy = w.y + 0.5, wz = w.z + 0.5;
            double dist = Math.sqrt(pp.squaredDistanceTo(wx, wy, wz));
            if (max > 0 && dist > max) continue;
            int state = Projector.projectEdge(wx, wy + 1.0, wz, 16f, P);
            int col = 0xFF000000 | w.color;
            String label = w.name + "  " + distText(dist);
            int tw = Gfx.tw(label);
            int pw = tw + 22, ph = 18;
            float x = P[0], y = P[1];
            if (state == 0) {
                // маркер: ромб над точкой + стеклянная плашка с названием и дистанцией
                float s = (float) Math.max(0.65, Math.min(1.1, 1.25 - dist / 400.0));
                var m = c.getMatrices();
                m.pushMatrix();
                m.translate(x, y);
                m.scale(s, s);
                Gfx.glass(c, -pw / 2, -ph - 8, pw, ph, 7, false);
                Gfx.disc(c, -pw / 2 + 9, -ph - 8 + ph / 2, 3, col);
                Gfx.shadow(c, label, -pw / 2 + 16, -ph - 8 + (ph - 8) / 2, 0xFFFFFFFF);
                // ромб-пин
                m.pushMatrix();
                m.translate(0, 0);
                m.rotate((float) (Math.PI / 4));
                c.fill(-3, -3, 3, 3, col);
                m.popMatrix();
                c.fill(0, -8, 1, -2, Gfx.withAlpha(col, 160));
                m.popMatrix();
            } else {
                // у края: плашка + стрелка в сторону метки
                float cx = Math.max(pw / 2f + 6, Math.min(sw - pw / 2f - 6, x));
                float cy = Math.max(ph / 2f + 6, Math.min(sh - ph / 2f - 6, y));
                var m = c.getMatrices();
                m.pushMatrix();
                m.translate(cx, cy);
                Gfx.glass(c, -pw / 2, -ph / 2, pw, ph, 7, false);
                Gfx.disc(c, -pw / 2 + 9, 0, 3, col);
                Gfx.shadow(c, label, -pw / 2 + 16, -4, 0xFFFFFFFF);
                m.popMatrix();
                // стрелка: треугольник из повёрнутого квадрата за краем плашки
                float ang = P[3];
                float ax = cx + (float) Math.cos(ang) * (pw / 2f + 5) * (Math.abs(Math.cos(ang)) > 0.5 ? 1f : 0.4f);
                float ay = cy + (float) Math.sin(ang) * (ph / 2f + 5);
                var m2 = c.getMatrices();
                m2.pushMatrix();
                m2.translate(ax, ay);
                m2.rotate(ang + (float) (Math.PI / 4));
                c.fill(-3, -3, 3, 3, col);
                m2.popMatrix();
            }
        }
    }
}
