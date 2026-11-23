package dev.untilits.ui;

import dev.untilits.Binds;
import dev.untilits.CombatFx;
import dev.untilits.Cfg;
import dev.untilits.Features;
import dev.untilits.Gfx;
import dev.untilits.Notif;
import dev.untilits.NoRender;
import dev.untilits.TargetEsp;
import dev.untilits.VisualFx;
import dev.untilits.Waypoints;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Обычное меню (P). Три вкладки (Функции / Визуал / Оптимизация) из «плашек».
 * ЛКМ по плашке — вкл/выкл, ПКМ — настройки сбоку, СКМ — привязка клавиши.
 */
public final class MainScreen extends UiScreen {
    private static final String[] TABS = {"Функции", "Визуал", "Оптимизация"};
    private static final String[] CORNERS = {"Лево-верх", "Право-верх", "Лево-низ", "Право-низ"};
    private static final int PW = 212;       // ширина боковой панели настроек
    private static int tab = 0;

    private record PanelDef(String key, String title, Supplier<List<Item>> build) { }
    private static PanelDef panel;           // открытая панель настроек (переживает пересоздание экрана)

    private final List<Plate> plates = new ArrayList<>();
    private List<Item> pItems = new ArrayList<>();
    private int scrollMain, scrollP;
    private float panelA;
    private long lastNs = System.nanoTime();
    private int dw = PW;
    private boolean overlay;

    private final TextBox wpName = new TextBox("Название метки", 22, this::wpEnter);
    private final TextBox wpX = new TextBox("X", 10, null), wpY = new TextBox("Y", 10, null), wpZ = new TextBox("Z", 10, null);

    public MainScreen() { super("Untilits"); }

    @Override
    protected void init() {
        layoutPanel(348, 256);
        buildPlates();
        rebuildPanel();
        panelA = panel != null ? 1f : 0f;
        relayout();
    }

    @Override protected boolean showBindHint() { return false; }

    // ======================================================= плашки

    private final class Plate extends Item {
        final String label, id;
        final BooleanSupplier get;
        final Consumer<Boolean> set;
        final PanelDef def;

        Plate(String label, String id, BooleanSupplier get, Consumer<Boolean> set, PanelDef def) {
            this.label = label; this.id = id; this.get = get; this.set = set; this.def = def; h = 26;
        }

        boolean hit(int mx, int my) { return mx >= x && mx < x + w && my >= y && my < y + h; }

        void draw(DrawContext c, int mx, int my) {
            boolean on = get.getAsBoolean(), hov = hit(mx, my);
            boolean open = def != null && panel != null && panel.key().equals(def.key());
            int acc = Cfg.D.accent;
            Gfx.round(c, x, y, w, h, 9, on ? Gfx.withAlpha(acc, hov ? 96 : 74) : (hov ? 0x26FFFFFF : 0x16FFFFFF));
            Gfx.ring(c, x, y, w, h, 9, open ? 0xDDFFFFFF : (on ? Gfx.withAlpha(acc, 190) : 0x2AFFFFFF));
            boolean binding = id != null && id.equals(bindingId);
            String t = binding ? "Нажми клавишу…" : label;
            int reserve = 34 + (def != null ? 10 : 0);
            while (t.length() > 3 && Gfx.tw(t) > w - reserve) t = t.substring(0, t.length() - 1);
            Gfx.text(c, t, x + 9, y + (h - 8) / 2, binding ? Gfx.withAlpha(acc, 255) : (on ? 0xFFFFFFFF : Gfx.TEXT));
            int sx = x + w - 10;
            Gfx.disc(c, sx, y + h / 2, 3, on ? Gfx.withAlpha(acc, 255) : 0x44FFFFFF);
            if (def != null) Gfx.ring(c, sx - 14, y + h / 2 - 3, 6, 6, 3, open ? 0xFFFFFFFF : 0x77FFFFFF);
            Integer key = id == null ? null : Cfg.D.binds.get(id);
            if (key != null && !binding) {
                String k = Binds.keyName(key);
                Gfx.text(c, k, x + w - reserve + 6 - Gfx.tw(k) + (def != null ? 0 : 6), y + (h - 8) / 2, Gfx.withAlpha(acc, 230));
            }
        }
    }

