package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** FreeCam: камера летает отдельно, персонаж стоит на месте. Включение — клавиша V или меню. */
public final class Freecam {
    public static boolean active;
    public static float yaw, pitch;
    private static double x, y, z, px, py, pz;

    private Freecam() { }

    public static void set(boolean on) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (on) {
            if (mc.player == null) return;
            Vec3d e = mc.player.getEyePos();
            x = px = e.x; y = py = e.y; z = pz = e.z;
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
        }
        active = on;
    }

    public static Vec3d pos(float tp) {
        return new Vec3d(MathHelper.lerp(tp, px, x), MathHelper.lerp(tp, py, y), MathHelper.lerp(tp, pz, z));
    }

    public static void look(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
    }

    public static void tick(MinecraftClient mc) {
        if (!active) return;
        if (mc.player == null || mc.world == null) { active = false; return; }
        px = x; py = y; pz = z;
        if (mc.currentScreen != null) return;
        GameOptions o = mc.options;
        double sp = Cfg.D.freecamSpeed * 0.1 * (o.sprintKey.isPressed() ? 2.0 : 1.0);
        double yr = Math.toRadians(yaw);
        double fx = -Math.sin(yr), fz = Math.cos(yr);   // вперёд
        double rx = -Math.cos(yr), rz = -Math.sin(yr);  // вправо
        double mx = 0, my = 0, mz = 0;
        if (o.forwardKey.isPressed()) { mx += fx; mz += fz; }
        if (o.backKey.isPressed()) { mx -= fx; mz -= fz; }
        if (o.rightKey.isPressed()) { mx -= rx; mz -= rz; }
        if (o.leftKey.isPressed()) { mx += rx; mz += rz; }
        if (o.jumpKey.isPressed()) my += 1;
        if (o.sneakKey.isPressed()) my -= 1;
        double len = Math.sqrt(mx * mx + mz * mz);
        if (len > 0) { mx /= len; mz /= len; }
        x += mx * sp; y += my * sp; z += mz * sp;
    }
}
