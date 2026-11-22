package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/** NameTags: ники на любой дистанции, видны сквозь стены и у крадущихся. Слева — предмет в левой руке, друзья — зелёным. */
public final class NameTags {
    private static final float[] P = new float[3];
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};

    private NameTags() { }

    public static void render(DrawContext c, MinecraftClient mc, float tp) {
        if (!Cfg.D.names || mc.world == null || mc.player == null) return;
        double range = Cfg.D.nameRange, r2 = range * range;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            double d2 = p.squaredDistanceTo(mc.player);
            if (d2 > r2) continue;
            Vec3d lp = p.getLerpedPos(tp);
            if (!Projector.project(lp.x, lp.y + p.getHeight() + 0.35, lp.z, P)) continue;
            draw(c, p, P[0], P[1], (float) Math.sqrt(d2));
        }
    }

    private static void draw(DrawContext c, AbstractClientPlayerEntity p, float sx, float sy, float dist) {
        String name = p.getName().getString();
        boolean friend = Friends.is(name);
        String hpText = Math.round(p.getHealth() + p.getAbsorptionAmount()) + "HP";
        boolean gear = Cfg.D.nameItems;

        ItemStack off = gear ? p.getOffHandStack() : ItemStack.EMPTY;
        boolean hasOff = !off.isEmpty();
        int items = 0;
        if (gear) for (EquipmentSlot s : SLOTS) if (!p.getEquippedStack(s).isEmpty()) items++;

        int h = 20, pad = 5, gap = 5;
        int nameW = Gfx.tw(name);
        int hpW = Cfg.D.nameHp ? Gfx.tw(hpText) + 8 : 0;
        int itemsW = items * 17;
        int offW = hasOff ? 17 : 0;
        int w = pad + (offW > 0 ? offW + gap - 1 : 0) + nameW + (hpW > 0 ? gap + hpW : 0) + (itemsW > 0 ? gap + itemsW : 0) + pad;

        float s = Math.max(0.55f, Math.min(1.0f, 1.15f - dist / 220f));
        var m = c.getMatrices();
        m.pushMatrix();
        m.translate(sx, sy);
        m.scale(s, s);

        int x = -w / 2, y = -h;
        Gfx.glass(c, x, y, w, h, 7, false);
        int cx = x + pad;
        if (hasOff) {                      // предмет в левой руке — на месте бывшей «буквы»
            c.drawItem(off, cx, y + (h - 16) / 2);
            cx += offW + gap - 1;
        }
        Gfx.shadow(c, name, cx, y + (h - 8) / 2, friend ? 0xFF55FF55 : 0xFFFFFFFF);
        cx += nameW;
        if (hpW > 0) {
            cx += gap;
            Gfx.round(c, cx, y + 3, hpW, h - 6, 4, 0xCCB8323A, 0xCC8E2229);
            Gfx.text(c, hpText, cx + 4, y + (h - 8) / 2, 0xFFFFE3E3);
            cx += hpW;
        }
        if (itemsW > 0) {
            cx += gap;
            for (EquipmentSlot sl : SLOTS) {
                ItemStack st = p.getEquippedStack(sl);
                if (st.isEmpty()) continue;
                c.drawItem(st, cx, y + (h - 16) / 2);
                cx += 17;
            }
        }
        m.popMatrix();
    }
}
