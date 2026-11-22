package dev.untilits.ui;

import dev.untilits.Cfg;
import dev.untilits.Freecam;
import dev.untilits.Gfx;
import dev.untilits.SysHud;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

/** Скрытое меню: PlayerESP, NameTags, System HUD. Открывается только после ввода кода. */
public final class SecretScreen extends UiScreen {
    private static int tab;
    private boolean dragHud;
    private int offX, offY;

    public SecretScreen() { super("Скрытое меню"); }

    private static final String[] TABS = {"ESP", "Tracers", "Мир", "Ники и HUD"};
    private static final int[] HEIGHTS = {248, 182, 220, 214};

    @Override
    protected void init() {
        layoutPanel(400, HEIGHTS[tab]);
        items.clear();
        Cfg.Data d = Cfg.D;
        switch (tab) {
            case 0 -> {
                items.add(new Section("ESP — контур сквозь стены"));
                items.add(new Toggle("Игроки", () -> d.esp, v -> d.esp = v));
                items.add(new Toggle("Мобы (враждебные красные, мирные зелёные)", () -> d.espMobs, v -> d.espMobs = v));
                items.add(new Toggle("Предметы", () -> d.espItems, v -> d.espItems = v));
                items.add(new Toggle("Ванильный контур-glow (тяжёлый, режет FPS)", () -> d.espGlow, v -> d.espGlow = v));
                items.add(new Slider("Дальность ESP", 16, 512, 16, " бл.", () -> d.espRange, v -> d.espRange = v));
                items.add(new Section("Слабые места — игроки с низким HP светятся оранжевым"));
                items.add(new Toggle("Подсветка слабых мест", () -> d.weak, v -> d.weak = v));
                items.add(new Slider("Порог HP (≤)", 1, 20, 1, " HP", () -> d.weakHp, v -> d.weakHp = v));
            }
            case 1 -> {
                items.add(new Section("Tracers — линии из прицела к целям"));
                items.add(new Toggle("Tracers (игроки)", () -> d.tracers, v -> d.tracers = v));
                items.add(new Toggle("К мобам", () -> d.tracersMobs, v -> d.tracersMobs = v));
                items.add(new Toggle("К предметам", () -> d.tracersItems, v -> d.tracersItems = v));
                items.add(new Slider("Дальность Tracers", 16, 512, 16, " бл.", () -> d.tracerRange, v -> d.tracerRange = v));
            }
            case 2 -> {
                items.add(new Section("Chest ESP — сундуки, бочки, шалкеры, эндер-сундуки"));
                items.add(new Toggle("Chest ESP", () -> d.chestEsp, v -> d.chestEsp = v));
                items.add(new Toggle("Показывать спавнеры", () -> d.chestSpawners, v -> d.chestSpawners = v));
                items.add(new Slider("Дальность", 8, 128, 8, " бл.", () -> d.chestRange, v -> d.chestRange = v));
                items.add(new Section("FreeCam — свободная камера (клавиша V)"));
                items.add(new Toggle("FreeCam", () -> Freecam.active, v -> Freecam.set(v)));
                items.add(new Slider("Скорость камеры", 1, 30, 1, "", () -> d.freecamSpeed, v -> d.freecamSpeed = v));
            }
            default -> {
                items.add(new Section("NameTags — ники издалека, даже у крадущихся"));
                items.add(new Toggle("NameTags", () -> d.names, v -> d.names = v));
                items.add(new Slider("Дальность ников", 16, 512, 16, " бл.", () -> d.nameRange, v -> d.nameRange = v));
                items.add(new Toggle("Показывать HP", () -> d.nameHp, v -> d.nameHp = v));
                items.add(new Toggle("Показывать броню и предмет", () -> d.nameItems, v -> d.nameItems = v));
                items.add(new Section("Hud — панель System (перетащи мышью)"));
                items.add(new Toggle("System HUD (FPS / MS / TPS)", () -> d.sysHud, v -> d.sysHud = v));
            }
        }
        place(py + 56);
    }

    @Override
    protected void header(DrawContext c, int mx, int my) {
        Gfx.round(c, px + 16, py + 12, 8, 8, 4, Cfg.D.accent);
        Gfx.shadow(c, "Скрытое меню", px + 30, py + 12, 0xFFFFFFFF);
        int x = px + 16;
        for (int i = 0; i < TABS.length; i++) {
            int w = Gfx.tw(TABS[i]) + 16;
            Gfx.round(c, x, py + 30, w, 18, 8, i == tab ? Cfg.D.accent : 0x40FFFFFF);
            Gfx.text(c, TABS[i], x + 8, py + 35, 0xFFFFFFFF);
            x += w + 6;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        if (click.button() == 0 && my >= py + 30 && my < py + 48) {
            int x = px + 16;
            for (int i = 0; i < TABS.length; i++) {
                int w = Gfx.tw(TABS[i]) + 16;
                if (mx >= x && mx < x + w) { if (tab != i) { tab = i; init(); } return true; }
                x += w + 6;
            }
        }
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() == 0 && Cfg.D.sysHud
                && mx >= SysHud.bx && mx < SysHud.bx + SysHud.bw && my >= SysHud.by && my < SysHud.by + SysHud.bh) {
            dragHud = true;
            offX = mx - SysHud.bx;
            offY = my - SysHud.by;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragHud) {
            Cfg.D.sysX = (int) click.x() - offX;
            Cfg.D.sysY = (int) click.y() - offY;
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragHud) { dragHud = false; Cfg.save(); return true; }
        return super.mouseReleased(click);
    }
}
