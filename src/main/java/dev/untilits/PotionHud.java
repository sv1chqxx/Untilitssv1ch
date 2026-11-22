package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Potion HUD: список эффектов в стеклянной панели — цвет эффекта, название и уровень, время и полоска остатка. */
public final class PotionHud {
    private static final String[] ROMAN = {"", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
    private static final Map<RegistryEntry<StatusEffect>, Integer> MAX = new HashMap<>();

    private PotionHud() { }

    private static String time(int ticks) {
        int s = ticks / 20;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        if (!Cfg.D.potionHud || mc.player == null) return;
        var all = mc.player.getStatusEffects();
        if (all.isEmpty()) { MAX.clear(); return; }
        List<StatusEffectInstance> list = new ArrayList<>(all);
        list.sort((a, b) -> Integer.compare(b.getDuration(), a.getDuration()));
        MAX.keySet().removeIf(k -> list.stream().noneMatch(i -> i.getEffectType().equals(k)));

        int rowH = 24, pad = 8, w = 0;
        String[] names = new String[list.size()];
        String[] times = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            StatusEffectInstance e = list.get(i);
            int amp = e.getAmplifier();
            names[i] = e.getEffectType().value().getName().getString() + (amp > 0 && amp < ROMAN.length ? " " + ROMAN[amp] : (amp >= ROMAN.length ? " " + (amp + 1) : ""));
            times[i] = e.isInfinite() ? "∞" : time(e.getDuration());
            w = Math.max(w, 14 + Gfx.tw(names[i]) + 12 + Gfx.tw(times[i]));
        }
        w = Math.max(w + pad * 2, 120);
        int h = list.size() * rowH + 10;
        int sh = c.getScaledWindowHeight();
        int x = 8, y = Math.max(8, sh / 2 - h / 2);

        Gfx.glass(c, x, y, w, h, 9, false);
        int ty = y + 5;
        for (int i = 0; i < list.size(); i++) {
            StatusEffectInstance e = list.get(i);
            int dur = e.getDuration();
            int mx = MAX.merge(e.getEffectType(), dur, Math::max);
            int col = 0xFF000000 | (e.getEffectType().value().getColor() & 0xFFFFFF);
            boolean bad = e.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL;
            Gfx.disc(c, x + pad + 3, ty + 8, 4, col);
            Gfx.ring(c, x + pad - 1, ty + 4, 9, 9, 4, bad ? 0xAAFF6B6B : 0x55FFFFFF);
            Gfx.shadow(c, names[i], x + pad + 14, ty + 4, bad ? 0xFFFFB3B3 : 0xFFFFFFFF);
            boolean blink = !e.isInfinite() && dur < 200 && (System.currentTimeMillis() / 300) % 2 == 0;
            int tc = blink ? 0xFFFF6B6B : Gfx.DIM;
            Gfx.text(c, times[i], x + w - pad - Gfx.tw(times[i]), ty + 4, tc);
            // полоска остатка
            int bx = x + pad + 14, bw = w - pad * 2 - 14, by = ty + 15;
            Gfx.round(c, bx, by, bw, 3, 1, 0x33FFFFFF);
            float f = e.isInfinite() ? 1f : Math.max(0f, Math.min(1f, dur / (float) Math.max(1, mx)));
            Gfx.round(c, bx, by, Math.max(2, (int) (bw * f)), 3, 1, col);
            ty += rowH;
        }
    }
}
