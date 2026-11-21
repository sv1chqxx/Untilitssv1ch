package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;

/** PlayerESP: используем ванильную подсветку (glow) — контур игроков виден сквозь стены. Вызывается из EntityMixin. */
public final class Esp {
    private Esp() { }

    public static boolean shouldGlow(Entity e) {
        if (!Cfg.D.esp) return false;
        if (!(e instanceof AbstractClientPlayerEntity)) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || e == mc.player) return false;
        double r = Cfg.D.espRange;
        return e.squaredDistanceTo(mc.player) <= r * r;
    }

    public static int color() { return Cfg.D.accent & 0xFFFFFF; }
}
