package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/** Эффекты вокруг цели (по образцу TargetESP из Maksai): рисуются спрайтами по проекции. */
public final class TargetEsp {
    public static final String[] MODES = {"Маркер", "Призраки", "Цветение", "Созвездие", "Сердца", "Цепи", "Сакура", "Роза", "Мечи", "Молнии", "Круг"};
    public static final String[] SKINS = {"Классика", "Прицел", "Звезда", "Кольцо"};
    private static final String[] SKIN_TEX = {"marker0", "marker1", "marker2", "marker3"};
    static final float[] P = new float[3], Q = new float[3];
    private static final float[] R1 = new float[3], R2 = new float[3];
    private static final ItemStack SWORD = new ItemStack(Items.DIAMOND_SWORD);

    public static LivingEntity target;
    private static LivingEntity shown;
    private static long seen, since;

    private TargetEsp() { }

    private static Identifier tex(String n) { return Identifier.of("untilits", "textures/fx/" + n + ".png"); }

    public static long seenAt() { return seen; }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) { target = null; return; }
        if (mc.targetedEntity instanceof LivingEntity le && le != mc.player && le.isAlive()) {
            target = le;
            seen = System.currentTimeMillis();
        } else if (target != null && (!target.isAlive() || System.currentTimeMillis() - seen > 2500)) {
            target = null;
        }
    }

    // ---------- примитивы ----------

    private static int col(float hueShift, int alpha) {
        return Gfx.withAlpha(Gfx.hsv(((Cfg.D.tgtHue + hueShift) % 360f + 360f) % 360f / 360f, 0.62f, 1f), Math.max(0, Math.min(255, alpha)));
    }

    /** Квадратный спрайт 256x256 с центром (x,y), размером s px, поворотом rot. */
    static void spr(DrawContext c, String name, float x, float y, float s, float rot, int color) {
        sprRect(c, name, x, y, s, s, rot, color, 256, 256);
    }

    static void sprRect(DrawContext c, String name, float x, float y, float sw, float sh, float rot, int color, int tw, int th) {
        if (sw < 1.5f || sh < 1.5f || (color >>> 24) == 0) return;
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.rotate(rot);
        m.scale(sw / tw, sh / th);
        c.drawTexture(RenderPipelines.GUI_TEXTURED, tex(name), -tw / 2, -th / 2, 0f, 0f, tw, th, tw, th, tw, th, color);
        m.popMatrix();
    }

    static void seg(DrawContext c, float x0, float y0, float x1, float y1, int col) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f || len > 700f) return;
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(x0, y0);
        m.rotate((float) Math.atan2(dy, dx));
        c.fill(0, 0, Math.max(1, (int) Math.ceil(len)), 2, col);
        m.popMatrix();
    }

    static float pxPerBlock(double x, double y, double z) {
        if (!Projector.project(x, y, z, R1) || !Projector.project(x, y + 1, z, R2)) return 0;
        return Math.min(70f, Math.abs(R2[1] - R1[1]));
    }

    private static float rnd(int i, int k) {
        double v = Math.sin(i * 12.9898 + k * 78.233) * 43758.5453;
        return (float) (v - Math.floor(v));
    }

    /** Мягкое свечение под спрайтом. */
    private static void glow(DrawContext c, float x, float y, float s, int color) {
        if (Cfg.D.tgtGlow) spr(c, "glow", x, y, s, 0, color);
    }

    // ---------- рендер ----------

    public static void render(DrawContext c, MinecraftClient mc, float tp) {
        Cfg.Data d = Cfg.D;
        if (!d.tgtEsp || target == null || mc.player == null) return;
        LivingEntity t = target;
        long now = System.currentTimeMillis();
        float fade = Math.min(1f, (2500 - (now - seen)) / 600f);
        if (fade <= 0f) return;
        if (t != shown) { shown = t; since = now; }
        float appear = Math.min(1f, (now - since) / 380f);
        appear = 1f - (1f - appear) * (1f - appear);
        if (t == mc.player && !Freecam.active) return;
        Vec3d p = t.getLerpedPos(tp);
        double h = t.getHeight(), w = t.getWidth();
        float sp = d.tgtSpeed / 100f;
        float time = (now % 1000000L) / 1000f * sp;
        float size = d.tgtSize / 100f;
        boolean hurt = d.tgtFlash && t.hurtTime > 0;
        int a = (int) (255 * fade * appear);
        int n = d.tgtCount;
        double cy = p.y + h * 0.55;
        switch (d.tgtMode) {
            case 0 -> marker(c, p, h, size, time, hurt, a);
            case 1 -> ghosts(c, p, w, h, size, time, hurt, a, n);
            case 2 -> bloom(c, p, w, h, size, time, hurt, a, n);
            case 3 -> constellation(c, p, w, h, size, time, hurt, a, n);
            case 4 -> hearts(c, p, w, h, size, time, hurt, a, n);
            case 5 -> chains(c, p, w, h, size, time, hurt, a);
            case 6 -> petals(c, p, w, h, size, time, a, n, "petal", "flower", true);
            case 7 -> petals(c, p, w, h, size, time, a, n, "rosepetal", "rose", false);
            case 8 -> swords(c, p, w, h, size, time, hurt, a, n);
            case 9 -> lightning(c, p, w, h, size, time, hurt, a, n);
            default -> circle(c, p, w, h, size, time, hurt, a);
        }
    }

    private static int tint(boolean hurt, float shift, int alpha) { return hurt ? Gfx.withAlpha(0xFF4040, alpha) : col(shift, alpha); }

    private static void marker(DrawContext c, Vec3d p, double h, float size, float time, boolean hurt, int a) {
        double y = p.y + h * 0.55;
        float ppb = pxPerBlock(p.x, y, p.z);
        if (ppb <= 0 || !Projector.project(p.x, y, p.z, P)) return;
        float px = Math.min(300f, ppb * (float) Math.max(0.9, h * 0.95) * size);
        glow(c, P[0], P[1], px * 1.5f, tint(hurt, 0, a / 3));
        spr(c, SKIN_TEX[Cfg.D.tgtSkin], P[0], P[1], px, time * 1.6f, tint(hurt, 0, a));
    }

    private static void ghosts(DrawContext c, Vec3d p, double w, double h, float size, float time, boolean hurt, int a, int n) {
        double r = (w * 0.85 + 0.45) * size;
        int orbs = Math.max(1, Math.min(4, (n + 1) / 2));
        for (int g = 0; g < orbs; g++) {
            for (int k = 0; k < 14; k++) {
                float tt = time * 2.6f - k * 0.075f;
                double ang = tt + g * Math.PI * 2 / orbs;
                double yy = p.y + h * (0.52 + 0.42 * Math.sin(tt * 0.7 + g * 2.1));
                if (!Projector.project(p.x + Math.cos(ang) * r, yy, p.z + Math.sin(ang) * r, P)) continue;
                float ppb = pxPerBlock(p.x, yy, p.z);
                float f = 1f - k / 14f;
                int al = (int) (a * f * f);
                glow(c, P[0], P[1], ppb * 0.85f * size * (0.35f + 0.65f * f), tint(hurt, g * 40f + k * 4f, al / 2));
                spr(c, "bloom", P[0], P[1], ppb * 0.42f * size * f, 0, tint(hurt, g * 40f, al));
            }
        }
    }

    private static void bloom(DrawContext c, Vec3d p, double w, double h, float size, float time, boolean hurt, int a, int n) {
        int count = Math.min(48, n * 3 + 6);
        for (int i = 0; i < count; i++) {
            float ph = (time * (0.18f + rnd(i, 1) * 0.16f) + rnd(i, 2)) % 1f;
            double ang = rnd(i, 3) * Math.PI * 2 + time * 0.25 * (rnd(i, 9) > 0.5f ? 1 : -1);
            double rr = (w * 0.45 + 0.1 + ph * (0.7 + rnd(i, 4) * 0.8)) * size;
            double yy = p.y + h * (0.1 + rnd(i, 5) * 0.8) + ph * 0.6 - 0.2;
            if (!Projector.project(p.x + Math.cos(ang) * rr, yy, p.z + Math.sin(ang) * rr, P)) continue;
            float ppb = pxPerBlock(p.x, yy, p.z);
            float al = (float) Math.sin(ph * Math.PI);
            float s = ppb * (0.35f + rnd(i, 6) * 0.45f) * size * (1f - ph * 0.35f);
            int base = hurt ? 0xFF6060 : Gfx.lerp(0xFFFFFFFF, Gfx.hsv(Cfg.D.tgtHue / 360f, 0.5f, 1f), 0.55f);
            spr(c, "bloom", P[0], P[1], s, time * (rnd(i, 7) - 0.5f) * 3f, Gfx.withAlpha(base, (int) (a * al * 0.5f)));
        }
    }

    private static void constellation(DrawContext c, Vec3d p, double w, double h, float size, float time, boolean hurt, int a, int n) {
        int count = Math.min(30, n * 2 + 4);
        float[] xs = new float[count], ys = new float[count];
        boolean[] ok = new boolean[count];
        float[] ss = new float[count];
        for (int i = 0; i < count; i++) {
            double ang = rnd(i, 1) * Math.PI * 2 + time * 0.35;
            double yy01 = rnd(i, 2);
            double rr = (w * 0.6 + 0.55 + rnd(i, 3) * 0.55) * size * Math.sqrt(1 - Math.pow(yy01 * 2 - 1, 2) * 0.6);
            double yy = p.y + h * (0.05 + yy01 * 1.0);
            ok[i] = Projector.project(p.x + Math.cos(ang) * rr, yy, p.z + Math.sin(ang) * rr, P);
            xs[i] = P[0]; ys[i] = P[1];
            ss[i] = pxPerBlock(p.x, yy, p.z);
        }
        for (int i = 0; i < count; i++) {
            int j = (i + 1) % count, k = (i + 3) % count;
            if (ok[i] && ok[j] && i != count - 1) seg(c, xs[i], ys[i], xs[j], ys[j], tint(hurt, 20, a / 4));
            if (ok[i] && ok[k] && i % 3 == 0) seg(c, xs[i], ys[i], xs[k], ys[k], tint(hurt, 20, a / 6));
        }
        for (int i = 0; i < count; i++) {
            if (!ok[i]) continue;
            float tw = 0.7f + 0.3f * (float) Math.sin(time * 3 + i * 1.7);
            float s = ss[i] * (0.22f + rnd(i, 4) * 0.14f) * size * tw;
            glow(c, xs[i], ys[i], s * 3f, tint(hurt, i * 9f, a / 3));
            spr(c, "star4", xs[i], ys[i], s * 1.8f, time * 0.8f + i, tint(hurt, i * 9f, a));
        }
    }

    private static void hearts(DrawContext c, Vec3d p, double w, double h, float size, float time, boolean hurt, int a, int n) {
        int count = Math.min(24, n + 2);
        for (int i = 0; i < count; i++) {
            float ph = (time * 0.32f + i / (float) count) % 1f;
            double ang = i * 2.399 + time * 0.8 + Math.sin(ph * 6) * 0.4;
            double rr = (w * 0.55 + 0.3 + 0.15 * Math.sin(ph * 5 + i)) * size;
            double yy = p.y + ph * (h + 0.7);
            if (!Projector.project(p.x + Math.cos(ang) * rr, yy, p.z + Math.sin(ang) * rr, P)) continue;
            float ppb = pxPerBlock(p.x, yy, p.z);
            float al = (float) Math.sin(ph * Math.PI);
            float s = ppb * 0.3f * size * (0.7f + 0.3f * (float) Math.sin(time * 6 + i));
            glow(c, P[0], P[1], s * 2.2f, tint(hurt, 330f, (int) (a * al / 3)));
            spr(c, "heart", P[0], P[1], s, (float) Math.sin(time * 2 + i) * 0.35f, tint(hurt, 330f - Cfg.D.tgtHue + Cfg.D.tgtHue * 0.1f, (int) (a * al)));
        }
    }

    private static void chains(DrawContext c, Vec3d p, double w, double h, float size, float time, boolean hurt, int a) {
        double r = (w * 0.62 + 0.14) * size;
        int segs = 18;
        int base = hurt ? 0xFF8080 : Gfx.lerp(0xFFFFFFFF, Gfx.hsv(Cfg.D.tgtHue / 360f, 0.4f, 1f), 0.2f);
        for (int band = 0; band < 2; band++) {
            double dir = band == 0 ? 1 : -1;
            boolean prev = false;
            float px = 0, py = 0;
            double pa = 0;
            for (int j = 0; j <= segs; j++) {
                double u = j / (double) segs;
                double ang = time * 0.7 * dir + dir * u * Math.PI * 1.9 + band * 1.6;
                double yy = p.y + h * (0.12 + 0.78 * u);
                boolean ok = Projector.project(p.x + Math.cos(ang) * r, yy, p.z + Math.sin(ang) * r, P);
                if (ok && prev) {
                    float dx = P[0] - px, dy = P[1] - py;
                    float len = (float) Math.sqrt(dx * dx + dy * dy);
                    if (len > 2f && len < 220f) {
                        float depth = (float) (0.5 + 0.5 * Math.sin((ang + pa) / 2 + 1.5707));
                        sprRect(c, "chain", (P[0] + px) / 2, (P[1] + py) / 2, len * 1.12f, len * 0.84f, (float) Math.atan2(dy, dx),
                                Gfx.withAlpha(base, (int) (a * (0.3f + 0.7f * depth))), 256, 192);
                    }
                }
                px = P[0]; py = P[1]; pa = ang; prev = ok;
            }
        }
    }

    private static void petals(DrawContext c, Vec3d p, double w, double h, float size, float time, int a, int n, String petal, String flower, boolean tintHue) {
        int count = Math.min(48, n * 4 + 6);
        for (int i = 0; i < count; i++) {
            float ph = (time * (0.22f + rnd(i, 1) * 0.18f) + rnd(i, 2)) % 1f;
            double ang = rnd(i, 3) * Math.PI * 2 + time * 0.6 + Math.sin(ph * 7 + i) * 0.5;
            double rr = (w * 0.6 + 0.25 + rnd(i, 4) * 0.8 + 0.12 * Math.sin(ph * 9)) * size;
            double yy = p.y + h + 0.5 - ph * (h + 0.7);
            if (!Projector.project(p.x + Math.cos(ang) * rr, yy, p.z + Math.sin(ang) * rr, P)) continue;
            float ppb = pxPerBlock(p.x, yy, p.z);
            float al = (float) Math.sin(ph * Math.PI);
            boolean fl = i % 5 == 0;
            float s = ppb * (fl ? 0.3f : 0.2f) * size;
            int tc = tintHue ? Gfx.withAlpha(Gfx.lerp(0xFFFFFFFF, Gfx.hsv(Cfg.D.tgtHue / 360f, 0.35f, 1f), 0.4f), (int) (a * al)) : Gfx.withAlpha(0xFFFFFF, (int) (a * al));
            spr(c, fl ? flower : petal, P[0], P[1], s, time * 2.2f + i * 1.3f, tc);
        }
    }

    private static void swords(DrawContext c, Vec3d p, double w, double h, float size, float time, boolean hurt, int a, int n) {
        int count = Math.min(12, n + 1);
        double r = (w * 0.8 + 0.7) * size;
        for (int i = 0; i < count; i++) {
            double ang = time * 1.4 + i * Math.PI * 2 / count;
            double yy = p.y + h * 0.58 + Math.sin(time * 2 + i * 1.3) * 0.12;
            if (!Projector.project(p.x + Math.cos(ang) * r, yy, p.z + Math.sin(ang) * r, P)) continue;
            float ppb = pxPerBlock(p.x, yy, p.z);
            float depth = (float) (0.65 + 0.35 * Math.sin(ang + 1.5707));
            float s = ppb * 0.55f * size * depth;
            glow(c, P[0], P[1], s * 1.6f, tint(hurt, i * 15f, (int) (a * 0.35f)));
            if (s < 3f) continue;
            var m = c.getMatrices();
            m.pushMatrix();
            m.translate(P[0], P[1]);
            m.rotate((float) (Math.sin(ang) * 0.5 + 0.785));
            m.scale(s / 16f, s / 16f);
            c.drawItem(SWORD, -8, -8);
            m.popMatrix();
        }
    }

    private static void lightning(DrawContext c, Vec3d p, double w, double h, float size, float time, boolean hurt, int a, int n) {
        Cfg.Data d = Cfg.D;
        long frame = System.currentTimeMillis() / 70;
        double r = (w * 0.75 + 0.3) * size;
        int arcs = Math.max(2, Math.min(12, n));
        for (int i = 0; i < arcs; i++) {
            if (rnd((int) frame, i + 31) < 0.3f) continue;   // мерцание
            double base = rnd(i, 1) * Math.PI * 2 + time * 0.4;
            int pts = 9;
            float px = 0, py = 0;
            boolean prev = false;
            for (int k = 0; k <= pts; k++) {
                double f = k / (double) pts;
                double jit = (rnd((int) frame * 13 + i, k + 5) - 0.5) * 0.55;
                double rr = r * (0.8 + (rnd((int) frame * 7 + i, k + 40) - 0.5) * 0.6);
                double ang = base + jit;
                double yy = p.y + h + 0.5 - f * (h + 0.5);
                boolean ok = Projector.project(p.x + Math.cos(ang) * rr, yy, p.z + Math.sin(ang) * rr, P);
                if (ok && prev) {
                    seg(c, px - 1, py, P[0] - 1, P[1], tint(hurt, i * 10f, a / 4));
                    seg(c, px + 1, py, P[0] + 1, P[1], tint(hurt, i * 10f, a / 4));
                    seg(c, px, py, P[0], P[1], Gfx.withAlpha(0xFFFFFF, a));
                }
                px = P[0]; py = P[1]; prev = ok;
            }
        }
        double top = p.y + h + 0.3;
        if (Projector.project(p.x, top, p.z, P)) {
            float ppb = pxPerBlock(p.x, top, p.z);
            float fl = (float) Math.pow(0.5 + 0.5 * Math.sin(time * 9), 2);
            glow(c, P[0], P[1], ppb * 1.6f * size, tint(hurt, 0, (int) (a * (0.25f + 0.35f * fl))));
        }
        float cyc = (time / 3.3f) % 1f;
        if (cyc < 0.09f && d.tgtLFlash) {
            float f = cyc / 0.09f;
            int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
            sprRect(c, "screenglow", sw / 2f, sh / 2f, sw, sh, 0, tint(hurt, 0, (int) (110 * (1f - f))), 512, 288);
        }
    }

    private static void circle(DrawContext c, Vec3d p, double w, double h, float size, float time, boolean hurt, int a) {
        double r = (w * 0.75 + 0.15) * size;
        int n = 40;
        double yy = p.y + (0.5 + 0.5 * Math.sin(time * 2.0)) * h;
        for (int pass = 0; pass < 2; pass++) {
            double y0 = pass == 0 ? yy : p.y + 0.03;
            float px = 0, py = 0;
            boolean prev = false;
            for (int i = 0; i <= n; i++) {
                double ang = i * Math.PI * 2 / n;
                boolean ok = Projector.project(p.x + Math.cos(ang) * r, y0, p.z + Math.sin(ang) * r, P);
                if (ok && prev) seg(c, px, py, P[0], P[1], tint(hurt, i * 4f, pass == 0 ? a : a / 3));
                px = P[0]; py = P[1]; prev = ok;
            }
        }
        for (int k = 0; k < 3; k++) {
            for (int j = 0; j < 8; j++) {
                double ang = time * 2.2 - j * 0.09 + k * Math.PI * 2 / 3;
                if (!Projector.project(p.x + Math.cos(ang) * r, yy, p.z + Math.sin(ang) * r, P)) continue;
                float ppb = pxPerBlock(p.x, yy, p.z);
                float f = 1f - j / 8f;
                glow(c, P[0], P[1], ppb * 0.55f * size * f, tint(hurt, k * 40f, (int) (a * f)));
            }
        }
    }

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
