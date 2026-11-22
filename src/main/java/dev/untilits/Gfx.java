package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Рисование «жидкого стекла» из обычных fill-ов: скруглённые панели, блик, рамка. */
public final class Gfx {
    public static final int TEXT = 0xFFE9EEFB;
    public static final int DIM = 0xFF8A93A8;

    private Gfx() { }

    public static int withAlpha(int c, int a) { return (Math.max(0, Math.min(255, a)) << 24) | (c & 0xFFFFFF); }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 255) + ((((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t));
        int r = (int) (((c1 >> 16) & 255) + ((((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t));
        int g = (int) (((c1 >> 8) & 255) + ((((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t));
        int b = (int) ((c1 & 255) + (((c2 & 255) - (c1 & 255)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int inset(int dy, int r) {
        if (r <= 0) return 0;
        double d = r - dy - 0.5;
        return r - (int) Math.round(Math.sqrt(Math.max(0, r * r - d * d)));
    }

    /** Скруглённый прямоугольник с вертикальным градиентом (рисуется несколькими крупными заливками, а не построчно). */
    public static void round(DrawContext c, int x, int y, int w, int h, int r, int top, int bot) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        boolean solid = top == bot;
        int mid = h - 2 * r;
        // середина: один блок или несколько полос градиента
        if (mid > 0) {
            int n = solid ? 1 : Math.min(8, mid);
            for (int k = 0; k < n; k++) {
                int i0 = r + mid * k / n, i1 = r + mid * (k + 1) / n;
                if (i1 <= i0) continue;
                int col = solid ? top : lerp(top, bot, h <= 1 ? 0f : ((i0 + i1) * 0.5f) / (h - 1));
                if ((col >>> 24) != 0) c.fill(x, y + i0, x + w, y + i1, col);
            }
        }
        // углы: группируем соседние строки с одинаковым отступом
        for (int pass = 0; pass < 2; pass++) {
            int i = 0;
            while (i < r) {
                int in = inset(i, r), j = i + 1;
                while (j < r && inset(j, r) == in) j++;
                int row0 = pass == 0 ? i : h - j, row1 = pass == 0 ? j : h - i;
                int col = solid ? top : lerp(top, bot, h <= 1 ? 0f : ((row0 + row1) * 0.5f) / (h - 1));
                if ((col >>> 24) != 0) c.fill(x + in, y + row0, x + w - in, y + row1, col);
                i = j;
            }
        }
    }

    public static void round(DrawContext c, int x, int y, int w, int h, int r, int col) { round(c, x, y, w, h, r, col, col); }

    /** Контур скруглённого прямоугольника толщиной 1px. */
    public static void ring(DrawContext c, int x, int y, int w, int h, int r, int col) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        int mid = h - 2 * r;
        if (mid > 0) {
            c.fill(x, y + r, x + 1, y + h - r, col);
            c.fill(x + w - 1, y + r, x + w, y + h - r, col);
        }
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < r; i++) {
                int in = inset(i, r);
                int row = pass == 0 ? i : h - 1 - i;
                if (i == 0) { c.fill(x + in, y + row, x + w - in, y + row + 1, col); continue; }
                int seg = Math.max(1, inset(i - 1, r) - in);
                c.fill(x + in, y + row, x + in + seg, y + row + 1, col);
                c.fill(x + w - in - seg, y + row, x + w - in, y + row + 1, col);
            }
        }
    }

    public static void disc(DrawContext c, int cx, int cy, int r, int col) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt(Math.max(0, r * r + r - dy * dy)));
            c.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, col);
        }
    }

    /** Панель liquid glass: тень, градиентная заливка тоном, блик сверху, светлая рамка. */
    public static void glass(DrawContext c, int x, int y, int w, int h, int r, boolean shadow) {
        int tint = Cfg.D.glass & 0xFFFFFF;
        int al = Cfg.glassAlpha();
        if (shadow) {
            round(c, x - 2, y - 1, w + 4, h + 5, r + 2, 0x14000000, 0x22000000);
            round(c, x - 1, y, w + 2, h + 3, r + 1, 0x18000000, 0x28000000);
        }
        round(c, x, y, w, h, r, withAlpha(lerp(tint, 0x6E7FA8, 0.28f), al), withAlpha(tint, (int) (al * 0.92f)));
        round(c, x + 2, y + 1, w - 4, Math.max(2, h / 2), Math.max(1, r - 2), 0x28FFFFFF, 0x00FFFFFF);
        ring(c, x, y, w, h, r, 0x46FFFFFF);
    }

    // ---- текст ----
    public static int tw(String s) { return MinecraftClient.getInstance().textRenderer.getWidth(s); }

    public static void text(DrawContext c, String s, int x, int y, int col) {
        c.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, col, false);
    }

    public static void shadow(DrawContext c, String s, int x, int y, int col) {
        c.drawText(MinecraftClient.getInstance().textRenderer, s, x, y, col, true);
    }

    // ---- цвет ----
    public static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        int i = (int) (h * 6f);
        float f = h * 6f - i, p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static float hueOf(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        if (d < 1e-4f) return 0f;
        float h;
        if (max == r) h = ((g - b) / d) % 6f;
        else if (max == g) h = (b - r) / d + 2f;
        else h = (r - g) / d + 4f;
        h /= 6f;
        return h < 0 ? h + 1f : h;
    }

    /** Маленькая 7x7 иконка из битовой карты. */
    public static void icon(DrawContext c, int x, int y, String[] bmp, int col) {
        for (int j = 0; j < bmp.length; j++)
            for (int i = 0; i < bmp[j].length(); i++)
                if (bmp[j].charAt(i) == '#') c.fill(x + i, y + j, x + i + 1, y + j + 1, col);
    }

    public static final String[] ICON_SYSTEM = {"...#...", "...#...", "..###..", "#######", "..###..", "...#...", "...#..."};
    public static final String[] ICON_FPS = {"....##.", "...##..", "..##...", ".#####.", "...##..", "..##...", ".##...."};
    public static final String[] ICON_PING = {"......#", "......#", "....#.#", "....#.#", "..#.#.#", "..#.#.#", "#.#.#.#"};
    public static final String[] ICON_TPS = {".#####.", "#.....#", ".#####.", "#.....#", ".#####.", "#.....#", ".#####."};
}
