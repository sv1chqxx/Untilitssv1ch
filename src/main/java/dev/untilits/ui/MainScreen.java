package dev.untilits.ui;

import dev.untilits.Cfg;
import dev.untilits.Gfx;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

/** Обычное меню (P). Внизу справа — еле заметный круг, ведущий к вводу кода. */
public final class MainScreen extends UiScreen {
    private static final String[] TABS = {"HUD", "Эффекты", "Цвета"};
    private static final String[] CORNERS = {"Лево-верх", "Право-верх", "Лево-низ", "Право-низ"};
    private static int tab = 0;

    public MainScreen() { super("Untilits"); }

    @Override
    protected void init() {
        layoutPanel(380, 226);
        build();
    }

    private void build() {
        items.clear();
        Cfg.Data d = Cfg.D;
        switch (tab) {
            case 0 -> {
                items.add(new Toggle("FPS", () -> d.fps, v -> d.fps = v));
                items.add(new Toggle("CPS (клики в секунду)", () -> d.cps, v -> d.cps = v));
                items.add(new Toggle("Пинг", () -> d.ping, v -> d.ping = v));
                items.add(new Toggle("Координаты", () -> d.coords, v -> d.coords = v));
                items.add(new Toggle("Клавиши (WASD / мышь)", () -> d.keys, v -> d.keys = v));
                items.add(new Toggle("Броня и прочность", () -> d.armor, v -> d.armor = v));
                items.add(new Cycle("Положение HUD", () -> CORNERS[d.hudCorner], () -> d.hudCorner = (d.hudCorner + 1) % 4));
            }
            case 1 -> {
                items.add(new Section("Цифры урона"));
                items.add(new Toggle("Показывать цифры урона", () -> d.damageNumbers, v -> d.damageNumbers = v));
                items.add(new Slider("Размер цифр", 50, 250, 10, "%", () -> d.dmgScale, v -> d.dmgScale = v));
                items.add(new Section("Кровь"));
                items.add(new Toggle("Кровь при ударах", () -> d.blood, v -> d.blood = v));
                items.add(new Slider("Количество крови", 25, 300, 25, "%", () -> d.bloodAmount, v -> d.bloodAmount = v));
            }
            default -> {
                int[] acc = {0x5B8CFF, 0x45D0FF, 0x37E0A8, 0xB78CFF, 0xFF7AC8, 0xFF6B6B, 0xFFB454, 0xF2F4FA};
                int[] tint = {0x101A2E, 0x0B0E16, 0x1B1230, 0x0E2420, 0x2A1418, 0x2B2F3A};
                items.add(new Section("Акцент"));
                items.add(new Swatches("Готовые цвета", acc, () -> d.accent, v -> d.accent = 0xFF000000 | v));
                items.add(new HueBar());
                items.add(new Section("Liquid Glass"));
                items.add(new Swatches("Тон стекла", tint, () -> d.glass, v -> d.glass = v));
                items.add(new Slider("Прозрачность стекла", 30, 98, 2, "%", () -> d.glassPct, v -> d.glassPct = v));
            }
        }
        place(py + 52);
    }

    private int tabX(int i) {
        int x = px + 16;
        for (int k = 0; k < i; k++) x += Gfx.tw(TABS[k]) + 22 + 4;
        return x;
    }

    @Override
    protected void header(DrawContext c, int mx, int my) {
        Gfx.round(c, px + 16, py + 12, 8, 8, 4, Cfg.D.accent);
        Gfx.shadow(c, "Untilits", px + 30, py + 12, 0xFFFFFFFF);
        for (int i = 0; i < TABS.length; i++) {
            int bx = tabX(i), bw = Gfx.tw(TABS[i]) + 22, by = py + 28;
            boolean sel = i == tab, hov = mx >= bx && mx < bx + bw && my >= by && my < by + 16;
            Gfx.round(c, bx, by, bw, 16, 8, sel ? Gfx.withAlpha(Cfg.D.accent, 210) : (hov ? 0x26FFFFFF : 0x14FFFFFF));
            Gfx.text(c, TABS[i], bx + 11, by + 4, sel ? 0xFFFFFFFF : Gfx.DIM);
        }
        // «скрытая» точка входа: еле заметный круг в правом нижнем углу панели
        int cx = px + pw - 11, cy = py + ph - 11;
        boolean near = Math.abs(mx - cx) <= 6 && Math.abs(my - cy) <= 6;
        Gfx.disc(c, cx, cy, 3, near ? 0x30FFFFFF : 0x12FFFFFF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        if (click.button() == 0) {
            for (int i = 0; i < TABS.length; i++) {
                int bx = tabX(i), bw = Gfx.tw(TABS[i]) + 22, by = py + 28;
                if (mx >= bx && mx < bx + bw && my >= by && my < by + 16) {
                    tab = i;
                    build();
                    return true;
                }
            }
            int cx = px + pw - 11, cy = py + ph - 11;
            if (Math.abs(mx - cx) <= 6 && Math.abs(my - cy) <= 6) {
                client.setScreen(new PasswordScreen());
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
}
