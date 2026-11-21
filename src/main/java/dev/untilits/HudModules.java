package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Обычный HUD (аналог KronHUD): FPS, CPS, координаты, пинг, клавиши, броня — в стиле стекла. */
public final class HudModules {
    private static final ArrayDeque<Long> LMB = new ArrayDeque<>();
    private static final ArrayDeque<Long> RMB = new ArrayDeque<>();
    private static boolean lWas, rWas;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private interface Block { int w(); int h(); void draw(DrawContext c, int x, int y); }

    private HudModules() { }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null) return;
        boolean l = mc.options.attackKey.isPressed(), r = mc.options.useKey.isPressed();
        long now = System.currentTimeMillis();
        if (l && !lWas) LMB.add(now);
        if (r && !rWas) RMB.add(now);
        lWas = l; rWas = r;
        while (!LMB.isEmpty() && now - LMB.peekFirst() > 1000) LMB.pollFirst();
        while (!RMB.isEmpty() && now - RMB.peekFirst() > 1000) RMB.pollFirst();
    }

    private static Block pill(String label, String value) {
        return new Block() {
            public int w() { return 6 + Gfx.tw(label) + 4 + Gfx.tw(value) + 6; }
            public int h() { return 15; }
            public void draw(DrawContext c, int x, int y) {
                Gfx.glass(c, x, y, w(), h(), 6, false);
                Gfx.text(c, label, x + 6, y + 4, Gfx.DIM);
                Gfx.text(c, value, x + 6 + Gfx.tw(label) + 4, y + 4, Gfx.TEXT);
            }
        };
    }

    private static Block keys(MinecraftClient mc) {
        return new Block() {
            final int k = 20, g = 2;
            public int w() { return k * 3 + g * 2; }
            public int h() { return k * 3 + g * 2 - 4; }
            void key(DrawContext c, int x, int y, int kw, int kh, String s, boolean down) {
                Gfx.glass(c, x, y, kw, kh, 5, false);
                if (down) Gfx.round(c, x + 1, y + 1, kw - 2, kh - 2, 4, Gfx.withAlpha(Cfg.D.accent, 150));
                Gfx.text(c, s, x + (kw - Gfx.tw(s)) / 2, y + (kh - 8) / 2, down ? 0xFFFFFFFF : Gfx.TEXT);
            }
            public void draw(DrawContext c, int x, int y) {
                key(c, x + k + g, y, k, k, "W", mc.options.forwardKey.isPressed());
                key(c, x, y + k + g, k, k, "A", mc.options.leftKey.isPressed());
                key(c, x + k + g, y + k + g, k, k, "S", mc.options.backKey.isPressed());
                key(c, x + 2 * (k + g), y + k + g, k, k, "D", mc.options.rightKey.isPressed());
                int bw = (w() - g) / 2, by = y + 2 * (k + g);
                key(c, x, by, bw, k - 4, "ЛКМ", mc.options.attackKey.isPressed());
                key(c, x + bw + g, by, bw, k - 4, "ПКМ", mc.options.useKey.isPressed());
            }
        };
    }

    private static Block armor(MinecraftClient mc) {
        List<ItemStack> list = new ArrayList<>();
        for (EquipmentSlot s : ARMOR) {
            ItemStack st = mc.player.getEquippedStack(s);
            if (!st.isEmpty()) list.add(st);
        }
        if (list.isEmpty()) return null;
        return new Block() {
            public int w() { return 6 + 16 + 4 + 28 + 6; }
            public int h() { return list.size() * 17 + 4; }
            public void draw(DrawContext c, int x, int y) {
                Gfx.glass(c, x, y, w(), h(), 6, false);
                int yy = y + 2;
                for (ItemStack st : list) {
                    c.drawItem(st, x + 6, yy);
                    String t = st.isDamageable() ? String.valueOf(st.getMaxDamage() - st.getDamage()) : "";
                    Gfx.text(c, t, x + 6 + 16 + 4, yy + 4, Gfx.TEXT);
                    yy += 17;
                }
            }
        };
    }

    private static String facing(float yaw) {
        String[] d = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int i = Math.round((((yaw % 360f) + 360f) % 360f) / 45f) % 8;
        return d[i];
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        if (mc.player == null) return;
        Cfg.Data d = Cfg.D;
        List<Block> blocks = new ArrayList<>();
        if (d.fps) blocks.add(pill("FPS", String.valueOf(mc.getCurrentFps())));
        if (d.cps) blocks.add(pill("CPS", LMB.size() + " | " + RMB.size()));
        if (d.ping) blocks.add(pill("Пинг", SysHud.ping(mc) + " мс"));
        if (d.coords) {
            blocks.add(pill("XYZ", mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ()
                    + "  " + facing(mc.player.getYaw())));
        }
        if (d.keys) blocks.add(keys(mc));
        if (d.armor) { Block a = armor(mc); if (a != null) blocks.add(a); }
        if (blocks.isEmpty()) return;

        int gap = 3, margin = 6, total = 0;
        for (Block b : blocks) total += b.h() + gap;
        total -= gap;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        boolean right = d.hudCorner == 1 || d.hudCorner == 3;
        boolean bottom = d.hudCorner >= 2;
        int y = bottom ? sh - margin - total - 30 : margin;
        for (Block b : blocks) {
            int x = right ? sw - margin - b.w() : margin;
            b.draw(c, x, y);
            y += b.h() + gap;
        }
    }
}