    private Plate plate(String label, String id, PanelDef def) {
        Features.F f = Features.ALL.get(id);
        return new Plate(label, id, f.get(), f.set(), def);
    }

    private Plate plate(String label, BooleanSupplier get, Consumer<Boolean> set, PanelDef def) {
        return new Plate(label, null, get, set, def);
    }

    private Plate nr(String label, int bit) { return plate(label, () -> NoRender.nr(bit), v -> NoRender.setNr(bit, v), null); }

    private Plate opt(String label, int bit) { return plate(label, () -> NoRender.opt(bit), v -> NoRender.setOpt(bit, v), null); }

    private void buildPlates() {
        plates.clear();
        Cfg.Data d = Cfg.D;
        switch (tab) {
            case 0 -> {
                PanelDef hud = def("hud", "HUD", this::pHud);
                plates.add(plate("FPS", "fps", hud));
                plates.add(plate("CPS", "cps", hud));
                plates.add(plate("Пинг", "ping", hud));
                plates.add(plate("Координаты", "coords", hud));
                plates.add(plate("Клавиши", "keys", hud));
                plates.add(plate("Броня (HUD)", "armor", hud));
                plates.add(plate("Potion HUD", "potionHud", null));
                plates.add(plate("Predictions", "predict", def("predict", "Predictions", this::pPredict)));
                plates.add(plate("Прочность брони", "armorOver", def("armor", "Прочность брони", this::pArmor)));
                plates.add(plate("Метки", "wpEnabled", def("wp", "Метки", this::pWp)));
                plates.add(plate("Цифры урона", "damageNumbers", def("dmg", "Цифры урона", this::pDmg)));
                plates.add(plate("Кровь", "blood", def("blood", "Кровь", this::pBlood)));
                plates.add(plate("Уведомления", "notif", null));
                plates.add(plate("FastEXP", "fastExp", null));
            }
            case 1 -> {
                plates.add(plate("Target ESP", "tgtEsp", def("tgt", "Target ESP", this::pTgt)));
                plates.add(plate("Карточка цели", "tgtInfo", null));
                plates.add(plate("Ambience", "ambOn", def("amb", "Ambience", this::pAmb)));
                plates.add(plate("Kill Effects", "killFx", def("kill", "Kill Effects", this::pKill)));
                plates.add(plate("Jump Circles", "jumpCircles", def("jc", "Jump Circles", this::pJc)));
                plates.add(plate("Hit Particles", "hitFx", def("hitfx", "Hit Particles", this::pHitFx)));
                plates.add(plate("Hitmarker", "hitmarker", def("hitm", "Hitmarker", this::pHitm)));
                plates.add(plate("Zoom", "zoomOn", def("zoom", "Zoom", this::pZoom)));
                plates.add(plate("FreeLook", "flOn", null));
                plates.add(plate("Item Physics", "itemPhys", null));
                plates.add(plate("Shulker Preview", "shulkerPrev", null));
                plates.add(plate("Cooldowns", "cdOn", def("cd", "Cooldowns", this::pCd)));
            }
            default -> {
                plates.add(nr("Без скорборда", NoRender.SCOREBOARD));
                plates.add(nr("Без боссбара", NoRender.BOSSBAR));
                plates.add(nr("Без титров", NoRender.TITLES));
                plates.add(nr("Без виньетки", NoRender.VIGNETTE));
                plates.add(nr("Без портала", NoRender.PORTAL));
                plates.add(nr("Без анимации тотема", NoRender.TOTEM));
                plates.add(nr("Без огня на экране", NoRender.FIRE));
                plates.add(nr("Без воды на экране", NoRender.WATER));
                plates.add(nr("Без дождя и снега", NoRender.WEATHER));
                plates.add(nr("Без дыма", NoRender.SMOKE));
                plates.add(nr("Без частиц ломания", NoRender.BREAK));
                plates.add(nr("Без стрел в игроках", NoRender.ARROWS));
                plates.add(nr("Без свечения игроков", NoRender.PGLOW));
                plates.add(opt("Скрыть предметы", NoRender.O_ITEMS));
                plates.add(opt("Скрыть опыт", NoRender.O_XP));
                plates.add(opt("Скрыть стойки", NoRender.O_STANDS));
                plates.add(opt("Скрыть рамки", NoRender.O_FRAMES));
                plates.add(opt("Скрыть далёкие снаряды", NoRender.O_PROJ));
                plates.add(plate("Дальность сущностей", () -> d.optDist > 0, v -> d.optDist = v ? 64 : 0, def("dist", "Дальность сущностей", this::pDist)));
                plates.add(opt("Simplified Leaves", NoRender.O_LEAVES));
                plates.add(opt("Без блеска зачарований", NoRender.O_GLINT));
                plates.add(plate("Memory Optimizer", "memOpt", null));
            }
        }
        scrollMain = Cfg.clamp(scrollMain, 0, maxScrollMain());
    }

