package dev.untilits.ui;

import dev.untilits.Cfg;
import dev.untilits.Gfx;
import dev.untilits.SysHud;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

/** Скрытое меню: PlayerESP, NameTags, System HUD. Открывается только после ввода кода. */
public final class SecretScreen extends UiScreen {
    private boolean dragHud;
    private int offX, offY;

    public SecretScreen() { super("Скрытое меню"); }

    @Override
    protected void init() {
        layoutPanel(380, 236);
        items.clear();
        Cfg.Data d = Cfg.D;
        items.add(new Section("PlayerESP — игроки сквозь стены"));
        items.add(new Toggle("PlayerESP", () -> d.esp, v -> d.esp = v));
        items.add(new Slider("Дальность ESP", 16, 512, 16, " бл.", () -> d.espRange, v -> d.espRange = v));
        items.add(new Section("NameTags — ники издалека, даже у крадущихся"));
        items.add(new Toggle("NameTags", () -> d.names, v -> d.names = v));
        items.add(new Slider("Дальность ников", 16, 512, 16, " бл.", () -> d.nameRange, v -> d.nameRange = v));
        items.add(new Toggle("Показывать HP", () -> d.nameHp, v -> d.nameHp = v));
        items.add(new Toggle("Показывать броню и предмет", () -> d.nameItems, v -> d.nameItems = v));
        items.add(new Section("Hud — панель System (перетащи мышью)"));
        items.add(new Toggle("System HUD (FPS / MS / TPS)", () -> d.sysHud, v -> d.sysHud = v));
        place(py + 34);
    }

    @Override
    protected void header(DrawContext c, int mx, int my) {
        Gfx.round(c, px + 16, py + 12, 8, 8, 4, Cfg.D.accent);
        Gfx.shadow(c, "Скрытое меню", px + 30, py + 12, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        int mx = (int) click.x(), my = (int) click.y();
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
