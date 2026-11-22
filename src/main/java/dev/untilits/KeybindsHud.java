package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Таблица кейбиндов: показывает включённые функции, у которых есть назначенная клавиша. */
public final class KeybindsHud {
    public static int bx, by, bw, bh;

    private KeybindsHud() { }

    public static void render(DrawContext c, MinecraftClient mc) {
        if (!Cfg.D.kbHud) return;
        List<String[]> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> e : Cfg.D.binds.entrySet()) {
            Features.F f = Features.ALL.get(e.getKey());
            if (f != null && f.get().getAsBoolean()) rows.add(new String[]{f.label(), Binds.keyName(e.getValue())});
        }
        boolean preview = rows.isEmpty();
        if (preview) {
            if (mc.currentScreen == null) return;
            rows.add(new String[]{"нет включённых", "—"});
        }
        int pad = 7, rowH = 11, head = 16;
        int w = Gfx.tw("Keybinds") + 20;
        for (String[] r : rows) w = Math.max(w, Gfx.tw(r[0]) + 14 + Gfx.tw("[" + r[1] + "]"));
        w += pad * 2;
        int h = head + rows.size() * rowH + 5;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        int x = Cfg.D.kbX < 0 ? sw - w - 8 : Cfg.D.kbX;
        int y = Cfg.D.kbY;
        x = Cfg.clamp(x, 0, Math.max(0, sw - w));
        y = Cfg.clamp(y, 0, Math.max(0, sh - h));
        bx = x; by = y; bw = w; bh = h;
        Gfx.glass(c, x, y, w, h, 8, false);
        Gfx.round(c, x + pad, y + 6, 6, 6, 3, Cfg.D.accent);
        Gfx.shadow(c, "Keybinds", x + pad + 10, y + 5, 0xFFFFFFFF);
        int ty = y + head;
        for (String[] r : rows) {
            Gfx.text(c, r[0], x + pad, ty, preview ? Gfx.DIM : Gfx.TEXT);
            String k = "[" + r[1] + "]";
            Gfx.text(c, k, x + w - pad - Gfx.tw(k), ty, Gfx.withAlpha(Cfg.D.accent, 255));
            ty += rowH;
        }
    }
}