    private PanelDef def(String key, String title, Supplier<List<Item>> b) { return new PanelDef(key, title, b); }

    // ======================================================= содержимое панелей настроек

    private List<Item> pHud() {
        Cfg.Data d = Cfg.D;
        List<Item> l = new ArrayList<>();
        l.add(new Section("Показывать"));
        l.add(feat("fps", "FPS"));
        l.add(feat("cps", "CPS"));
        l.add(feat("ping", "Пинг"));
        l.add(feat("coords", "Координаты"));
        l.add(feat("keys", "Клавиши (WASD / мышь)"));
        l.add(feat("armor", "Броня и прочность"));
        l.add(new Cycle("Положение HUD", () -> CORNERS[d.hudCorner], () -> d.hudCorner = (d.hudCorner + 1) % 4));
        return l;
    }

    private List<Item> pPredict() {
        List<Item> l = new ArrayList<>();
        l.add(feat("predict", "Траектории и точка падения"));
        l.add(feat("predictFlying", "Чужие жемчуга и стрелы"));
        return l;
    }

    private List<Item> pArmor() {
        Cfg.Data d = Cfg.D;
        List<Item> l = new ArrayList<>();
        l.add(feat("armorOver", "Прочность над игроками"));
        l.add(feat("armorWarn", "Предупреждение о поломке"));
        l.add(new Slider("Порог предупреждения", 5, 50, 5, "%", () -> d.armorWarnPct, v -> d.armorWarnPct = v));
        return l;
    }

    private List<Item> pDmg() {
        Cfg.Data d = Cfg.D;
        List<Item> l = new ArrayList<>();
        l.add(feat("damageNumbers", "Показывать цифры урона"));
        l.add(new Slider("Размер цифр", 50, 250, 10, "%", () -> d.dmgScale, v -> d.dmgScale = v));
        return l;
    }

    private List<Item> pBlood() {
        Cfg.Data d = Cfg.D;
        List<Item> l = new ArrayList<>();
        l.add(feat("blood", "Кровь при ударах"));
        l.add(new Slider("Количество", 25, 300, 25, "%", () -> d.bloodAmount, v -> d.bloodAmount = v));
        return l;
    }

    private List<Item> pTgt() {
        Cfg.Data d = Cfg.D;
        int m = d.tgtMode;
        List<Item> l = new ArrayList<>();
        l.add(feat("tgtEsp", "Включить Target ESP"));
        l.add(new Cycle("Режим", () -> TargetEsp.MODES[d.tgtMode], () -> { d.tgtMode = (d.tgtMode + 1) % TargetEsp.MODES.length; rebuildPanel(); }));
        if (m == 0) l.add(new Cycle("Скин маркера", () -> TargetEsp.SKINS[d.tgtSkin], () -> d.tgtSkin = (d.tgtSkin + 1) % TargetEsp.SKINS.length));
        l.add(new Slider("Размер", 40, 250, 10, "%", () -> d.tgtSize, v -> d.tgtSize = v));
        l.add(new Slider("Цвет (оттенок)", 0, 360, 10, "°", () -> d.tgtHue, v -> d.tgtHue = v));
        l.add(new Slider("Скорость", 30, 300, 10, "%", () -> d.tgtSpeed, v -> d.tgtSpeed = v));
        if (m != 0 && m != 5 && m != 10) l.add(new Slider("Количество", 1, 24, 1, "", () -> d.tgtCount, v -> d.tgtCount = v));
        l.add(new Toggle("Свечение", () -> d.tgtGlow, v -> d.tgtGlow = v));
        if (m == 9) l.add(new Toggle("Вспышка экрана", () -> d.tgtLFlash, v -> d.tgtLFlash = v));
        l.add(feat("tgtFlash", "Красная вспышка при ударе"));
        l.add(feat("tgtInfo", "Карточка цели (ник, HP)"));
        return l;
    }

