package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/** 3D-сцены убийства: автобус, из которого двое забирают тело, и FPV-дрон, влетающий в цель и взрывающийся. Рисуются полигонами по проекции. */
public final class Kill3D {
    private record Scene(boolean bus, double x, double y, double z, double yaw, long start, long dur, long seed) { }
    private static final List<Scene> SCENES = new ArrayList<>();
    private static boolean boomed;
    private static final float[] A = new float[3], B = new float[3], C = new float[3], D = new float[3];

    private Kill3D() { }

    public static void start(boolean bus, double x, double y, double z, double yaw) {
        while (SCENES.size() >= 2) SCENES.remove(0);
        long dur = (long) ((bus ? Cfg.D.killBusDur : Cfg.D.killFpvDur) * 1000L);
        SCENES.add(new Scene(bus, x, y, z, yaw, System.currentTimeMillis(), dur, System.nanoTime() & 0xFFFF));
    }

    /** Поза камеры дрона {x,y,z,yawDeg,pitchDeg} или null, если камера дрона не активна. */
    public static double[] camPose() {
        if (!Cfg.D.killCam || !Cfg.D.killFx) return null;
        long now = System.currentTimeMillis();
        for (Scene s : SCENES) {
            if (s.bus || now - s.start > s.dur) continue;
            double D = s.dur / 1000.0, flight = D * 0.72;
            double t = Math.min((now - s.start) / 1000.0, flight - 0.01);
            double[] p = dronePos(s, t), q = dronePos(s, t + 0.05);
            double dx = q[0] - p[0], dy = q[1] - p[1], dz = q[2] - p[2];
            // смотрим на цель, чуть смещаясь по курсу полёта
            double tx = s.x - p[0], ty = s.y + 1.0 - p[1], tz = s.z - p[2];
            double h = Math.sqrt(tx * tx + tz * tz);
            return new double[]{p[0], p[1], p[2], Math.toDegrees(Math.atan2(-tx, tz)), Math.toDegrees(-Math.atan2(ty, h))};
        }
        return null;
    }

    private static double[] dronePos(Scene s, double t) {
        double flight = s.dur / 1000.0 * 0.72;
        java.util.Random rnd = new java.util.Random(s.seed);
        double ang = rnd.nextDouble() * Math.PI * 2;
        double sx = s.x + Math.cos(ang) * 26, sz = s.z + Math.sin(ang) * 26, sy = s.y + 12;
        double u = Math.min(1.0, t / flight), e = Math.pow(u, 1.55), wob = (1 - u) * 0.9;
        return new double[]{lerp(sx, s.x, e) + Math.sin(u * 11) * wob * Math.cos(ang + 1.57), lerp(sy, s.y + 1.0, e) + Math.sin(u * 9 + 1) * wob * 0.5, lerp(sz, s.z, e) + Math.sin(u * 11) * wob * Math.sin(ang + 1.57)};
    }

    public static void clear() { SCENES.clear(); }

    // ======================================================= меш / полигоны

    private record Face(float[] xs, float[] ys, float depth, int col, boolean edge) { }

    private static final class Frame {
        double ox, oy, oz, yaw, pitch;
        Frame(double ox, double oy, double oz, double yaw, double pitch) { this.ox = ox; this.oy = oy; this.oz = oz; this.yaw = yaw; this.pitch = pitch; }
        double[] w(double x, double y, double z) {
            double cp = Math.cos(pitch), sp = Math.sin(pitch);
            double y1 = y * cp - z * sp, z1 = y * sp + z * cp;
            double cy = Math.cos(yaw), sy = Math.sin(yaw);
            double x2 = x * cy + z1 * sy, z2 = -x * sy + z1 * cy;
            return new double[]{ox + x2, oy + y1, oz + z2};
        }
    }

