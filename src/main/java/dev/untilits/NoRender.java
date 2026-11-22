package dev.untilits;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.client.MinecraftClient;

/** NoRender (убрать лишние эффекты) и Optimizations (отсечение сущностей). Флаги — битовые маски в конфиге. */
public final class NoRender {
    public static final int SCOREBOARD = 0, BOSSBAR = 1, TITLES = 2, VIGNETTE = 3, PORTAL = 4, TOTEM = 5, FIRE = 6, WATER = 7, WEATHER = 8, SMOKE = 9, BREAK = 10, ARROWS = 11, PGLOW = 12;
    public static final int O_ITEMS = 0, O_XP = 1, O_STANDS = 2, O_FRAMES = 3, O_PROJ = 4;

    private NoRender() { }

    public static boolean nr(int bit) { return (Cfg.D.nrMask >> bit & 1) != 0; }
    public static void setNr(int bit, boolean v) { Cfg.D.nrMask = v ? Cfg.D.nrMask | (1 << bit) : Cfg.D.nrMask & ~(1 << bit); }
    public static boolean opt(int bit) { return (Cfg.D.optMask >> bit & 1) != 0; }
    public static void setOpt(int bit, boolean v) { Cfg.D.optMask = v ? Cfg.D.optMask | (1 << bit) : Cfg.D.optMask & ~(1 << bit); }

    /** true — сущность рисовать не нужно. */
    public static boolean cull(Entity e) {
        int m = Cfg.D.optMask;
        if (m == 0 && Cfg.D.optDist == 0) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || e == mc.player || e instanceof net.minecraft.entity.player.PlayerEntity) return false;
        if ((m & 1) != 0 && e instanceof ItemEntity) return true;
        if ((m >> 1 & 1) != 0 && e instanceof ExperienceOrbEntity) return true;
        if ((m >> 2 & 1) != 0 && e instanceof ArmorStandEntity) return true;
        if ((m >> 3 & 1) != 0 && e instanceof ItemFrameEntity) return true;
        double d2 = e.squaredDistanceTo(mc.player);
        if ((m >> 4 & 1) != 0 && e instanceof ProjectileEntity && d2 > 32 * 32) return true;
        int dist = Cfg.D.optDist;
        return dist > 0 && d2 > (double) dist * dist;
    }
}
