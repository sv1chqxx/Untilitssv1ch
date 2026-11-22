package dev.untilits.ui;

import dev.untilits.Cfg;
import dev.untilits.Gfx;
import dev.untilits.NoRender;
import dev.untilits.TargetEsp;
import dev.untilits.VisualFx;
import dev.untilits.Waypoints;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Обычное меню (P). По бокам выезжают панели: слева — Метки (waypoints), справа — Эффекты, Predictions, Броня.
 * Внизу справа — еле заметный круг, ведущий к вводу кода.
 */
public final class MainScreen extends UiScreen {
    private static final String[] TABS = {"HUD", "Эффекты", "Визуал", "Таргет ESP", "Оптим.", "Цвета"};
    private static final String[] CORNERS = {"Лево-верх", "Право-верх", "Лево-низ", "Право-низ"};
    private static final int DW = 184;       // ширина боковой панели
    private static final int ROWS = 5;       // строк меток на экране
    private static int tab = 0;
    private static int optPage = 0;
    private static int visPage = 0;
    private static final String[] OPT_PAGES = {"NoRender: интерфейс", "NoRender: мир", "Оптимизация", "Оптимизация 2"};
    private static boolean leftOpen, rightOpen;
    private int scrollL, scrollR;
    private int dw = DW;
    private boolean overlay;

    private float leftA, rightA;
    private long lastNs = System.nanoTime();
    private final List<Item> left = new ArrayList<>();
    private final List<Item> right = new ArrayList<>();
    private TextBox wpName;

    public MainScreen() { super("Untilits"); }

    @Override
    protected void init() {
        layoutPanel(380, 226);
        build();
        buildLeft();
        buildRight();
        relayout();
        leftA = leftOpen ? 1f : 0f;
        rightA = rightOpen ? 1f : 0f;
    }

    // ---------------- основная панель ----------------

    private Toggle nr(String label, int bit) { return new Toggle(label, () -> NoRender.nr(bit), v -> NoRender.setNr(bit, v)); }

    private Toggle opt(String label, int bit) { return new Toggle(label, () -> NoRender.opt(bit), v -> NoRender.setOpt(bit, v)); }

