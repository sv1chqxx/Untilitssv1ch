package dev.untilits.ui;

import dev.untilits.Cfg;
import dev.untilits.Gfx;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Базовый экран Liquid Glass: панель + набор собственных виджетов (без ванильных кнопок). */
abstract class UiScreen extends Screen {
    protected int px, py, pw, ph;
    protected final List<Item> items = new ArrayList<>();
    private Item dragging;

    protected UiScreen(String title) { super(Text.literal(title)); }

    @Override public boolean shouldPause() { return false; }

    /** Ванильный фон (блюр) не используем — рисуем свой затемняющий слой в render(). */
    @Override public void renderBackground(DrawContext c, int mx, int my, float delta) { }

    protected void layoutPanel(int w, int h) {
        pw = Math.min(w, width - 12);
        ph = Math.min(h, height - 12);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    protected void place(int startY) {
        int y = startY;
        for (Item it : items) {
            it.x = px + 16;
            it.w = pw - 32;
            it.y = y;
            y += it.h + 2;
        }
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, 0x5A050810);
        Gfx.glass(c, px, py, pw, ph, 12, true);
        header(c, mx, my);
        for (Item it : items) it.draw(c, mx, my);
    }

    protected void header(DrawContext c, int mx, int my) { }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        if (click.button() == 0) {
            for (Item it : items) {
                if (it.hit(mx, my)) {
                    if (it.click(mx, my)) dragging = it;
                    Cfg.save();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragging != null) { dragging.drag((int) click.x()); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) { dragging = null; Cfg.save(); return true; }
        return super.mouseReleased(click);
    }

    // ---------------- виджеты ----------------

    abstract static class Item {
        int x, y, w, h = 18;
        boolean hit(int mx, int my) { return mx >= x - 4 && mx <= x + w + 4 && my >= y && my < y + h; }
        abstract void draw(DrawContext c, int mx, int my);
        boolean click(int mx, int my) { return false; }
        void drag(int mx) { }
        void hover(DrawContext c, int mx, int my) {
            if (hit(mx, my)) Gfx.round(c, x - 4, y, w + 8, h, 6, 0x12FFFFFF);
        }
    }

    static final class Section extends Item {
        final String t;
        Section(String t) { this.t = t; h = 11; }
        void draw(DrawContext c, int mx, int my) {
            Gfx.text(c, t.toUpperCase(), x, y + 2, Gfx.withAlpha(Cfg.D.accent, 230));
        }
    }

    static final class Toggle extends Item {
        final String label; final BooleanSupplier get; final Consumer<Boolean> set;
        Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) { this.label = label; this.get = get; this.set = set; }
        void draw(DrawContext c, int mx, int my) {
            hover(c, mx, my);
            Gfx.text(c, label, x, y + (h - 8) / 2, Gfx.TEXT);
            boolean on = get.getAsBoolean();
            int sx = x + w - 26, sy = y + (h - 12) / 2;
            Gfx.round(c, sx, sy, 26, 12, 6, on ? Gfx.withAlpha(Cfg.D.accent, 235) : 0x55FFFFFF);
            Gfx.ring(c, sx, sy, 26, 12, 6, 0x44FFFFFF);
            Gfx.disc(c, on ? sx + 19 : sx + 7, sy + 6, 4, 0xFFFFFFFF);
        }
        boolean click(int mx, int my) { set.accept(!get.getAsBoolean()); return false; }
    }

    static final class Cycle extends Item {
        final String label; final Supplier<String> value; final Runnable next;
        Cycle(String label, Supplier<String> value, Runnable next) { this.label = label; this.value = value; this.next = next; }
        void draw(DrawContext c, int mx, int my) {
            hover(c, mx, my);
            Gfx.text(c, label, x, y + (h - 8) / 2, Gfx.TEXT);
            String v = value.get();
            int vw = Gfx.tw(v) + 12;
            Gfx.round(c, x + w - vw, y + 2, vw, h - 4, 5, 0x22FFFFFF);
            Gfx.text(c, v, x + w - vw + 6, y + (h - 8) / 2, Gfx.TEXT);
        }
        boolean click(int mx, int my) { next.run(); return false; }
    }

