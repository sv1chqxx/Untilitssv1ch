package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Проекция мировых точек на экран (для NameTags и цифр урона) без доступа к матрицам рендера. */
public final class Projector {
    private static double px, py, pz, fx, fy, fz, rx, ry, rz, ux, uy, uz, tanHalf, aspect;
    private static int sw, sh;

    private Projector() { }

    public static boolean setup(MinecraftClient mc, float tp, int screenW, int screenH) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return false;
        sw = screenW; sh = screenH;
        Vec3d pos = p.getLerpedPos(tp);
        double eye = p.getEyeHeight(p.getPose());
        float yaw = p.getYaw(tp), pitch = p.getPitch(tp);
        Perspective pers = mc.options.getPerspective();
        if (pers == Perspective.THIRD_PERSON_FRONT) { yaw += 180f; pitch = -pitch; }
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        fx = -Math.sin(yr) * Math.cos(pr);
        fy = -Math.sin(pr);
        fz = Math.cos(yr) * Math.cos(pr);
        rx = -Math.cos(yr); ry = 0; rz = -Math.sin(yr);
        ux = ry * fz - rz * fy;
        uy = rz * fx - rx * fz;
        uz = rx * fy - ry * fx;
        px = pos.x; py = pos.y + eye; pz = pos.z;
        if (Freecam.active) {
            Vec3d fp = Freecam.pos(tp);
            px = fp.x; py = fp.y; pz = fp.z;
            yr = Math.toRadians(Freecam.yaw); pr = Math.toRadians(Freecam.pitch);
            fx = -Math.sin(yr) * Math.cos(pr);
            fy = -Math.sin(pr);
            fz = Math.cos(yr) * Math.cos(pr);
            rx = -Math.cos(yr); ry = 0; rz = -Math.sin(yr);
            ux = ry * fz - rz * fy;
            uy = rz * fx - rx * fz;
            uz = rx * fy - ry * fx;
        } else if (pers != Perspective.FIRST_PERSON) {
            double back = 4.0;
            px -= fx * back; py -= fy * back; pz -= fz * back;
        }
        double fov = mc.options.getFov().getValue();
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
}
