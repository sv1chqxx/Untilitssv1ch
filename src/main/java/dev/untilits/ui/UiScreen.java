package dev.untilits.ui;

import dev.untilits.Binds;
import dev.untilits.Cfg;
import dev.untilits.Features;
import dev.untilits.Gfx;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
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
    protected Item dragging;
    static String bindingId;   // функция, для которой сейчас ждём клавишу

    protected UiScreen(String title) { super(Text.literal(title)); TextBox.focused = null; bindingId = null; }

    protected Toggle feat(String id, String label) {
        Features.F f = Features.ALL.get(id);
        return new Toggle(label, f.get(), f.set(), id);
    }

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
        if (showBindHint()) Gfx.text(c, bindingId != null ? "Нажми клавишу…  (Esc — отмена, Backspace — снять)" : "СКМ по функции, затем клавиша — привязка", px + 16, py + ph - 13, bindingId != null ? Gfx.withAlpha(Cfg.D.accent, 255) : 0x55FFFFFF);
    }

    protected void header(DrawContext c, int mx, int my) { }

    protected boolean showBindHint() { return true; }

    /** Клик по списку виджетов: ЛКМ — действие, СКМ по Toggle — режим привязки клавиши. */
    protected boolean clickItems(List<Item> list, int mx, int my, int button) {
        for (Item it : list) {
            if (!it.hit(mx, my)) continue;
            if (button == 2) {
                if (it instanceof Toggle t && t.id != null) bindingId = t.id;
                return true;
            }
            if (button == 0) {
                if (it.click(mx, my)) dragging = it;
                Cfg.save();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        bindingId = null;
        TextBox.focused = null;
        if (clickItems(items, mx, my, click.button())) return true;
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int k = input.key();
        if (bindingId != null) {
            String id = bindingId;
            bindingId = null;
            if (k == GLFW.GLFW_KEY_ESCAPE) return true;
            if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) Cfg.D.binds.remove(id);
            else Cfg.D.binds.put(id, k);
            Cfg.save();
            return true;
        }
        TextBox tb = TextBox.focused;
        if (tb != null) {
            if (k == GLFW.GLFW_KEY_ESCAPE) { TextBox.focused = null; return true; }
            if (k == GLFW.GLFW_KEY_BACKSPACE) { if (!tb.text.isEmpty()) tb.text = tb.text.substring(0, tb.text.length() - 1); return true; }
            if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER) { if (tb.onEnter != null) tb.onEnter.run(); return true; }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        TextBox tb = TextBox.focused;
        if (tb != null) {
            if (input.isValidChar() && tb.text.length() < tb.max) tb.text += input.asString();
            return true;
        }
        return super.charTyped(input);
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
        final String label; final BooleanSupplier get; final Consumer<Boolean> set; final String id;
        Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) { this(label, get, set, null); }
        Toggle(String label, BooleanSupplier get, Consumer<Boolean> set, String id) {
            this.label = label; this.get = get; this.set = set; this.id = id;
        }
        void draw(DrawContext c, int mx, int my) {
            hover(c, mx, my);
            boolean binding = id != null && id.equals(bindingId);
            if (binding) Gfx.text(c, "Нажми клавишу…", x, y + (h - 8) / 2, Gfx.withAlpha(Cfg.D.accent, 255));
            else Gfx.text(c, label, x, y + (h - 8) / 2, Gfx.TEXT);
            boolean on = get.getAsBoolean();
            int sx = x + w - 26, sy = y + (h - 12) / 2;
            Gfx.round(c, sx, sy, 26, 12, 6, on ? Gfx.withAlpha(Cfg.D.accent, 235) : 0x55FFFFFF);
            Gfx.ring(c, sx, sy, 26, 12, 6, 0x44FFFFFF);
            Gfx.disc(c, on ? sx + 19 : sx + 7, sy + 6, 4, 0xFFFFFFFF);
            Integer key = id == null ? null : Cfg.D.binds.get(id);
            if (key != null && !binding) {
                String k = "[" + Binds.keyName(key) + "]";
                Gfx.text(c, k, sx - 6 - Gfx.tw(k), y + (h - 8) / 2, Gfx.withAlpha(Cfg.D.accent, 230));
            }
        }
        boolean click(int mx, int my) { set.accept(!get.getAsBoolean()); return false; }
    }

    static final class Button extends Item {
        final String label; final Runnable run;
        Button(String label, Runnable run) { this.label = label; this.run = run; h = 20; }
        void draw(DrawContext c, int mx, int my) {
            boolean hov = hit(mx, my);
            Gfx.round(c, x, y, w, h, 7, hov ? 0x36FFFFFF : 0x1EFFFFFF);
            Gfx.ring(c, x, y, w, h, 7, 0x30FFFFFF);
            Gfx.text(c, label, x + (w - Gfx.tw(label)) / 2, y + (h - 8) / 2, Gfx.TEXT);
        }
        boolean hit(int mx, int my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
        boolean click(int mx, int my) { run.run(); return false; }
    }

    static final class ButtonRow extends Item {
        final String a, b; final Runnable ra, rb;
        ButtonRow(String a, Runnable ra, String b, Runnable rb) { this.a = a; this.ra = ra; this.b = b; this.rb = rb; h = 20; }
        void draw(DrawContext c, int mx, int my) {
            int hw = (w - 4) / 2;
            one(c, x, hw, a, mx, my);
            one(c, x + hw + 4, hw, b, mx, my);
        }
        private void one(DrawContext c, int bx, int bw, String t, int mx, int my) {
            boolean hov = mx >= bx && mx < bx + bw && my >= y && my < y + h;
            Gfx.round(c, bx, y, bw, h, 7, hov ? 0x36FFFFFF : 0x1EFFFFFF);
            Gfx.ring(c, bx, y, bw, h, 7, 0x30FFFFFF);
            Gfx.text(c, t, bx + (bw - Gfx.tw(t)) / 2, y + (h - 8) / 2, Gfx.TEXT);
        }
        boolean click(int mx, int my) {
            int hw = (w - 4) / 2;
            if (mx < x + hw + 2) ra.run(); else rb.run();
            return false;
        }
    }

    /** Поле ввода текста (свой виджет; символы приходят через charTyped экрана). */
    static final class TextBox extends Item {
        static TextBox focused;
        final String placeholder; final int max; final Runnable onEnter;
        String text = "";
        TextBox(String placeholder, int max, Runnable onEnter) { this.placeholder = placeholder; this.max = max; this.onEnter = onEnter; h = 20; }
        void draw(DrawContext c, int mx, int my) {
            boolean f = focused == this;
            Gfx.round(c, x, y, w, h, 7, f ? 0x30FFFFFF : 0x1AFFFFFF);
            Gfx.ring(c, x, y, w, h, 7, f ? Gfx.withAlpha(Cfg.D.accent, 200) : 0x30FFFFFF);
            if (text.isEmpty() && !f) Gfx.text(c, placeholder, x + 8, y + (h - 8) / 2, 0x66FFFFFF);
            else {
                Gfx.text(c, text, x + 8, y + (h - 8) / 2, Gfx.TEXT);
                if (f && (System.currentTimeMillis() / 500) % 2 == 0) c.fill(x + 9 + Gfx.tw(text), y + 5, x + 10 + Gfx.tw(text), y + h - 5, 0xFFFFFFFF);
            }
        }
        boolean hit(int mx, int my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
        boolean click(int mx, int my) { focused = this; return false; }
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