    private static final int[][] QUADS = {{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
    private static final double LX = 0.35, LY = 0.85, LZ = 0.4;

    private static final class Mesh {
        final List<Face> faces = new ArrayList<>();

        /** Бокс с центром (cx,cy,cz) и полуразмерами; swing — поворот вокруг оси X в точке (pvx,pvy,pvz). */
        void box(Frame f, double cx, double cy, double cz, double hx, double hy, double hz, double swing, double pvx, double pvy, double pvz, int rgb) {
            double[][] v = new double[8][];
            double cs = Math.cos(swing), sn = Math.sin(swing);
            for (int i = 0; i < 8; i++) {
                double x = cx + ((i & 1) == 0 ? -hx : hx), y = cy + ((i & 2) == 0 ? -hy : hy), z = cz + ((i & 4) == 0 ? -hz : hz);
                double ry = y - pvy, rz = z - pvz;
                double y2 = pvy + ry * cs - rz * sn, z2 = pvz + ry * sn + rz * cs;
                v[i] = f.w(x, y2, z);
                v[i][2] = v[i][2];
                double[] wv = f.w(x, y2, z2);
                v[i] = wv;
            }
            double mx = 0, my = 0, mz = 0;
            for (double[] p : v) { mx += p[0]; my += p[1]; mz += p[2]; }
            mx /= 8; my /= 8; mz /= 8;
            double camX = Projector.camX(), camY = Projector.camY(), camZ = Projector.camZ();
            for (int[] q : QUADS) {
                double[] p0 = v[q[0]], p1 = v[q[1]], p2 = v[q[2]];
                double ux = p1[0] - p0[0], uy = p1[1] - p0[1], uz = p1[2] - p0[2];
                double wx = p2[0] - p0[0], wy = p2[1] - p0[1], wz = p2[2] - p0[2];
                double nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx;
                double fcx = (p0[0] + v[q[2]][0]) / 2, fcy = (p0[1] + v[q[2]][1]) / 2, fcz = (p0[2] + v[q[2]][2]) / 2;
                if (nx * (fcx - mx) + ny * (fcy - my) + nz * (fcz - mz) < 0) { nx = -nx; ny = -ny; nz = -nz; }
                if (nx * (camX - fcx) + ny * (camY - fcy) + nz * (camZ - fcz) <= 0) continue;   // обратная грань
                double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (nl < 1e-9) continue;
                double lit = (nx * LX + ny * LY + nz * LZ) / nl;
                double shade = 0.58 + 0.42 * Math.max(0, lit);
                int r = (int) (((rgb >> 16) & 255) * shade), g = (int) (((rgb >> 8) & 255) * shade), b = (int) ((rgb & 255) * shade);
                float[] xs = new float[4], ys = new float[4];
                float depth = 0;
                boolean ok = true;
                float[] o = new float[3];
                for (int k = 0; k < 4; k++) {
                    double[] p = v[q[k]];
                    if (!Projector.project(p[0], p[1], p[2], o)) { ok = false; break; }
                    xs[k] = o[0]; ys[k] = o[1]; depth += o[2];
                }
                if (!ok) continue;
                faces.add(new Face(xs, ys, depth / 4f, 0xFF000000 | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b), true));
            }
        }

        void draw(DrawContext c) {
            faces.sort((a, b) -> Float.compare(b.depth, a.depth));
            int sh = c.getScaledWindowHeight(), sw = c.getScaledWindowWidth();
            for (Face f : faces) {
                float minY = Math.min(Math.min(f.ys[0], f.ys[1]), Math.min(f.ys[2], f.ys[3]));
                float maxY = Math.max(Math.max(f.ys[0], f.ys[1]), Math.max(f.ys[2], f.ys[3]));
                int y0 = Math.max(0, (int) Math.floor(minY)), y1 = Math.min(sh - 1, (int) Math.ceil(maxY));
                for (int y = y0; y <= y1; y++) {
                    double yc = y + 0.5, lo = 1e9, hi = -1e9;
                    for (int e = 0; e < 4; e++) {
                        int n = (e + 1) & 3;
                        double ya = f.ys[e], yb = f.ys[n];
                        if ((ya <= yc && yb > yc) || (yb <= yc && ya > yc)) {
                            double x = f.xs[e] + (yc - ya) * (f.xs[n] - f.xs[e]) / (yb - ya);
                            if (x < lo) lo = x;
                            if (x > hi) hi = x;
                        }
                    }
                    if (hi > lo) {
                        int xa = Math.max(0, (int) Math.floor(lo)), xb = Math.min(sw, (int) Math.ceil(hi));
                        if (xb > xa) c.fill(xa, y, xb, y + 1, f.col);
                    }
                }
                // тёмная кромка для читаемости
                if (f.edge && Math.max(maxY - minY, 0) > 6) {
                    int ec = 0x66000000;
                    for (int e = 0; e < 4; e++) { int n = (e + 1) & 3; TargetEsp.seg(c, f.xs[e], f.ys[e], f.xs[n], f.ys[n], ec); }
                }
            }
        }
    }

    // ======================================================= фигуры

    private static void figure(Mesh m, Frame f, int shirt, int pants, int skin, double swing, double armSwing) {
        m.box(f, 0, 1.75, 0, .25, .25, .25, 0, 0, 0, 0, skin);
        m.box(f, 0, 1.125, 0, .25, .375, .125, 0, 0, 0, 0, shirt);
        m.box(f, -.375, 1.125, 0, .125, .375, .125, armSwing, -.375, 1.5, 0, shirt);
        m.box(f, .375, 1.125, 0, .125, .375, .125, -armSwing, .375, 1.5, 0, shirt);
        m.box(f, -.125, .375, 0, .125, .375, .125, swing, -.125, .75, 0, pants);
        m.box(f, .125, .375, 0, .125, .375, .125, -swing, .125, .75, 0, pants);
    }

    private static double ease(double t) { t = Math.max(0, Math.min(1, t)); return t * t * (3 - 2 * t); }
    private static double lerp(double a, double b, double t) { return a + (b - a) * Math.max(0, Math.min(1, t)); }
    private static double seg(double t, double a, double b) { return Math.max(0, Math.min(1, (t - a) / (b - a))); }

    // ======================================================= рендер

    public static void render(DrawContext c, MinecraftClient mc) {
        if (SCENES.isEmpty()) return;
        long now = System.currentTimeMillis();
        SCENES.removeIf(s -> now - s.start > s.dur + 200);
        for (Scene s : new ArrayList<>(SCENES)) {
            double t = (now - s.start) / 1000.0;
            if (s.bus) bus(c, mc, s, t); else fpv(c, mc, s, t);
        }
    }

    // ---------------- автобус

    private static void bus(DrawContext c, MinecraftClient mc, Scene s, double t) {
        double k = (s.dur / 1000.0) / 9.0;
        double u = t / k;               // нормированное время 0..9
        double yaw = s.yaw;
        double fx = Math.sin(yaw), fz = Math.cos(yaw);          // вперёд у автобуса
        double sx = Math.cos(yaw), sz = -Math.sin(yaw);         // вправо у автобуса
        double cx = s.x + sx * 4.0, cz = s.z + sz * 4.0, gy = s.y;   // стоянка автобуса (жертва слева от него)
        // положение автобуса
        double dist;
        if (u < 2.0) dist = -30 * Math.pow(1 - ease(u / 2.0 * 0.0 + Math.pow(u / 2.0, 0.6)), 1.0) ;
        else if (u < 7.5) dist = 0;
        else dist = 42 * Math.pow(seg(u, 7.5, 9.0), 2);
        if (u < 2.0) { double q = u / 2.0; dist = -30 * (1 - (1 - Math.pow(1 - q, 3))); dist = -30 * Math.pow(1 - q, 3); }
        double bx = cx + fx * dist, bz = cz + fz * dist;
        boolean moving = u < 2.0 || u > 7.5;
        double bounce = moving ? Math.sin(t * 22) * 0.02 : 0;
        Frame bf = new Frame(bx, gy + bounce, bz, yaw, 0);
        Mesh m = new Mesh();
        // кузов
        m.box(bf, 0, 1.45, 0, 1.2, 1.05, 3.5, 0, 0, 0, 0, 0xF2B705);
        m.box(bf, 0, 2.6, 0, 1.2, .08, 3.5, 0, 0, 0, 0, 0xF4F4F4);
        m.box(bf, 0, .95, 0, 1.22, .06, 3.52, 0, 0, 0, 0, 0x1A1A1A);          // полоса
        m.box(bf, 0, .38, 0, 1.2, .1, 3.55, 0, 0, 0, 0, 0x2A2A2A);            // днище/бампер
        for (int side = -1; side <= 1; side += 2) {
            double wx = side * 1.215;
            for (int i = 0; i < 4; i++) m.box(bf, wx, 1.75, -2.55 + i * 1.05, .02, .38, .42, 0, 0, 0, 0, 0x9FD8FF);
            m.box(bf, wx, 1.75, 3.0 - 0.0, .02, .38, .3, 0, 0, 0, 0, 0x9FD8FF);
            for (int zz = -1; zz <= 1; zz += 2) {
                m.box(bf, side * 1.2, .45, zz * 2.2, .16, .45, .45, 0, 0, 0, 0, 0x151515);
                m.box(bf, side * 1.34, .45, zz * 2.2, .02, .2, .2, 0, 0, 0, 0, 0xB0B0B0);
            }
        }
        m.box(bf, 0, 1.75, 3.52, 1.0, .42, .02, 0, 0, 0, 0, 0x8EC9F5);       // лобовое
        m.box(bf, 0, 1.75, -3.52, 1.0, .42, .02, 0, 0, 0, 0, 0x8EC9F5);
        m.box(bf, -.8, .8, 3.53, .2, .12, .02, 0, 0, 0, 0, 0xFFF6C2);          // фары
        m.box(bf, .8, .8, 3.53, .2, .12, .02, 0, 0, 0, 0, 0xFFF6C2);
        m.box(bf, 0, 2.72, 0, .5, .12, .6, 0, 0, 0, 0, 0xE63946);              // мигалка
        // дверь (слева, ближе к жертве)
        double door = ease(seg(u, 2.0, 2.6)) - ease(seg(u, 7.0, 7.5));
        m.box(bf, -1.22, 1.3, 2.3 + door * 1.0, .02, .95, .45, 0, 0, 0, 0, 0x2B2B2B);
        // точки двери и тела
        double[] doorW = bf.w(-1.6, 0, 2.3);
        double vx = s.x, vz = s.z;
        double tx = doorW[0] - vx, tz = doorW[2] - vz;
        double tl = Math.max(0.01, Math.sqrt(tx * tx + tz * tz));
        tx /= tl; tz /= tl;
        double travelYaw = Math.atan2(tx, tz);
        double bodyYaw = Math.atan2(-tx, -tz);   // голова по ходу движения
        // грузчики и тело
        double aRun = seg(u, 2.2, 3.4), aGrab = seg(u, 3.4, 4.2), aCarry = seg(u, 4.2, 6.4), aEnter = seg(u, 6.4, 6.9);
        double headX = vx + tx * 1.35, headZ = vz + tz * 1.35, feetX = vx - tx * 1.35, feetZ = vz - tz * 1.35;
        double corpseX, corpseZ, lift;
        if (u < 4.2) { corpseX = vx; corpseZ = vz; lift = 0; }
        else {
            double q = ease(aCarry);
            corpseX = lerp(vx, doorW[0], q); corpseZ = lerp(vz, doorW[2], q);
            lift = 0.95 * ease(seg(u, 4.2, 4.7));
        }
        boolean corpseVisible = u > 0.3 && u < 6.9;
        // стартовое положение у двери (локально выходят из двери)
        double[] exitW = bf.w(-1.5, 0, 2.3);
        if (corpseVisible) {
            Frame cf = new Frame(corpseX, gy + 0.13 + lift, corpseZ, bodyYaw, -Math.PI / 2);
            figure(m, cf, 0x1FB5B5, 0x3B3FA0, 0xA8744F, 0, 0);
        }
        if (u > 2.2 && u < 6.9) {
            for (int who = 0; who < 2; who++) {
                double dx = who == 0 ? headX : feetX, dz = who == 0 ? headZ : feetZ;
                double px, pz;
                double shift = who * 0.25;
                double r = seg(u, 2.2 + shift, 3.4 + shift);
                if (u < 4.2) { px = lerp(exitW[0], dx, ease(r)); pz = lerp(exitW[2], dz, ease(r)); }
                else {
                    double q = ease(aCarry);
                    double ox = lerp(dx - vx, who == 0 ? tx * 0.0 + (doorW[0] - vx) * 0 : 0, 0);
                    px = dx + (corpseX - vx); pz = dz + (corpseZ - vz);
                }
                // уход в автобус в конце
                if (u > 6.4) { double q = ease(aEnter); px = lerp(px, doorW[0], q); pz = lerp(pz, doorW[2], q); }
                boolean running = u < 3.4 || (u > 4.2 && u < 6.9);
                double sw = running ? Math.sin(t * 13 + who * Math.PI) * 0.8 : 0;
                double arms = u > 3.4 ? -1.35 + (u > 4.2 ? 0 : 0.3 * (1 - aGrab)) : sw;
                double faceYaw = u < 3.4 ? Math.atan2(dx - exitW[0], dz - exitW[2]) : travelYaw;
                if (u > 3.4 && u < 4.2) faceYaw = who == 0 ? travelYaw + Math.PI : travelYaw;   // наклон к телу
                Frame pf = new Frame(px, gy, pz, who == 0 && u > 3.4 && u < 4.2 ? travelYaw : faceYaw, 0);
                int shirt = who == 0 ? 0xE8590C : 0x2F9E44;
                figure(m, pf, shirt, 0x212529, 0xE0AC84, sw, arms);
            }
        }
        m.draw(c);
    }

    // ---------------- FPV-дрон

    private static void fpv(DrawContext c, MinecraftClient mc, Scene s, double t) {
        double D = s.dur / 1000.0;
        double flight = D * 0.72;
        java.util.Random rnd = new java.util.Random(s.seed);
        double ang = rnd.nextDouble() * Math.PI * 2;
        double sx = s.x + Math.cos(ang) * 26, sz = s.z + Math.sin(ang) * 26, sy = s.y + 12;
        double tx = s.x, ty = s.y + 1.0, tz = s.z;
        double u = Math.min(1.0, t / flight);
        double e = Math.pow(u, 1.55);
        double wob = (1 - u) * 0.9;
        double px = lerp(sx, tx, e) + Math.sin(u * 11) * wob * Math.cos(ang + 1.57);
        double pz = lerp(sz, tz, e) + Math.sin(u * 11) * wob * Math.sin(ang + 1.57);
        double py = lerp(sy, ty, e) + Math.sin(u * 9 + 1) * wob * 0.5;
        double u2 = Math.min(1.0, (t + 0.05) / flight);
        double e2 = Math.pow(u2, 1.55), wob2 = (1 - u2) * 0.9;
        double qx = lerp(sx, tx, e2) + Math.sin(u2 * 11) * wob2 * Math.cos(ang + 1.57);
        double qz = lerp(sz, tz, e2) + Math.sin(u2 * 11) * wob2 * Math.sin(ang + 1.57);
        double qy = lerp(sy, ty, e2) + Math.sin(u2 * 9 + 1) * wob2 * 0.5;
        double dx = qx - px, dy = qy - py, dz = qz - pz;
        double yaw = Math.atan2(dx, dz);
        double pitch = -Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 0.6 + 0.35;
        double roll = Math.sin(t * 5) * 0.1;
        if (t < flight) {
            if (Cfg.D.killCam) { osd(c, t, flight); return; }
            Mesh m = new Mesh();
            double S = 2.4;
            Frame f = new Frame(px, py, pz, yaw, pitch);
            m.box(f, 0, 0, 0, .09 * S, .02 * S, .13 * S, 0, 0, 0, 0, 0x1B1B1F);
            m.box(f, 0, .035 * S, -.01 * S, .05 * S, .02 * S, .08 * S, 0, 0, 0, 0, 0xE8590C);
            m.box(f, 0, .0, .14 * S, .025 * S, .022 * S, .025 * S, 0, 0, 0, 0, 0x3B82F6);
            for (int i = 0; i < 4; i++) {
                double sgx = (i & 1) == 0 ? -1 : 1, sgz = (i & 2) == 0 ? -1 : 1;
                double ax = sgx * .16 * S, az = sgz * .16 * S;
                Frame af = new Frame(px, py, pz, yaw, pitch);
                m.box(af, ax / 2, 0, az / 2, Math.abs(ax) / 2 + .01 * S, .008 * S, Math.abs(az) / 2 + .01 * S, 0, 0, 0, 0, 0x2A2A30);
                Frame pr = new Frame(0, 0, 0, yaw + t * 90 * (i % 2 == 0 ? 1 : -1), pitch);
                double[] tip = f.w(ax, .02 * S, az);
                pr.ox = tip[0]; pr.oy = tip[1]; pr.oz = tip[2];
                m.box(pr, 0, 0, 0, .12 * S, .004 * S, .012 * S, 0, 0, 0, 0, 0xCFCFD6);
                m.box(pr, 0, .004 * S, 0, .012 * S, .01 * S, .012 * S, 0, 0, 0, 0, (i < 2) ? 0xFF3B30 : 0x30D158);
            }
            m.draw(c);
            // хвост-след и свет светодиода
            float[] P = new float[3];
            for (int j = 1; j <= 8; j++) {
                double tt = Math.max(0, t - j * 0.04);
                double uu = Math.min(1.0, tt / flight), ee = Math.pow(uu, 1.55), ww = (1 - uu) * 0.9;
                double ex = lerp(sx, tx, ee) + Math.sin(uu * 11) * ww * Math.cos(ang + 1.57);
                double ez = lerp(sz, tz, ee) + Math.sin(uu * 11) * ww * Math.sin(ang + 1.57);
                double ey = lerp(sy, ty, ee) + Math.sin(uu * 9 + 1) * ww * 0.5;
                if (Projector.project(ex, ey, ez, P)) {
                    float ppb = TargetEsp.pxPerBlock(ex, ey, ez);
                    TargetEsp.spr(c, "glow", P[0], P[1], ppb * 0.6f * (1f - j / 9f), 0, Gfx.withAlpha(0xFF6A3D, (int) (140 * (1f - j / 9f))));
                }
            }
            if (Projector.project(px, py, pz, P) && (int) (t * 8) % 2 == 0) {
                float ppb = TargetEsp.pxPerBlock(px, py, pz);
                TargetEsp.spr(c, "glow", P[0], P[1], ppb * 1.0f, 0, Gfx.withAlpha(0xFF2D2D, 200));
            }
        } else {
            double et = t - flight;
            if (!boomed || et < 0.05) {
                if (et >= 0 && !s.equals(null)) explode(mc, s, tx, ty, tz);
            }
            explosion2d(c, tx, ty, tz, et, s.seed);
        }
        if (t >= flight + 1.4) boomed = false;
    }

    /** Экранный интерфейс дрона (вид от первого лица). */
    private static void osd(DrawContext c, double t, double flight) {
        int w = c.getScaledWindowWidth(), h = c.getScaledWindowHeight();
        int col = 0xFFFF3B30;
        c.fill(0, 0, w, 14, 0x99000000);
        c.fill(0, h - 14, w, h, 0x99000000);
        if ((int) (t * 2) % 2 == 0) Gfx.round(c, 10, 4, 6, 6, 3, col);
        Gfx.text(c, "FPV  REC", 22, 3, Gfx.TEXT);
        Gfx.text(c, String.format("ALT %.0f м", Math.max(0, 12 * (1 - t / flight))), w - 70, 3, Gfx.TEXT);
        int cx = w / 2, cy = h / 2;
        c.fill(cx - 8, cy, cx - 2, cy + 1, 0xCCFFFFFF);
        c.fill(cx + 2, cy, cx + 8, cy + 1, 0xCCFFFFFF);
        c.fill(cx, cy - 8, cx + 1, cy - 2, 0xCCFFFFFF);
        c.fill(cx, cy + 2, cx + 1, cy + 8, 0xCCFFFFFF);
    }

    private static final java.util.Set<Long> EXPLODED = new java.util.HashSet<>();

    private static void explode(MinecraftClient mc, Scene s, double x, double y, double z) {
        if (!EXPLODED.add(s.start)) return;
        if (EXPLODED.size() > 40) EXPLODED.clear();
        if (mc.world == null) return;
        var r = mc.world.random;
        mc.world.addParticleClient(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 0, 0, 0);
        for (int i = 0; i < 40; i++)
            mc.world.addParticleClient(ParticleTypes.FLAME, x, y, z, (r.nextDouble() - .5) * .5, (r.nextDouble() - .3) * .4, (r.nextDouble() - .5) * .5);
        for (int i = 0; i < 24; i++)
            mc.world.addParticleClient(ParticleTypes.LARGE_SMOKE, x, y, z, (r.nextDouble() - .5) * .3, r.nextDouble() * .25, (r.nextDouble() - .5) * .3);
        mc.world.playSoundClient(x, y, z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 1.6f, 0.9f, false);
    }

    private static void explosion2d(DrawContext c, double x, double y, double z, double et, long seed) {
        if (et > 1.3) return;
        float[] P = new float[3];
        if (!Projector.project(x, y, z, P)) return;
        float ppb = TargetEsp.pxPerBlock(x, y, z);
        float f = (float) (et / 1.3);
        float a = 1f - f;
        TargetEsp.spr(c, "glow", P[0], P[1], ppb * (2f + 7f * (float) Math.sqrt(f)), 0, Gfx.withAlpha(0xFF8A1F, (int) (230 * a)));
        TargetEsp.spr(c, "glow", P[0], P[1], ppb * (1f + 3.5f * (float) Math.sqrt(f)), 0, Gfx.withAlpha(0xFFE08A, (int) (255 * a)));
        TargetEsp.spr(c, "circle", P[0], P[1], ppb * (1.5f + 9f * f), 0, Gfx.withAlpha(0xFFB04A, (int) (200 * a)));
        java.util.Random rnd = new java.util.Random(seed);
        for (int i = 0; i < 18; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2, el = (rnd.nextDouble() - .2) * 1.2, sp = 2 + rnd.nextDouble() * 5;
            double d = sp * Math.sqrt(et);
            double ex = x + Math.cos(ang) * d, ez = z + Math.sin(ang) * d, ey = y + el * d - et * et * 3.0;
            if (!Projector.project(ex, ey, ez, P)) continue;
            float pp = TargetEsp.pxPerBlock(ex, ey, ez);
            TargetEsp.spr(c, "glow", P[0], P[1], pp * 0.35f * a, 0, Gfx.withAlpha(0xFFC060, (int) (255 * a)));
        }
        if (et < 0.25) c.fill(0, 0, c.getScaledWindowWidth(), c.getScaledWindowHeight(), Gfx.withAlpha(0xFFFFFF, (int) (150 * (1f - et / 0.25))));
    }
}