    private void build() {
        items.clear();
        Cfg.Data d = Cfg.D;
        switch (tab) {
            case 0 -> {
                items.add(feat("fps", "FPS"));
                items.add(feat("cps", "CPS (клики в секунду)"));
                items.add(feat("ping", "Пинг"));
                items.add(feat("coords", "Координаты"));
                items.add(feat("keys", "Клавиши (WASD / мышь)"));
                items.add(feat("armor", "Броня и прочность"));
                items.add(new Cycle("Положение HUD", () -> CORNERS[d.hudCorner], () -> d.hudCorner = (d.hudCorner + 1) % 4));
            }
            case 1 -> {
                items.add(new Section("Цифры урона"));
                items.add(feat("damageNumbers", "Показывать цифры урона"));
                items.add(new Slider("Размер цифр", 50, 250, 10, "%", () -> d.dmgScale, v -> d.dmgScale = v));
                items.add(new Section("Кровь"));
                items.add(feat("blood", "Кровь при ударах"));
                items.add(new Slider("Количество крови", 25, 300, 25, "%", () -> d.bloodAmount, v -> d.bloodAmount = v));
            }
            case 2 -> {
                items.add(new Cycle("Раздел", () -> visPage == 0 ? "Ambience" : "Утилиты", () -> { visPage = 1 - visPage; build(); }));
                if (visPage == 0) {
                    items.add(feat("ambOn", "Включить Ambience"));
                    items.add(new Cycle("Погода", () -> VisualFx.WEATHER[d.ambWeather], () -> d.ambWeather = (d.ambWeather + 1) % 4));
                    items.add(feat("ambTimeLock", "Зафиксировать время"));
                    items.add(new Slider("Время суток", 0, 24000, 500, "", () -> d.ambTime, v -> d.ambTime = v));
                    items.add(new Cycle("Тонировка экрана", () -> VisualFx.TINT[d.ambTint], () -> d.ambTint = (d.ambTint + 1) % 8));
                    items.add(feat("ambRain", "Дождь-оверлей на экране"));
                    items.add(new Slider("Плотность дождя", 20, 300, 20, "%", () -> d.rainDensity, v -> d.rainDensity = v));
                } else {
                    items.add(feat("notif", "Уведомления (вкл/выкл функций)"));
                    items.add(feat("fastExp", "FastEXP (бутылки опыта без задержки)"));
                    items.add(feat("cdOn", "Cooldowns (зелье лечения)"));
                    items.add(feat("memOpt", "Memory Optimizer (чистка памяти)"));
                    items.add(feat("killFx", "Kill Effects"));
                    items.add(new Cycle("Эффект убийства", () -> VisualFx.KILL[d.killMode], () -> d.killMode = (d.killMode + 1) % 7));
                }
            }
            case 3 -> {
                items.add(new Section("Эффект вокруг цели"));
                items.add(feat("tgtEsp", "Target ESP"));
                items.add(new Cycle("Режим", () -> TargetEsp.MODES[d.tgtMode], () -> d.tgtMode = (d.tgtMode + 1) % TargetEsp.MODES.length));
                items.add(new Cycle("Скин маркера", () -> TargetEsp.SKINS[d.tgtSkin], () -> d.tgtSkin = (d.tgtSkin + 1) % TargetEsp.SKINS.length));
                items.add(new Slider("Размер", 40, 250, 10, "%", () -> d.tgtSize, v -> d.tgtSize = v));
                items.add(new Slider("Цвет (оттенок)", 0, 360, 10, "°", () -> d.tgtHue, v -> d.tgtHue = v));
                items.add(feat("tgtFlash", "Красная вспышка при ударе"));
                items.add(feat("tgtInfo", "Карточка цели (ник, HP)"));
            }
            case 4 -> {
                items.add(new Cycle("Раздел", () -> OPT_PAGES[optPage], () -> { optPage = (optPage + 1) % 4; build(); }));
                if (optPage == 0) {
                    items.add(nr("Убрать скорборд", NoRender.SCOREBOARD));
                    items.add(nr("Убрать боссбар", NoRender.BOSSBAR));
                    items.add(nr("Убрать титры", NoRender.TITLES));
                    items.add(nr("Убрать виньетку", NoRender.VIGNETTE));
                    items.add(nr("Убрать эффект портала", NoRender.PORTAL));
                    items.add(nr("Убрать анимацию тотема", NoRender.TOTEM));
                } else if (optPage == 1) {
                    items.add(nr("Убрать огонь на экране", NoRender.FIRE));
                    items.add(nr("Убрать размытие под водой", NoRender.WATER));
                    items.add(nr("Убрать дождь и снег", NoRender.WEATHER));
                    items.add(nr("Убрать дым", NoRender.SMOKE));
                    items.add(nr("Убрать частицы ломания блоков", NoRender.BREAK));
                    items.add(nr("Убрать стрелы в игроках", NoRender.ARROWS));
                    items.add(nr("Убрать свечение игроков", NoRender.PGLOW));
                } else if (optPage == 3) {
                    items.add(opt("Simplified Leaves (грани листвы; затем F3+A)", NoRender.O_LEAVES));
                    items.add(opt("Убрать свечение зачарований", NoRender.O_GLINT));
                } else {
                    items.add(opt("Не рисовать предметы на земле", NoRender.O_ITEMS));
                    items.add(opt("Не рисовать шары опыта", NoRender.O_XP));
                    items.add(opt("Не рисовать стойки для брони", NoRender.O_STANDS));
                    items.add(opt("Не рисовать рамки", NoRender.O_FRAMES));
                    items.add(opt("Не рисовать далёкие снаряды", NoRender.O_PROJ));
                    items.add(new Slider("Дальность сущностей (0 — всё)", 0, 256, 16, "", () -> d.optDist, v -> d.optDist = v));
                }
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

    // ---------------- левая панель: метки ----------------

    private void addHere() {
        Cfg.WP w = Waypoints.addHere(MinecraftClient.getInstance(), wpName.text);
        if (w != null) { wpName.text = ""; buildLeft(); scrollL = 1 << 20; }
    }

    private void addLook() {
        Cfg.WP w = Waypoints.addLook(MinecraftClient.getInstance(), wpName.text);
        if (w != null) { wpName.text = ""; buildLeft(); scrollL = 1 << 20; }
    }

    private void buildLeft() {
        String keep = wpName == null ? "" : wpName.text;
        left.clear();
        wpName = new TextBox("Название метки", 22, this::addHere);
        wpName.text = keep;
        left.add(new Section("Новая метка"));
        left.add(wpName);
        left.add(new ButtonRow("+ Здесь", this::addHere, "+ По взгляду", this::addLook));
        int n = Waypoints.list().size();
        left.add(new Section("Метки (" + n + ")"));
        for (int i = 0; i < n; i++) left.add(new WpRow(Waypoints.list().get(i)));
        if (n == 0) left.add(new Hint("Пока пусто — поставь первую метку"));
    }

    private final class WpRow extends Item {
        final Cfg.WP wp;
        WpRow(Cfg.WP wp) { this.wp = wp; h = 26; }

        void draw(DrawContext c, int mx, int my) {
            hover(c, mx, my);
            int col = 0xFF000000 | wp.color;
            Gfx.disc(c, x + 5, y + 8, 4, wp.visible ? col : Gfx.withAlpha(col, 70));
            String name = wp.name;
            while (name.length() > 3 && Gfx.tw(name) > w - 52) name = name.substring(0, name.length() - 1);
            Gfx.text(c, name, x + 14, y + 4, wp.visible ? Gfx.TEXT : Gfx.DIM);
            MinecraftClient mc = MinecraftClient.getInstance();
            String sub = wp.x + " " + wp.y + " " + wp.z;
            if (mc.player != null && wp.dim.equals(Waypoints.dim(mc)))
                sub += "  ·  " + Waypoints.distText(Math.sqrt(mc.player.getEntityPos().squaredDistanceTo(wp.x + .5, wp.y + .5, wp.z + .5)));
            else sub += "  ·  " + (wp.dim.endsWith("nether") ? "Незер" : wp.dim.endsWith("end") ? "Энд" : "др. мир");
            Gfx.text(c, sub, x + 14, y + 15, 0x66FFFFFF);
            // глаз и крестик
            int ex = x + w - 24;
            if (wp.visible) Gfx.round(c, ex, y + 4, 9, 6, 3, Gfx.withAlpha(Cfg.D.accent, 230));
            else Gfx.ring(c, ex, y + 4, 9, 6, 3, 0x66FFFFFF);
            Gfx.text(c, "x", x + w - 8, y + 4, 0xFFFF8A8A);
        }

        boolean click(int mx, int my) {
            if (mx < x + 11) { // цвет по кругу
                int i = 0;
                for (int k = 0; k < Waypoints.PALETTE.length; k++) if (Waypoints.PALETTE[k] == wp.color) i = k;
                wp.color = Waypoints.PALETTE[(i + 1) % Waypoints.PALETTE.length];
            } else if (mx >= x + w - 12) {
                Waypoints.list().remove(wp);
                buildLeft();
            } else if (mx >= x + w - 28) {
                wp.visible = !wp.visible;
            }
            return false;
        }
    }

    private static final class Hint extends Item {
        final String t;
        Hint(String t) { this.t = t; h = 16; }
        void draw(DrawContext c, int mx, int my) { Gfx.text(c, t, x, y + 4, 0x66FFFFFF); }
    }

    // ---------------- правая панель: эффекты, предикты, броня, метки ----------------

    private void buildRight() {
        Cfg.Data d = Cfg.D;
        right.clear();
        right.add(new Section("Potion HUD"));
        right.add(feat("potionHud", "Эффекты на экране"));
        right.add(new Section("Predictions"));
        right.add(feat("predict", "Траектории и падение"));
        right.add(feat("predictFlying", "Чужие жемчуга и стрелы"));
        right.add(new Section("Броня"));
        right.add(feat("armorOver", "Прочность над игроком"));
        right.add(feat("armorWarn", "Предупреждение о поломке"));
        right.add(new Slider("Порог предупреждения", 5, 50, 5, "%", () -> d.armorWarnPct, v -> d.armorWarnPct = v));
        right.add(new Section("Метки"));
        right.add(feat("wpEnabled", "Показывать метки"));
        right.add(feat("wpDeath", "Метка смерти"));
        right.add(new Slider("Дальность (0 — без лимита)", 0, 2000, 50, "", () -> d.wpMaxDist, v -> d.wpMaxDist = v));
    }

    // ---------------- геометрия панелей ----------------

    private static float ease(float a) { return 1f - (1f - a) * (1f - a) * (1f - a); }

    private int contentH(List<Item> list) {
        int h = 40;
        for (Item it : list) h += it.h + 2;
        return h + 8;
    }

    /** Размещает основную панель и решает, хватает ли места боковым (иначе — режим «поверх»). */
    private void relayout() {
        int gap = 6;
        int open = (leftOpen ? 1 : 0) + (rightOpen ? 1 : 0);
        int avail = width - 8;
        dw = DW;
        overlay = false;
        if (open > 0 && pw + open * (dw + gap) > avail) {
            dw = (avail - pw) / open - gap;
            if (dw < 128) { overlay = true; dw = pw; }
        }
        float eL = ease(leftA), eR = ease(rightA);
        if (overlay) {
            px = (width - pw) / 2;
        } else {
            float W = pw + eL * (dw + gap) + eR * (dw + gap);
            px = Math.round((width - W) / 2f + eL * (dw + gap));
        }
        place(py + 52);
    }

    private boolean overlayShown() { return overlay && (leftA > 0.02f || rightA > 0.02f); }

    private int[] box(boolean isLeft, List<Item> list) {
        if (overlay) return new int[]{px, py, pw, ph};
        int dh = Math.min(height - 12, contentH(list));
        int dy = Math.max(6, py + ph / 2 - dh / 2);
        if (dy + dh > height - 6) dy = height - 6 - dh;
        return new int[]{isLeft ? px - 6 - dw : px + pw + 6, dy, dw, dh};
    }

    private int maxScroll(List<Item> list, int[] b) {
        int content = contentH(list) - 40 - 8;
        return Math.max(0, content - (b[3] - 36 - 8));
    }

    private void placeDrawer(List<Item> list, int[] b, int scroll) {
        int y = b[1] + 32 - scroll;
        for (Item it : list) {
            it.x = b[0] + 12;
            it.w = b[2] - 24;
            it.y = y;
            y += it.h + 2;
        }
    }

    private void drawDrawer(DrawContext c, boolean isLeft, List<Item> list, String title, int mx, int my) {
        float anim = ease(isLeft ? leftA : rightA);
        if (anim < 0.01f) return;
        int[] b = box(isLeft, list);
        int off = overlay ? Math.round((1f - anim) * (isLeft ? -b[2] : b[2])) : 0;
        int bx = b[0] + off;
        int[] bb = {bx, b[1], b[2], b[3]};
        Gfx.glass(c, bx, b[1], b[2], b[3], 12, true);
        Gfx.round(c, bx + 12, b[1] + 12, 6, 6, 3, Cfg.D.accent);
        Gfx.shadow(c, title, bx + 24, b[1] + 11, 0xFFFFFFFF);
        // кнопка закрытия
        int cx = bx + b[2] - 22, cy = b[1] + 8;
        boolean hov = mx >= cx && mx < cx + 14 && my >= cy && my < cy + 14;
        Gfx.round(c, cx, cy, 14, 14, 7, hov ? 0x44FFFFFF : 0x20FFFFFF);
        Gfx.text(c, "x", cx + 4, cy + 3, 0xFFFFFFFF);
        int ms = maxScroll(list, b);
        if (isLeft) scrollL = Cfg.clamp(scrollL, 0, ms); else scrollR = Cfg.clamp(scrollR, 0, ms);
        int sc = isLeft ? scrollL : scrollR;
        placeDrawer(list, bb, sc);
        if (!overlay) c.enableScissor(bx, b[1] + 30, bx + b[2], b[1] + b[3] - 4);
        else { c.enableScissor(Math.max(px, bx), b[1] + 30, Math.min(px + pw, bx + b[2]), b[1] + b[3] - 4); }
        for (Item it : list) if (it.y + it.h > b[1] + 28 && it.y < b[1] + b[3]) it.draw(c, mx, my);
        c.disableScissor();
        if (ms > 0) {
            int trackH = b[3] - 40;
            int th = Math.max(12, trackH * trackH / (trackH + ms));
            int ty = b[1] + 32 + (trackH - th) * sc / ms;
            Gfx.round(c, bx + b[2] - 5, ty, 2, th, 1, 0x55FFFFFF);
        }
    }

    private int[] handle(boolean isLeft) {
        int hh = 44;
        return new int[]{isLeft ? px + 2 : px + pw - 11, py + ph / 2 - hh / 2, 9, hh};
    }

    private void drawHandle(DrawContext c, boolean isLeft, int mx, int my) {
        int[] h = handle(isLeft);
        boolean open = isLeft ? leftOpen : rightOpen;
        boolean hov = mx >= h[0] - 2 && mx < h[0] + h[2] + 2 && my >= h[1] && my < h[1] + h[3];
        Gfx.round(c, h[0], h[1], h[2], h[3], 4, open ? Gfx.withAlpha(Cfg.D.accent, 200) : (hov ? 0x40FFFFFF : 0x22FFFFFF));
        String s = isLeft ? (open ? "<" : ">") : (open ? ">" : "<");
        Gfx.text(c, s, h[0] + (h[2] - Gfx.tw(s)) / 2, h[1] + h[3] / 2 - 4, 0xFFFFFFFF);
    }

    private void toggle(boolean isLeft) {
        if (isLeft) { leftOpen = !leftOpen; if (leftOpen && overlay) rightOpen = false; }
        else { rightOpen = !rightOpen; if (rightOpen && overlay) leftOpen = false; }
        // режим поверх определяется заново на следующем кадре; при смене режима закрываем лишнее
        if (leftOpen && rightOpen) {
            int avail = width - 8;
            if (pw + 2 * (DW + 6) > avail && (avail - pw) / 2 - 6 < 128) { if (isLeft) rightOpen = false; else leftOpen = false; }
        }
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        float k = Math.min(1f, dt * 9f);
        leftA += ((leftOpen ? 1f : 0f) - leftA) * k;
        rightA += ((rightOpen ? 1f : 0f) - rightA) * k;
        if (Math.abs(leftA - (leftOpen ? 1f : 0f)) < 0.004f) leftA = leftOpen ? 1f : 0f;
        if (Math.abs(rightA - (rightOpen ? 1f : 0f)) < 0.004f) rightA = rightOpen ? 1f : 0f;
        relayout();

        if (overlayShown() && Math.max(leftA, rightA) > 0.98f) {
            c.fill(0, 0, width, height, 0x5A050810);
        } else {
            super.render(c, mx, my, delta);
        }
        drawDrawer(c, true, left, "Метки", mx, my);
        drawDrawer(c, false, right, "Эффекты и броня", mx, my);
        if (!overlayShown()) {
            drawHandle(c, true, mx, my);
            drawHandle(c, false, mx, my);
        }
    }

    private int tabX(int i) {
        int x = px + 16;
        for (int k = 0; k < i; k++) x += Gfx.tw(TABS[k]) + 14 + 3;
        return x;
    }

    @Override
    protected void header(DrawContext c, int mx, int my) {
        Gfx.round(c, px + 16, py + 12, 8, 8, 4, Cfg.D.accent);
        Gfx.shadow(c, "Untilits", px + 30, py + 12, 0xFFFFFFFF);
        for (int i = 0; i < TABS.length; i++) {
            int bx = tabX(i), bw = Gfx.tw(TABS[i]) + 14, by = py + 28;
            boolean sel = i == tab, hov = mx >= bx && mx < bx + bw && my >= by && my < by + 16;
            Gfx.round(c, bx, by, bw, 16, 8, sel ? Gfx.withAlpha(Cfg.D.accent, 210) : (hov ? 0x26FFFFFF : 0x14FFFFFF));
            Gfx.text(c, TABS[i], bx + 7, by + 4, sel ? 0xFFFFFFFF : Gfx.DIM);
        }
        int cx = px + pw - 11, cy = py + ph - 11;
        boolean near = Math.abs(mx - cx) <= 6 && Math.abs(my - cy) <= 6;
        Gfx.disc(c, cx, cy, 3, near ? 0x30FFFFFF : 0x12FFFFFF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        int b = click.button();
        boolean ovl = overlayShown();
        if (b == 0) {
            // закрытие крестиком
            for (int side = 0; side < 2; side++) {
                boolean isLeft = side == 0;
                if (!(isLeft ? leftOpen : rightOpen)) continue;
                int[] bx = box(isLeft, isLeft ? left : right);
                if (mx >= bx[0] + bx[2] - 22 && mx < bx[0] + bx[2] - 8 && my >= bx[1] + 8 && my < bx[1] + 22) {
                    if (isLeft) leftOpen = false; else rightOpen = false;
                    TextBox.focused = null;
                    return true;
                }
            }
            if (!ovl) {
                for (int side = 0; side < 2; side++) {
                    int[] h = handle(side == 0);
                    if (mx >= h[0] - 2 && mx < h[0] + h[2] + 2 && my >= h[1] && my < h[1] + h[3]) { toggle(side == 0); return true; }
                }
            }
        }
        if (leftOpen && leftA > 0.9f && handleDrawer(left, true, click, scrollL)) return true;
        if (rightOpen && rightA > 0.9f && handleDrawer(right, false, click, scrollR)) return true;
        if (ovl) return true;   // в режиме «поверх» основная панель не кликается
        if (b == 0) {
            for (int i = 0; i < TABS.length; i++) {
                int bx = tabX(i), bw = Gfx.tw(TABS[i]) + 14, by = py + 28;
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

    private boolean handleDrawer(List<Item> list, boolean isLeft, Click click, int scroll) {
        int mx = (int) click.x(), my = (int) click.y();
        int[] bx = box(isLeft, list);
        if (mx < bx[0] || mx >= bx[0] + bx[2] || my < bx[1] || my >= bx[1] + bx[3]) return false;
        bindingId = null;
        TextBox.focused = null;
        if (my >= bx[1] + 30 && my < bx[1] + bx[3] - 4) {
            placeDrawer(list, bx, scroll);
            clickItems(list, mx, my, click.button());
        }
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && bindingId == null && TextBox.focused == null && (leftOpen || rightOpen)) {
            leftOpen = false;
            rightOpen = false;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hx, double vy) {
        for (int side = 0; side < 2; side++) {
            boolean isLeft = side == 0;
            if (!(isLeft ? leftOpen : rightOpen)) continue;
            List<Item> list = isLeft ? left : right;
            int[] b = box(isLeft, list);
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) {
                int ms = maxScroll(list, b);
                int step = vy < 0 ? 18 : -18;
                if (isLeft) scrollL = Cfg.clamp(scrollL + step, 0, ms); else scrollR = Cfg.clamp(scrollR + step, 0, ms);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, hx, vy);
    }
}
