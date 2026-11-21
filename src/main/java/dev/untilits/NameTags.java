package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/** NameTags: ники на любой дистанции, видны сквозь стены и у крадущихся; HP и предметы как на референсе. */
public final class NameTags {
    private static final float[] P = new float[3];
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};

    private NameTags() { }

    public static void render(DrawContext c, MinecraftClient mc, float tp) {
        if (!Cfg.D.names || mc.world == null || mc.player == null) return;
        double range = Cfg.D.nameRange;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            double dist = Math.sqrt(p.squaredDistanceTo(mc.player));
            if (dist > range) continue;
            Vec3d lp = p.getLerpedPos(tp);
            if (!Projector.project(lp.x, lp.y + p.getHeight() + 0.35, lp.z, P)) continue;
            draw(c, p, P[0], P[1], (float) dist);
        }
    }

    private static void draw(DrawContext c, AbstractClientPlayerEntity p, float sx, float sy, float dist) {
        String name = p.getName().getString();
        int hp = Math.round(p.getHealth() + p.getAbsorptionAmount());
        String hpText = hp + "HP";

        int items = 0;
        ItemStack[] st = new ItemStack[SLOTS.length];
        if (Cfg.D.nameItems) {
            for (int i = 0; i < SLOTS.length; i++) {
                st[i] = p.getEquippedStack(SLOTS[i]);
                if (!st[i].isEmpty()) items++;
            }
        }

        int h = 20, pad = 5, gap = 5;
        int avatar = 12;
        int nameW = Gfx.tw(name);
        int hpW = Cfg.D.nameHp ? Gfx.tw(hpText) + 8 : 0;
        int itemsW = items * 17;
        int w = pad + avatar + gap + nameW + (hpW > 0 ? gap + hpW : 0) + (itemsW > 0 ? gap + itemsW : 0) + pad;

        float s = Math.max(0.55f, Math.min(1.0f, 1.15f - dist / 220f));
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(sx, sy);
        m.scale(s, s);

        int x = -w / 2, y = -h;
        Gfx.glass(c, x, y, w, h, 7, false);
        int cx = x + pad;
        // «аватар»: квадрат цвета акцента с первой буквой
        Gfx.round(c, cx, y + (h - avatar) / 2, avatar, avatar, 4, Cfg.D.accent);
        String first = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        Gfx.text(c, first, cx + (avatar - Gfx.tw(first)) / 2 + 1, y + (h - 8) / 2, 0xFFFFFFFF);
        cx += avatar + gap;
        Gfx.shadow(c, name, cx, y + (h - 8) / 2, 0xFFFFFFFF);
        cx += nameW;
        if (hpW > 0) {
            cx += gap;
            Gfx.round(c, cx, y + 3, hpW, h - 6, 4, 0xCCB8323A, 0xCC8E2229);
            Gfx.text(c, hpText, cx + 4, y + (h - 8) / 2, 0xFFFFE3E3);
            cx += hpW;
        }
        if (itemsW > 0) {
            cx += gap;
            for (int i = 0; i < SLOTS.length; i++) {
                if (st[i] == null || st[i].isEmpty()) continue;
                c.drawItem(st[i], cx, y + (h - 16) / 2);
                cx += 17;
            }
        }
        m.popMatrix();
    }
}
