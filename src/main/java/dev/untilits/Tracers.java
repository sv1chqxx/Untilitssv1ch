package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

/** Tracers: линии от низа экрана к игрокам / мобам / предметам. */
public final class Tracers {
    private static final float[] P = new float[3];

    private Tracers() { }

    public static void render(DrawContext c, MinecraftClient mc, float tp) {
        Cfg.Data d = Cfg.D;
        if (!d.tracers || mc.world == null || mc.player == null) return;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        double r2 = (double) d.tracerRange * d.tracerRange;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            int k = Esp.kind(e);
            if (k == 0) continue;
            if (k == 2 || k == 3) { if (!d.tracersMobs) continue; }
            else if (k == 4) { if (!d.tracersItems) continue; }
            if (e.squaredDistanceTo(mc.player) > r2) continue;
            Vec3d lp = e.getLerpedPos(tp);
            if (!Projector.project(lp.x, lp.y + e.getHeight() * 0.5, lp.z, P)) continue;
            line(c, sw / 2, sh, Math.round(P[0]), Math.round(P[1]), 0xE0000000 | Esp.colorOf(k), sw, sh);
        }
    }

    private static void line(DrawContext c, int x0, int y0, int x1, int y1, int col, int sw, int sh) {
        int dx = x1 - x0, dy = y1 - y0;
        int steps = Math.min(4000, Math.max(Math.abs(dx), Math.abs(dy)));
        if (steps == 0) return;
        for (int i = 0; i <= steps; i++) {
            int x = x0 + dx * i / steps, y = y0 + dy * i / steps;
            if (x < 0 || y < 0 || x >= sw || y >= sh) continue;
            c.fill(x, y, x + 1, y + 1, col);
        }
    }
}
