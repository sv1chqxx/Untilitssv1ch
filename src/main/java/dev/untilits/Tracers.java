package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/** Лёгкий оверлей: 2D-рамки ESP и Tracers (линии из прицела). Один проход по сущностям, по одной заливке на линию. */
public final class Tracers {
    private static final float[] P = new float[3];

    private Tracers() { }

    public static void render(DrawContext c, MinecraftClient mc, float tp) {
        Cfg.Data d = Cfg.D;
        boolean anyEsp = d.esp || d.espMobs || d.espItems || d.weak;
        if ((!d.tracers && !anyEsp) || mc.world == null || mc.player == null) return;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        float cx = sw / 2f, cy = sh / 2f;
        double espR2 = (double) d.espRange * d.espRange, trR2 = (double) d.tracerRange * d.tracerRange;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            int k = Esp.kind(e);
            if (k == 0) continue;
            double dist2 = e.squaredDistanceTo(mc.player);
            boolean box = anyEsp && dist2 <= espR2 && Esp.boxWanted(e, k);
            boolean tr = d.tracers && dist2 <= trR2
                    && (k == 1 || ((k == 2 || k == 3) ? d.tracersMobs : d.tracersItems));
            if (!box && !tr) continue;
            int col = 0xFF000000 | Esp.color(e);
            Vec3d lp = e.getLerpedPos(tp);
            double hw = e.getWidth() * 0.5, h = e.getHeight();
            if (box && !box(c, lp.x, lp.y, lp.z, hw, h, col, sw, sh)) box = false;
            if (tr && Projector.project(lp.x, lp.y + h * 0.5, lp.z, P)) {
                line(c, cx, cy, P[0], P[1], 0xE0000000 | (col & 0xFFFFFF));
            }
        }
    }

    /** 2D-рамка вокруг объёма сущности/блока. @return true, если нарисована. */
    public static boolean box(DrawContext c, double x, double y, double z, double hw, double h, int col, int sw, int sh) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            double px = x + ((i & 1) == 0 ? -hw : hw);
            double py = y + ((i & 2) == 0 ? 0 : h);
            double pz = z + ((i & 4) == 0 ? -hw : hw);
            if (!Projector.project(px, py, pz, P)) return false;
            minX = Math.min(minX, P[0]); maxX = Math.max(maxX, P[0]);
            minY = Math.min(minY, P[1]); maxY = Math.max(maxY, P[1]);
        }
        if (maxX < 0 || maxY < 0 || minX > sw || minY > sh) return false;
        if (maxX - minX > sw * 3 || maxY - minY > sh * 3) return false;
        int x0 = Math.round(minX), y0 = Math.round(minY), x1 = Math.round(maxX), y1 = Math.round(maxY);
        if (x1 - x0 < 2) x1 = x0 + 2;
        if (y1 - y0 < 2) y1 = y0 + 2;
        c.fill(x0, y0, x1, y0 + 1, col);
        c.fill(x0, y1 - 1, x1, y1, col);
        c.fill(x0, y0 + 1, x0 + 1, y1 - 1, col);
        c.fill(x1 - 1, y0 + 1, x1, y1 - 1, col);
        return true;
    }

    /** Линия одной повёрнутой заливкой (вместо сотен пикселей). Начало чуть отодвинуто от центра прицела. */
    public static void line(DrawContext c, float x0, float y0, float x1, float y1, int col) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 8f) return;
        float inset = 5f;
        x0 += dx / len * inset; y0 += dy / len * inset;
        len -= inset;
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(x0, y0);
        m.rotate((float) Math.atan2(dy, dx));
        c.fill(0, 0, Math.min(4000, (int) len), 1, col);
        m.popMatrix();
    }
}
