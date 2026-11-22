package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/** Эффекты вокруг цели (по мотивам TargetESP): маркер, круг, призраки, кристаллы, спираль. Рисуется в 2D по проекции. */
public final class TargetEsp {
    public static final String[] MODES = {"Маркер", "Круг", "Призраки", "Кристаллы", "Спираль", "Сакура", "Молнии"};
    public static final String[] SKINS = {"Классика", "Прицел", "Звезда", "Кольцо"};
    private static final String[] SKIN_TEX = {"marker0", "marker1", "marker2", "marker3"};
    static final float[] P = new float[3], Q = new float[3];

    public static LivingEntity target;
    private static long seen;

    private TargetEsp() { }

    private static Identifier tex(String n) { return Identifier.of("untilits", "textures/fx/" + n + ".png"); }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) { target = null; return; }
        if (mc.targetedEntity instanceof LivingEntity le && le != mc.player && le.isAlive()) {
            target = le;
            seen = System.currentTimeMillis();
        } else if (target != null && (!target.isAlive() || System.currentTimeMillis() - seen > 2500)) {
            target = null;
        }
    }

    private static int col(float hueShift, int alpha) {
        return Gfx.withAlpha(Gfx.hsv(((Cfg.D.tgtHue + hueShift) % 360f) / 360f, 0.65f, 1f), alpha);
    }

    /** Спрайт с центром в (x,y), размером s пикселей, поворотом rot. */
    static void spr(DrawContext c, String name, float x, float y, float s, float rot, int color) {
        if (s < 2f) return;
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.rotate(rot);
        float k = s / 64f;
        m.scale(k, k);
        c.drawTexture(RenderPipelines.GUI_TEXTURED, tex(name), -32, -32, 0f, 0f, 64, 64, 256, 256, 256, 256, color);
        m.popMatrix();
    }

    static void seg(DrawContext c, float x0, float y0, float x1, float y1, int col) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f || len > 600f) return;
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(x0, y0);
        m.rotate((float) Math.atan2(dy, dx));
        c.fill(0, 0, Math.max(1, (int) Math.ceil(len)), 2, col);
        m.popMatrix();
    }

    /** Пикселей на 1 блок на расстоянии точки. */
    private static float pxPerBlock(double x, double y, double z) {
        if (!Projector.project(x, y, z, P) || !Projector.project(x, y + 1, z, Q)) return 0;
        return Math.abs(Q[1] - P[1]);
    }

    public static void render(DrawContext c, MinecraftClient mc, float tp) {
        Cfg.Data d = Cfg.D;
        if (!d.tgtEsp || target == null || mc.player == null) return;
        LivingEntity t = target;
        long now = System.currentTimeMillis();
        float fade = Math.min(1f, (2500 - (now - seen)) / 600f);
        if (fade <= 0f) return;
        if (t == mc.player && !Freecam.active) return;
        Vec3d p = t.getLerpedPos(tp);
        double h = t.getHeight(), w = t.getWidth();
        float time = (now % 100000L) / 1000f;
        float size = d.tgtSize / 100f;
        boolean hurt = d.tgtFlash && t.hurtTime > 0;
        int base = hurt ? 0xFF4040 : -1;
        int a = (int) (255 * fade);
        switch (d.tgtMode) {
            case 0 -> {
                float ppb = pxPerBlock(p.x, p.y + h * 0.55, p.z);
                if (ppb <= 0 || !Projector.project(p.x, p.y + h * 0.55, p.z, P)) return;
                float px = Math.min(260f, ppb * (float) Math.max(0.9, h * 0.9) * size);
                int color = hurt ? Gfx.withAlpha(0xFF4040, a) : Gfx.withAlpha(Gfx.hsv(d.tgtHue / 360f, 0.55f, 1f), a);
                spr(c, SKIN_TEX[d.tgtSkin], P[0], P[1], px, time * 1.6f, color);
            }
            case 1 -> {
                int n = 36;
                double r = (w * 0.75 + 0.1) * size;
                double yy = p.y + (0.5 + 0.5 * Math.sin(time * 2.2)) * h;
                float px = 0, py = 0;
                boolean prev = false;
                for (int i = 0; i <= n; i++) {
                    double ang = i * Math.PI * 2 / n;
                    boolean ok = Projector.project(p.x + Math.cos(ang) * r, yy, p.z + Math.sin(ang) * r, P);
                    if (ok && prev) seg(c, px, py, P[0], P[1], hurt ? Gfx.withAlpha(base, a) : col(i * 3f, a));
                    px = P[0]; py = P[1]; prev = ok;
                }
                // шлейф-ореол под кругом
                prev = false;
                for (int i = 0; i <= n; i++) {
                    double ang = i * Math.PI * 2 / n;
                    boolean ok = Projector.project(p.x + Math.cos(ang) * r, p.y + 0.02, p.z + Math.sin(ang) * r, P);
                    if (ok && prev) seg(c, px, py, P[0], P[1], Gfx.withAlpha(col(i * 3f, 255), a / 3));
                    px = P[0]; py = P[1]; prev = ok;
                }
            }
            case 2 -> {
                double r = (w * 0.9 + 0.35) * size;
                for (int g = 0; g < 3; g++) {
                    for (int k = 0; k < 6; k++) {
                        float tt = time * 2.4f - k * 0.11f;
                        double ang = tt + g * Math.PI * 2 / 3;
                        double yy = p.y + h * (0.5 + 0.42 * Math.sin(tt * 0.8 + g * 2.1));
                        if (!Projector.project(p.x + Math.cos(ang) * r, yy, p.z + Math.sin(ang) * r, P)) continue;
                        float ppb = pxPerBlock(p.x, yy, p.z);
                        float s = ppb * 0.55f * size * (1f - k * 0.13f);
                        int al = (int) (a * (1f - k * 0.16f));
                        spr(c, "glow", P[0], P[1], s, 0, hurt ? Gfx.withAlpha(base, al) : col(g * 30f, al));
                    }
                }
            }
            case 3 -> {
                double r = (w * 0.85 + 0.3) * size;
                for (int i = 0; i < 6; i++) {
                    double ang = time * 1.5 + i * Math.PI / 3;
                    double yy = p.y + h + 0.25 + Math.sin(time * 2 + i) * 0.08;
                    if (!Projector.project(p.x + Math.cos(ang) * r, yy, p.z + Math.sin(ang) * r, P)) continue;
                    float ppb = pxPerBlock(p.x, yy, p.z);
                    float s = Math.max(3f, ppb * 0.14f * size);
                    int color = hurt ? Gfx.withAlpha(base, a) : col(i * 20f, a);
                    spr(c, "glow", P[0], P[1], s * 3.2f, 0, Gfx.withAlpha(color, a / 2));
                    var m = c.getMatrices();
                    m.pushMatrix();
                    m.translate(P[0], P[1]);
                    m.rotate((float) (Math.PI / 4 + time * 2));
                    int hs = Math.max(2, (int) (s * 0.5f));
                    c.fill(-hs, -hs, hs, hs, color);
                    m.popMatrix();
                }
            }
            case 5 -> {
                int n = 26;
                for (int i = 0; i < n; i++) {
                    float ph = (time * 0.3f + i / (float) n) % 1f;
                    double ang = i * 2.4 + time * 0.9;
                    double r = (w * 0.7 + 0.25 + 0.3 * (0.5 + 0.5 * Math.sin(i * 1.7 + time))) * size;
                    double yy = p.y + h + 0.3 - ph * (h + 0.5);
                    if (!Projector.project(p.x + Math.cos(ang) * r, yy, p.z + Math.sin(ang) * r, P)) continue;
                    float ppb = pxPerBlock(p.x, yy, p.z);
                    float al = (float) Math.sin(ph * Math.PI);
                    spr(c, "petal", P[0], P[1], ppb * 0.22f * size, time * 2f + i, Gfx.withAlpha(Gfx.lerp(0xFFFFFFFF, Gfx.hsv(d.tgtHue / 360f, 0.5f, 1f), 0.5f), (int) (a * al)));
                }
            }
            case 6 -> {
                long seed = (long) (time * 9f);
                for (int b = 0; b < 3; b++) {
                    java.util.Random rnd = new java.util.Random(seed * 31 + b * 977);
                    double ox = (rnd.nextDouble() - .5) * 1.6 * size, oz = (rnd.nextDouble() - .5) * 1.6 * size;
                    double x0 = p.x + ox, z0 = p.z + oz, y0 = p.y + h + 1.6;
                    float lx = 0, ly = 0;
                    boolean prev = false;
                    int segs = 7;
                    for (int k = 0; k <= segs; k++) {
                        double f = k / (double) segs;
                        double jx = k == segs ? 0 : (rnd.nextDouble() - .5) * 0.5, jz = k == segs ? 0 : (rnd.nextDouble() - .5) * 0.5;
                        double xx = x0 + (p.x - x0) * f + jx, zz = z0 + (p.z - z0) * f + jz, yy = y0 + (p.y - y0) * f;
                        boolean ok = Projector.project(xx, yy, zz, P);
                        if (ok && prev) {
                            seg(c, lx, ly, P[0], P[1], Gfx.withAlpha(col(0, 255), a / 3));
                            seg(c, lx + 1, ly, P[0] + 1, P[1], Gfx.withAlpha(0xFFFFFF, a));
                        }
                        lx = P[0]; ly = P[1]; prev = ok;
                    }
                }
            }
            default -> {
                double r = (w * 0.7 + 0.2) * size;
                for (int s2 = 0; s2 < 2; s2++) {
                    for (int k = 0; k < 14; k++) {
                        float f = k / 13f;
                        double ang = time * 2.0 + f * Math.PI * 3 + s2 * Math.PI;
                        double yy = p.y + ((f + time * 0.25f) % 1.0) * h;
                        if (!Projector.project(p.x + Math.cos(ang) * r, yy, p.z + Math.sin(ang) * r, P)) continue;
                        float ppb = pxPerBlock(p.x, yy, p.z);
                        spr(c, "glow", P[0], P[1], ppb * 0.32f * size, 0,
                                hurt ? Gfx.withAlpha(base, a) : col(f * 70f + s2 * 40f, (int) (a * (0.35f + 0.65f * f))));
                    }
                }
            }
        }
    }

    public static boolean isPlayer(LivingEntity e) { return e instanceof PlayerEntity; }

    /** Карточка цели: ник, HP-полоса. */
    public static void renderInfo(DrawContext c, MinecraftClient mc) {
        if (!Cfg.D.tgtInfo || target == null) return;
        LivingEntity t = target;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        int w = 120, h = 34, x = sw / 2 + 40, y = sh / 2 + 28;
        Gfx.glass(c, x, y, w, h, 9, true);
        String n = t.getName().getString();
        while (n.length() > 3 && Gfx.tw(n) > w - 20) n = n.substring(0, n.length() - 1);
        Gfx.text(c, n, x + 10, y + 7, Friends.is(n) ? 0xFF55FF55 : Gfx.TEXT);
        float hp = Math.max(0, t.getHealth()), mx = Math.max(1, t.getMaxHealth());
        String s = String.format("%.1f", hp);
        Gfx.text(c, s, x + w - 10 - Gfx.tw(s), y + 7, Gfx.DIM);
        int bw = w - 20;
        Gfx.round(c, x + 10, y + 21, bw, 5, 2, 0x33FFFFFF);
        float f = Math.min(1f, hp / mx);
        Gfx.round(c, x + 10, y + 21, Math.max(3, (int) (bw * f)), 5, 2, Gfx.lerp(0xFFFF4D4D, 0xFF4DFF88, f));
    }
}
