package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;

/** ESP: ванильная подсветка (glow) — контур виден сквозь стены. Игроки / мобы / предметы. Вызывается из EntityMixin. */
public final class Esp {
    public static final int HOSTILE = 0xFFE5484D, PASSIVE = 0xFF46C46F, ITEM = 0xFFF2C94C;

    private Esp() { }

    /** 0 — не цель, 1 игрок, 2 враждебный моб, 3 мирный моб, 4 предмет. */
    public static int kind(Entity e) {
        if (e instanceof AbstractClientPlayerEntity) return 1;
        if (e instanceof HostileEntity) return 2;
        if (e instanceof LivingEntity) return 3;
        if (e instanceof ItemEntity) return 4;
        return 0;
    }

    public static final int WEAK = 0xFFFF7A1A;

    public static boolean isWeak(Entity e) {
        if (!Cfg.D.weak || !(e instanceof AbstractClientPlayerEntity p)) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || e == mc.player) return false;
        return p.getHealth() + p.getAbsorptionAmount() <= Cfg.D.weakHp;
    }

    public static int colorOf(int kind) {
        return switch (kind) {
            case 2 -> HOSTILE;
            case 3 -> PASSIVE;
            case 4 -> ITEM;
            default -> Cfg.D.accent;
        } & 0xFFFFFF;
    }

    public static boolean shouldGlow(Entity e) {
        Cfg.Data d = Cfg.D;
        if (!d.espGlow) return false;
        int k = kind(e);
        if (k == 0) return false;
        MinecraftClient mc0 = MinecraftClient.getInstance();
        if (isWeak(e) && mc0.player != null && e.squaredDistanceTo(mc0.player) <= (double) d.espRange * d.espRange) return true;
        boolean on = switch (k) {
            case 1 -> d.esp;
            case 2, 3 -> d.espMobs;
            default -> d.espItems;
        };
        if (!on) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || e == mc.player) return false;
        double r = d.espRange;
        return e.squaredDistanceTo(mc.player) <= r * r;
    }

    public static final int FRIEND = 0x55FF55;

    public static int color(Entity e) {
        if (e instanceof AbstractClientPlayerEntity p && Friends.is(p.getName().getString())) return FRIEND;
        return isWeak(e) ? (WEAK & 0xFFFFFF) : colorOf(kind(e));
    }

    /** Нужно ли рисовать 2D-рамку для сущности (дешёвый ESP). */
    public static boolean boxWanted(Entity e, int k) {
        Cfg.Data d = Cfg.D;
        return switch (k) {
            case 1 -> d.esp || isWeak(e);
            case 2, 3 -> d.espMobs;
            case 4 -> d.espItems;
            default -> false;
        };
    }
}