    private List<Item> pAmb() {
        Cfg.Data d = Cfg.D;
        List<Item> l = new ArrayList<>();
        l.add(feat("ambOn", "Включить Ambience"));
        l.add(new Cycle("Погода", () -> VisualFx.WEATHER[d.ambWeather], () -> d.ambWeather = (d.ambWeather + 1) % 4));
        l.add(feat("ambTimeLock", "Зафиксировать время"));
        l.add(new Slider("Время суток", 0, 24000, 500, "", () -> d.ambTime, v -> d.ambTime = v));
        l.add(new Cycle("Тонировка", () -> VisualFx.TINT[d.ambTint], () -> d.ambTint = (d.ambTint + 1) % 8));
        l.add(feat("ambRain", "Дождь на экране"));
        l.add(new Slider("Плотность дождя", 20, 300, 20, "%", () -> d.rainDensity, v -> d.rainDensity = v));
        return l;
    }

    private List<Item> pJc() {
        Cfg.Data d = Cfg.D;
        List<Item> l = new ArrayList<>();
        l.add(feat("jumpCircles", "Включить Jump Circles"));
        l.add(new Cycle("Вид", () -> CombatFx.JC_MODES[d.jcMode], () -> d.jcMode = (d.jcMode + 1) % CombatFx.JC_MODES.length));
        l.add(new Slider("Размер", 50, 220, 10, "%", () -> d.jcSize, v -> d.jcSize = v));
        return l;
    }

    private List<Item> pKill() {
        Cfg.Data d = Cfg.D;
        List<Item> l = new ArrayList<>();
        l.add(feat("killFx", "Включить Kill Effects"));
        l.add(new Cycle("Эффект", () -> VisualFx.KILL[d.killMode], () -> d.killMode = (d.killMode + 1) % VisualFx.KILL.length));
        l.add(new Slider("Автобус, длит.", 6, 14, 1, " c", () -> d.killBusDur, v -> d.killBusDur = v));
        l.add(new Slider("FPV, длит.", 3, 8, 1, " c", () -> d.killFpvDur, v -> d.killFpvDur = v));
        l.add(new Hint("Срабатывает, если цель убита"));
        l.add(new Hint("(нужен включённый Target ESP)"));
        return l;
    }

    private List<Item> pHitFx() {
        Cfg.Data d = Cfg.D;
        List<Item> l = new ArrayList<>();
        l.add(feat("hitFx", "Частицы при ударе"));
        l.add(new Cycle("Вид", () -> CombatFx.HIT_MODES[d.hitMode], () -> d.hitMode = (d.hitMode + 1) % 4));
        return l;
    }

    private List<Item> pHitm() {
        Cfg.Data d = Cfg.D;
        List<Item> l = new ArrayList<>();
        l.add(feat("hitmarker", "Hitmarker"));
        l.add(new Cycle("Звук", () -> CombatFx.SOUNDS[d.hitSound], () -> d.hitSound = (d.hitSound + 1) % 5));
        return l;
    }

    private List<Item> pZoom() {
        Cfg.Data d = Cfg.D;
        List<Item> l = new ArrayList<>();
        l.add(feat("zoomOn", "Zoom (удерживай C)"));
        l.add(new Slider("Сила", 2, 12, 1, "x", () -> d.zoomFactor, v -> d.zoomFactor = v));
        return l;
    }