    static final class Slider extends Item {
        final String label, unit; final int min, max, step; final IntSupplier get; final IntConsumer set;
        Slider(String label, int min, int max, int step, String unit, IntSupplier get, IntConsumer set) {
            this.label = label; this.min = min; this.max = max; this.step = step; this.unit = unit; this.get = get; this.set = set; h = 22;
        }
        int tx() { return x; }
        int tw() { return w; }
        void draw(DrawContext c, int mx, int my) {
            hover(c, mx, my);
            Gfx.text(c, label, x, y + 2, Gfx.TEXT);
            String v = get.getAsInt() + unit;
            Gfx.text(c, v, x + w - Gfx.tw(v), y + 2, Gfx.DIM);
            int ty = y + 15;
            Gfx.round(c, tx(), ty, tw(), 4, 2, 0x44FFFFFF);
            float f = (get.getAsInt() - min) / (float) (max - min);
            int fw = Math.max(4, (int) (tw() * f));
            Gfx.round(c, tx(), ty, fw, 4, 2, Gfx.withAlpha(Cfg.D.accent, 240));
            Gfx.disc(c, tx() + fw, ty + 2, 4, 0xFFFFFFFF);
        }
        boolean click(int mx, int my) { drag(mx); return true; }
        void drag(int mx) {
            float f = Math.max(0f, Math.min(1f, (mx - tx()) / (float) tw()));
            int v = min + Math.round(f * (max - min) / step) * step;
            set.accept(Cfg.clamp(v, min, max));
        }
    }

    static final class Swatches extends Item {
        final String label; final int[] cols; final IntSupplier get; final IntConsumer set;
        Swatches(String label, int[] cols, IntSupplier get, IntConsumer set) { this.label = label; this.cols = cols; this.get = get; this.set = set; h = 20; }
        int sx(int i) { return x + w - (cols.length - i) * 17 + 3; }
        void draw(DrawContext c, int mx, int my) {
            Gfx.text(c, label, x, y + (h - 8) / 2, Gfx.TEXT);
            for (int i = 0; i < cols.length; i++) {
                int bx = sx(i), by = y + (h - 12) / 2;
                Gfx.round(c, bx, by, 12, 12, 6, 0xFF000000 | cols[i]);
                boolean sel = (get.getAsInt() & 0xFFFFFF) == (cols[i] & 0xFFFFFF);
                if (sel) Gfx.ring(c, bx - 2, by - 2, 16, 16, 8, 0xFFFFFFFF);
                else Gfx.ring(c, bx, by, 12, 12, 6, 0x44FFFFFF);
            }
        }
        boolean hit(int mx, int my) { return my >= y && my < y + h && mx >= x; }
        boolean click(int mx, int my) {
            for (int i = 0; i < cols.length; i++) {
                int bx = sx(i);
                if (mx >= bx - 2 && mx < bx + 14) { set.accept(cols[i] & 0xFFFFFF); return false; }
            }
            return false;
        }
    }

    /** Радужная полоса выбора оттенка акцента. */
    static final class HueBar extends Item {
        HueBar() { h = 22; }
        void draw(DrawContext c, int mx, int my) {
            Gfx.text(c, "Оттенок акцента", x, y + 2, Gfx.TEXT);
            int ty = y + 14, th = 5;
            for (int i = 0; i < w; i++) c.fill(x + i, ty, x + i + 1, ty + th, Gfx.hsv(i / (float) w, 0.65f, 1f));
            Gfx.ring(c, x - 1, ty - 1, w + 2, th + 2, 2, 0x44FFFFFF);
            int kx = x + (int) (Gfx.hueOf(Cfg.D.accent) * (w - 1));
            Gfx.disc(c, kx, ty + 2, 4, 0xFFFFFFFF);
            Gfx.disc(c, kx, ty + 2, 3, Cfg.D.accent);
        }
        boolean click(int mx, int my) { drag(mx); return true; }
        void drag(int mx) {
            float h = Math.max(0f, Math.min(1f, (mx - x) / (float) w));
            Cfg.D.accent = Gfx.hsv(h, 0.65f, 1f);
        }
    }
}
