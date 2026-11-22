package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Всплывающие уведомления справа сверху (вкл/выкл функций, предупреждения). */
public final class Notif {
    private record N(String text, boolean on, long at) { }
    private static final List<N> LIST = new ArrayList<>();
    private static final long LIFE = 2600;

    private Notif() { }

    public static void push(String text, boolean on) {
        if (!Cfg.D.notif) return;
        LIST.add(new N(text, on, System.currentTimeMillis()));
        if (LIST.size() > 5) LIST.remove(0);
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        if (LIST.isEmpty() || !Cfg.D.notif) return;
        long now = System.currentTimeMillis();
        LIST.removeIf(n -> now - n.at > LIFE);
        int sw = c.getScaledWindowWidth();
        int y = 8;
        for (int i = LIST.size() - 1; i >= 0; i--) {
            N n = LIST.get(i);
            long age = now - n.at;
            float in = Math.min(1f, age / 220f), out = Math.min(1f, (LIFE - age) / 300f);
            float a = Math.min(in, out);
            int w = Gfx.tw(n.text) + 30, h = 20;
            int slide = (int) ((1f - in) * 30);
            int x = sw - w - 8 + slide;
            var m = c.getMatrices();
            Gfx.glass(c, x, y, w, h, 9, true);
            Gfx.disc(c, x + 11, y + 10, 3, Gfx.withAlpha(n.on ? 0x55FF77 : 0xFF6B6B, (int) (255 * a)));
            Gfx.text(c, n.text, x + 20, y + 6, Gfx.withAlpha(0xE9EEFB, (int) (255 * a)));
            y += h + 4;
        }
    }
}