    private List<Item> pCd() {
        Cfg.Data d = Cfg.D;
        List<Item> l = new ArrayList<>();
        l.add(feat("cdOn", "Cooldowns (зелье лечения)"));
        l.add(new Slider("Размер", 50, 200, 10, "%", () -> d.cdScale, v -> d.cdScale = v));
        return l;
    }

    private List<Item> pDist() {
        Cfg.Data d = Cfg.D;
        List<Item> l = new ArrayList<>();
        l.add(new Slider("Дальность (0 — всё)", 0, 256, 16, "", () -> d.optDist, v -> d.optDist = v));
        return l;
    }

    private List<Item> pTheme() {
        Cfg.Data d = Cfg.D;
        int[] acc = {0x5B8CFF, 0x45D0FF, 0x37E0A8, 0xB78CFF, 0xFF7AC8, 0xFF6B6B, 0xFFB454, 0xF2F4FA};
        int[] tint = {0x101A2E, 0x0B0E16, 0x1B1230, 0x0E2420, 0x2A1418, 0x2B2F3A};
        List<Item> l = new ArrayList<>();
        l.add(new Section("Акцент"));
        l.add(new Swatches("Цвета", acc, () -> d.accent, v -> d.accent = 0xFF000000 | v));
        l.add(new HueBar());
        l.add(new Section("Liquid Glass"));
        l.add(new Swatches("Тон стекла", tint, () -> d.glass, v -> d.glass = v));
        l.add(new Slider("Прозрачность", 30, 98, 2, "%", () -> d.glassPct, v -> d.glassPct = v));
        return l;
    }

    // ----- метки (waypoints)

    private void wpEnter() {
        if (!wpX.text.isBlank() && !wpY.text.isBlank() && !wpZ.text.isBlank()) addCoords(); else addHere();
    }

    private void wpAdded() { wpName.text = ""; rebuildPanel(); scrollP = 1 << 20; }

    private void addHere() {
        if (Waypoints.addHere(MinecraftClient.getInstance(), wpName.text) != null) wpAdded();
    }

    private void addLook() {
        if (Waypoints.addLook(MinecraftClient.getInstance(), wpName.text) != null) wpAdded();
        else Notif.push("Наведись на блок", false);
    }

    private void addCoords() {
        try {
            int x = (int) Math.floor(Double.parseDouble(wpX.text.trim().replace(',', '.')));
            int y = (int) Math.floor(Double.parseDouble(wpY.text.trim().replace(',', '.')));
            int z = (int) Math.floor(Double.parseDouble(wpZ.text.trim().replace(',', '.')));
            Waypoints.add(MinecraftClient.getInstance(), wpName.text, new BlockPos(x, y, z));
            wpAdded();
        } catch (NumberFormatException e) {
            Notif.push("Неверные координаты", false);
        }
    }

