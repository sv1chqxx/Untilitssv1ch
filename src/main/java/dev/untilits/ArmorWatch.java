package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/** Броня: прочность над игроками (иконка + полоска) и предупреждение о скорой поломке / поломке своей брони. */
public final class ArmorWatch {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] NAMES = {"Шлем", "Нагрудник", "Поножи", "Ботинки"};
    private static final float[] P = new float[3];

    private static final int[] lastDur = new int[4];       // остаток прочности на прошлом тике
    private static final boolean[] warned = new boolean[4];
    private static long brokenUntil;
    private static String brokenName = "";

    private ArmorWatch() { }

    public static int remaining(ItemStack s) { return s.getMaxDamage() - s.getDamage(); }

    public static float fraction(ItemStack s) {
        int max = s.getMaxDamage();
        return max <= 0 ? 1f : Math.max(0f, Math.min(1f, (max - s.getDamage()) / (float) max));
    }

    private static int colorOf(float f) {
        return f > 0.5f ? 0xFF55E08A : (f > 0.25f ? 0xFFFFC857 : 0xFFFF5A5A);
    }

    // ---------- своя броня: слежение ----------

    public static void tick(MinecraftClient mc) {
        if (!Cfg.D.armorWarn || mc.player == null) return;
        for (int i = 0; i < 4; i++) {
            ItemStack s = mc.player.getEquippedStack(SLOTS[i]);
            if (s.isEmpty() || s.getMaxDamage() <= 0) {
                // была почти сломана и пропала — считаем, что сломалась
                if (lastDur[i] > 0 && lastDur[i] <= 3) {
                    brokenName = NAMES[i];
                    brokenUntil = System.currentTimeMillis() + 4500;
                    mc.player.playSound(SoundEvents.ENTITY_ITEM_BREAK.value(), 1f, 0.8f);
                }
                lastDur[i] = 0; warned[i] = false;
                continue;
            }
            lastDur[i] = remaining(s);
            boolean low = fraction(s) * 100f <= Cfg.D.armorWarnPct;
            if (low && !warned[i]) mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1f, 0.7f);
            warned[i] = low;
        }
    }

    public static void renderWarning(DrawContext c, MinecraftClient mc) {
        if (!Cfg.D.armorWarn || mc.player == null) return;
        long now = System.currentTimeMillis();
        String text = null; int col = 0xFFFFC857;
        if (now < brokenUntil) { text = "Броня сломалась: " + brokenName; col = 0xFFFF5A5A; }
        else {
            int worst = -1; float wf = 2f;
            for (int i = 0; i < 4; i++) {
                if (!warned[i]) continue;
                float f = fraction(mc.player.getEquippedStack(SLOTS[i]));
                if (f < wf) { wf = f; worst = i; }
            }
            if (worst >= 0) { text = "Броня скоро сломается: " + NAMES[worst] + " " + Math.round(wf * 100) + "%"; col = wf < 0.1f ? 0xFFFF5A5A : 0xFFFFC857; }
        }
        if (text == null) return;
        boolean pulse = (now / 350) % 2 == 0;
        int tw = Gfx.tw(text), w = tw + 34, h = 22;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        int x = (sw - w) / 2, y = (int) (sh * 0.22f);
        Gfx.glass(c, x, y, w, h, 9, false);
        Gfx.ring(c, x, y, w, h, 9, Gfx.withAlpha(col, pulse ? 255 : 120));
        Gfx.disc(c, x + 12, y + h / 2, 5, Gfx.withAlpha(col, pulse ? 255 : 170));
        Gfx.text(c, "!", x + 12 - Gfx.tw("!") / 2 + 1, y + (h - 8) / 2, 0xFF101018);
        Gfx.shadow(c, text, x + 24, y + (h - 8) / 2, 0xFFFFFFFF);
    }

    // ---------- чужая броня над головой ----------

    public static void renderOverhead(DrawContext c, MinecraftClient mc, float tp) {
        if (!Cfg.D.armorOver || mc.world == null || mc.player == null) return;
        for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            double d2 = p.squaredDistanceTo(mc.player);
            if (d2 > 48 * 48) continue;
            Vec3d lp = p.getLerpedPos(tp);
            if (!Projector.project(lp.x, lp.y + p.getHeight() + 0.35, lp.z, P)) continue;
            float s = Math.max(0.6f, Math.min(1.0f, 1.1f - (float) Math.sqrt(d2) / 90f));
            // над ником, если включены NameTags
            float lift = Cfg.D.names ? 26f : 4f;
            int w = 4 * 20 + 6, h = 36;
            var m = c.getMatrices();
            m.pushMatrix();
            m.translate(P[0], P[1] - lift * s);
            m.scale(s, s);
            int x = -w / 2, y = -h;
            Gfx.glass(c, x, y, w, h, 7, false);
            for (int i = 0; i < 4; i++) {
                ItemStack st = p.getEquippedStack(SLOTS[i]);
                int ix = x + 5 + i * 20;
                if (st.isEmpty()) {
                    Gfx.round(c, ix + 2, y + 5, 12, 12, 4, 0x22FFFFFF);
                    continue;
                }
                c.drawItem(st, ix, y + 3);
                float f = fraction(st);
                int bw = 16;
                Gfx.round(c, ix, y + 21, bw, 3, 1, 0x44FFFFFF);
                if (st.getMaxDamage() > 0) {
                    Gfx.round(c, ix, y + 21, Math.max(2, (int) (bw * f)), 3, 1, colorOf(f));
                    if (f < 0.34f) Gfx.text(c, Math.round(f * 100) + "", ix + (bw - Gfx.tw(Math.round(f * 100) + "")) / 2, y + 25, 0xFFFF7A7A);
                } else {
                    Gfx.round(c, ix, y + 21, bw, 3, 1, 0xFF8FA0FF);
                }
            }
            m.popMatrix();
        }
    }
}
