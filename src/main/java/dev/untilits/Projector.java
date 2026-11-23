package dev.untilits;

import dev.untilits.mixin.GameRendererAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/** Проекция мировых точек на экран по реальной камере и реальному FOV (с учётом спринта/эффектов и FreeCam). */
public final class Projector {
    private static double px, py, pz, fx, fy, fz, rx, ry, rz, ux, uy, uz, tanHalf, aspect;
    private static int sw, sh;

    private Projector() { }

    public static double camX() { return px; }
    public static double camY() { return py; }
    public static double camZ() { return pz; }

    public static boolean setup(MinecraftClient mc, float tp, int screenW, int screenH) {
        if (mc.player == null || mc.gameRenderer == null) return false;
        Camera cam = mc.gameRenderer.getCamera();
        if (cam == null || !cam.isReady()) return false;
        sw = screenW; sh = screenH;
        Vec3d cp = cam.getCameraPos();
        px = cp.x; py = cp.y; pz = cp.z;
        double yr = Math.toRadians(cam.getYaw()), pr = Math.toRadians(cam.getPitch());
        fx = -Math.sin(yr) * Math.cos(pr);
        fy = -Math.sin(pr);
        fz = Math.cos(yr) * Math.cos(pr);
        rx = -Math.cos(yr); ry = 0; rz = -Math.sin(yr);
        ux = ry * fz - rz * fy;
        uy = rz * fx - rx * fz;
        uz = rx * fy - ry * fx;
        double fov;
        try {
            fov = ((GameRendererAccessor) (Object) mc.gameRenderer).untilits$getFov(cam, tp, true);
        } catch (Throwable t) {
            fov = mc.options.getFov().getValue();
        }
        if (!(fov > 1.0 && fov < 170.0)) fov = mc.options.getFov().getValue();
        tanHalf = Math.tan(Math.toRadians(fov) / 2.0);
        aspect = sw / (double) Math.max(1, sh);
        return true;
    }

    /** @return true, если точка перед камерой; out[0], out[1] — экранные координаты, out[2] — глубина. */
    public static boolean project(double x, double y, double z, float[] out) {
        double dx = x - px, dy = y - py, dz = z - pz;
        double depth = dx * fx + dy * fy + dz * fz;
        if (depth < 0.1) return false;
        double sx = dx * rx + dy * ry + dz * rz;
        double sy = dx * ux + dy * uy + dz * uz;
        double nx = sx / (depth * tanHalf * aspect);
        double ny = sy / (depth * tanHalf);
        out[0] = (float) ((nx * 0.5 + 0.5) * sw);
        out[1] = (float) ((0.5 - ny * 0.5) * sh);
        out[2] = (float) depth;
        return true;
    }

    /**
     * Как project, но точка за пределами экрана или за спиной прижимается к краю (для меток).
     * @return 0 — на экране, 1 — прижата к краю; out[3] — угол стрелки (рад.) для прижатых.
     */
    public static int projectEdge(double x, double y, double z, float margin, float[] out) {
        double dx = x - px, dy = y - py, dz = z - pz;
        double depth = dx * fx + dy * fy + dz * fz;
        double sx = dx * rx + dy * ry + dz * rz;
        double sy = dx * ux + dy * uy + dz * uz;
        double nx, ny;
        boolean front = depth >= 0.1;
        if (front) {
            nx = sx / (depth * tanHalf * aspect);
            ny = sy / (depth * tanHalf);
        } else {
            double l = Math.hypot(sx, sy);
            if (l < 1e-6) { sx = 0; sy = -1; l = 1; }
            nx = sx / l * 1000.0;
            ny = sy / l * 1000.0;
        }
        out[2] = (float) Math.max(0.0, depth);
        out[3] = (float) Math.atan2(-ny, nx);
        double mx = 1.0 - margin * 2.0 / sw, my = 1.0 - margin * 2.0 / sh;
        if (front && Math.abs(nx) <= mx && Math.abs(ny) <= my) {
            out[0] = (float) ((nx * 0.5 + 0.5) * sw);
            out[1] = (float) ((0.5 - ny * 0.5) * sh);
            return 0;
        }
        double k = Math.max(Math.abs(nx) / mx, Math.abs(ny) / my);
        nx /= k; ny /= k;
        out[0] = (float) ((nx * 0.5 + 0.5) * sw);
        out[1] = (float) ((0.5 - ny * 0.5) * sh);
        return 1;
    }
}