    private void fillMine() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        wpX.text = String.valueOf(mc.player.getBlockX());
        wpY.text = String.valueOf(mc.player.getBlockY());
        wpZ.text = String.valueOf(mc.player.getBlockZ());
    }

    private List<Item> pWp() {
        Cfg.Data d = Cfg.D;
        List<Item> l = new ArrayList<>();
        l.add(feat("wpEnabled", "Показывать метки"));
        l.add(feat("wpDeath", "Метка смерти"));
        l.add(new Slider("Дальность (0 — без лимита)", 0, 2000, 50, "", () -> d.wpMaxDist, v -> d.wpMaxDist = v));
        l.add(new Section("Новая метка"));
        l.add(wpName);
        l.add(wpX);
        l.add(wpY);
        l.add(wpZ);
        l.add(new ButtonRow("Взять мои", this::fillMine, "+ По координатам", this::addCoords));
        l.add(new ButtonRow("+ Здесь", this::addHere, "+ По взгляду", this::addLook));
        int n = Waypoints.list().size();
        l.add(new Section("Метки (" + n + ")"));
        for (Cfg.WP wp : new ArrayList<>(Waypoints.list())) l.add(new WpRow(wp));
        if (n == 0) l.add(new Hint("Пока пусто"));
        return l;
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
            int ex = x + w - 24;
            if (wp.visible) Gfx.round(c, ex, y + 4, 9, 6, 3, Gfx.withAlpha(Cfg.D.accent, 230));
            else Gfx.ring(c, ex, y + 4, 9, 6, 3, 0x66FFFFFF);
            Gfx.text(c, "x", x + w - 8, y + 4, 0xFFFF8A8A);
        }

        boolean click(int mx, int my) {
            if (mx < x + 11) {
                int i = 0;
                for (int k = 0; k < Waypoints.PALETTE.length; k++) if (Waypoints.PALETTE[k] == wp.color) i = k;
                wp.color = Waypoints.PALETTE[(i + 1) % Waypoints.PALETTE.length];
            } else if (mx >= x + w - 12) {
                Waypoints.list().remove(wp);
                rebuildPanel();
            } else if (mx >= x + w - 28) {
                wp.visible = !wp.visible;
            } else {
                wpName.text = wp.name;
                wpX.text = String.valueOf(wp.x);
                wpY.text = String.valueOf(wp.y);
                wpZ.text = String.valueOf(wp.z);
            }
            return false;
        }
    }

    private static final class Hint extends Item {
        final String t;
        Hint(String t) { this.t = t; h = 14; }
        void draw(DrawContext c, int mx, int my) { Gfx.text(c, t, x, y + 3, 0x66FFFFFF); }
    }

    private void rebuildPanel() {
        pItems = panel == null ? new ArrayList<>() : panel.build().get();
    }

    private void openPanel(PanelDef def) {
        if (panel != null && panel.key().equals(def.key())) { closePanel(); return; }
        panel = def;
        scrollP = 0;
        TextBox.focused = null;
        rebuildPanel();
    }

    private void closePanel() { panel = null; TextBox.focused = null; }

    // ======================================================= геометрия

    private static float ease(float a) { return 1f - (1f - a) * (1f - a) * (1f - a); }

    private int vy0() { return py + 52; }
    private int vy1() { return py + ph - 22; }
    private int cw() { return (pw - 32 - 6) / 2; }
    private int rows() { return (plates.size() + 1) / 2; }
    private int maxScrollMain() { return Math.max(0, rows() * 30 - (vy1() - vy0())); }

    private void layoutPlates() {
        int cw = cw();
        for (int i = 0; i < plates.size(); i++) {
            Plate p = plates.get(i);
            p.x = px + 16 + (i % 2) * (cw + 6);
            p.y = vy0() + (i / 2) * 30 - scrollMain;
            p.w = cw;
            p.h = 26;
        }
    }

    private int contentH(List<Item> list) {
        int h = 40;
        for (Item it : list) h += it.h + 2;
        return h + 8;
    }

    private void relayout() {
        int gap = 6;
        boolean open = panel != null;
        dw = PW;
        overlay = false;
        if (open && pw + gap + dw > width - 8) {
            dw = width - 8 - pw - gap;
            if (dw < 150) { overlay = true; dw = pw; }
        }
        float e = ease(panelA);
        if (overlay) px = (width - pw) / 2;
        else px = Math.round((width - (pw + e * (dw + gap))) / 2f);
        layoutPlates();
    }

    private boolean overlayShown() { return overlay && panelA > 0.02f; }

    private int[] box() {
        if (overlay) return new int[]{px, py, pw, ph};
        int dh = Math.min(height - 12, Math.max(80, contentH(pItems)));
        int dy = Math.max(6, py + ph / 2 - dh / 2);
        if (dy + dh > height - 6) dy = height - 6 - dh;
        return new int[]{px + pw + 6, dy, dw, dh};
    }

    private int maxScrollP(int[] b) {
        return Math.max(0, contentH(pItems) - 40 - 8 - (b[3] - 36 - 8));
    }

    private void placePanel(int[] b, int scroll) {
        int y = b[1] + 32 - scroll;
        for (Item it : pItems) {
            it.x = b[0] + 12;
            it.w = b[2] - 24;
            it.y = y;
            y += it.h + 2;
        }
    }

    // ======================================================= рисование

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        float target = panel != null ? 1f : 0f;
        panelA += (target - panelA) * Math.min(1f, dt * 10f);
        if (Math.abs(panelA - target) < 0.004f) panelA = target;
        relayout();

        if (overlayShown() && panelA > 0.98f) c.fill(0, 0, width, height, 0x5A050810);
        else super.render(c, mx, my, delta);
        if (panelA > 0.01f && panel != null || panelA > 0.01f) drawPanel(c, mx, my);
    }

    private int tabX(int i) {
        int x = px + 16;
        for (int k = 0; k < i; k++) x += Gfx.tw(TABS[k]) + 18 + 4;
        return x;
    }

    private int themeX() { String s = "Тема"; return px + pw - 16 - (Gfx.tw(s) + 18); }

    @Override
    protected void header(DrawContext c, int mx, int my) {
        int acc = Cfg.D.accent;
        Gfx.round(c, px + 16, py + 12, 8, 8, 4, acc);
        Gfx.shadow(c, "Untilits", px + 30, py + 12, 0xFFFFFFFF);
        // тема
        int tx = themeX(), tw = Gfx.tw("Тема") + 18;
        boolean themeOpen = panel != null && panel.key().equals("theme");
        boolean th = mx >= tx && mx < tx + tw && my >= py + 8 && my < py + 24;
        Gfx.round(c, tx, py + 9, tw, 15, 7, themeOpen ? Gfx.withAlpha(acc, 210) : (th ? 0x2AFFFFFF : 0x16FFFFFF));
        Gfx.text(c, "Тема", tx + 9, py + 13, themeOpen ? 0xFFFFFFFF : Gfx.DIM);
        // вкладки
        for (int i = 0; i < TABS.length; i++) {
            int bx = tabX(i), bw = Gfx.tw(TABS[i]) + 18, by = py + 28;
            boolean sel = i == tab, hov = mx >= bx && mx < bx + bw && my >= by && my < by + 17;
            Gfx.round(c, bx, by, bw, 17, 8, sel ? Gfx.withAlpha(acc, 210) : (hov ? 0x26FFFFFF : 0x14FFFFFF));
            Gfx.text(c, TABS[i], bx + 9, by + 5, sel ? 0xFFFFFFFF : Gfx.DIM);
        }
        // плашки в области просмотра
        c.enableScissor(px + 4, vy0() - 2, px + pw - 4, vy1() + 2);
        for (Plate p : plates) if (p.y + p.h > vy0() - 2 && p.y < vy1() + 2) p.draw(c, mx, my);
        c.disableScissor();
        int ms = maxScrollMain();
        if (ms > 0) {
            int trackH = vy1() - vy0();
            int thh = Math.max(14, trackH * trackH / (trackH + ms));
            int ty = vy0() + (trackH - thh) * scrollMain / ms;
            Gfx.round(c, px + pw - 9, ty, 3, thh, 1, 0x55FFFFFF);
        }
        String hint = bindingId != null ? "Нажми клавишу…  (Esc — отмена, Backspace — снять)" : "ЛКМ — вкл/выкл   ПКМ — настройки   СКМ — клавиша";
        Gfx.text(c, hint, px + 16, py + ph - 14, bindingId != null ? Gfx.withAlpha(acc, 255) : 0x55FFFFFF);
        int cx = px + pw - 11, cy = py + ph - 11;
        boolean near = Math.abs(mx - cx) <= 6 && Math.abs(my - cy) <= 6;
        Gfx.disc(c, cx, cy, 3, near ? 0x30FFFFFF : 0x12FFFFFF);
    }

    private void drawPanel(DrawContext c, int mx, int my) {
        PanelDef def = panel;
        if (def == null && pItems.isEmpty()) return;
        float anim = ease(panelA);
        int[] b = box();
        int off = overlay ? Math.round((1f - anim) * b[2]) : 0;
        int bx = b[0] + off;
        int[] bb = {bx, b[1], b[2], b[3]};
        Gfx.glass(c, bx, b[1], b[2], b[3], 12, true);
        Gfx.round(c, bx + 12, b[1] + 12, 6, 6, 3, Cfg.D.accent);
        String title = def != null ? def.title() : "";
        Gfx.shadow(c, title, bx + 24, b[1] + 11, 0xFFFFFFFF);
        int cx = bx + b[2] - 22, cy = b[1] + 8;
        boolean hov = mx >= cx && mx < cx + 14 && my >= cy && my < cy + 14;
        Gfx.round(c, cx, cy, 14, 14, 7, hov ? 0x44FFFFFF : 0x20FFFFFF);
        Gfx.text(c, "x", cx + 4, cy + 3, 0xFFFFFFFF);
        int ms = maxScrollP(b);
        scrollP = Cfg.clamp(scrollP, 0, ms);
        placePanel(bb, scrollP);
        c.enableScissor(Math.max(0, bx), b[1] + 30, Math.min(width, bx + b[2]), b[1] + b[3] - 4);
        for (Item it : pItems) if (it.y + it.h > b[1] + 28 && it.y < b[1] + b[3]) it.draw(c, mx, my);
        c.disableScissor();
        if (ms > 0) {
            int trackH = b[3] - 40;
            int th = Math.max(12, trackH * trackH / (trackH + ms));
            int ty = b[1] + 32 + (trackH - th) * scrollP / ms;
            Gfx.round(c, bx + b[2] - 5, ty, 2, th, 1, 0x55FFFFFF);
        }
    }

    // ======================================================= ввод

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        int b = click.button();
        boolean ovl = overlayShown();
        boolean wasBinding = bindingId != null;
        bindingId = null;

        if (panel != null && panelA > 0.5f) {
            int[] bx = box();
            if (b == 0 && mx >= bx[0] + bx[2] - 22 && mx < bx[0] + bx[2] - 8 && my >= bx[1] + 8 && my < bx[1] + 22) { closePanel(); return true; }
            if (mx >= bx[0] && mx < bx[0] + bx[2] && my >= bx[1] && my < bx[1] + bx[3]) {
                TextBox.focused = null;
                if (my >= bx[1] + 30 && my < bx[1] + bx[3] - 4) {
                    placePanel(bx, scrollP);
                    clickItems(pItems, mx, my, b);
                }
                return true;
            }
        }
        if (ovl) return true;

        if (b == 0) {
            int tx = themeX(), tw = Gfx.tw("Тема") + 18;
            if (mx >= tx && mx < tx + tw && my >= py + 8 && my < py + 24) { openPanel(def("theme", "Тема", this::pTheme)); return true; }
            for (int i = 0; i < TABS.length; i++) {
                int bx = tabX(i), bw = Gfx.tw(TABS[i]) + 18, by = py + 28;
                if (mx >= bx && mx < bx + bw && my >= by && my < by + 17) {
                    tab = i;
                    scrollMain = 0;
                    buildPlates();
                    return true;
                }
            }
            int cx = px + pw - 11, cy = py + ph - 11;
            if (Math.abs(mx - cx) <= 6 && Math.abs(my - cy) <= 6) { client.setScreen(new PasswordScreen()); return true; }
        }
        if (my >= vy0() && my < vy1()) {
            for (Plate p : plates) {
                if (!p.hit(mx, my)) continue;
                TextBox.focused = null;
                if (b == 0) {
                    p.set.accept(!p.get.getAsBoolean());
                    Cfg.save();
                } else if (b == 1) {
                    if (p.def != null) openPanel(p.def);
                } else if (b == 2) {
                    if (p.id != null) bindingId = p.id;
                }
                return true;
            }
        }
        TextBox.focused = null;
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && bindingId == null && TextBox.focused == null && panel != null) {
            closePanel();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hx, double vy) {
        int step = vy < 0 ? 22 : -22;
        if (panel != null) {
            int[] b = box();
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) {
                scrollP = Cfg.clamp(scrollP + step, 0, maxScrollP(b));
                return true;
            }
        }
        if (!overlayShown() && my >= vy0() && my < vy1()) {
            scrollMain = Cfg.clamp(scrollMain + step, 0, maxScrollMain());
            return true;
        }
        return super.mouseScrolled(mx, my, hx, vy);
    }
}
