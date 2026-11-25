package dev.untilits.ui;

import dev.untilits.Cfg;
import dev.untilits.Freecam;
import dev.untilits.Gfx;
import dev.untilits.Friends;
import dev.untilits.KeybindsHud;
import dev.untilits.SysHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

/** Скрытое меню: PlayerESP, NameTags, System HUD. Открывается только после ввода кода. */
public final class SecretScreen extends UiScreen {
    private static int tab;
    private int dragHud;   // 0 нет, 1 System, 2 Keybinds
    private int offX, offY;
    private TextBox friendBox, searchBox;

    public SecretScreen() { super("Скрытое меню"); }

    private static final String[] TABS = {"ESP", "Бой", "Мир", "Игроки"};
    private static final int[][] GROUPS = {{0, 1}, {7, 3}, {2, 6}, {4, 5}};
    private int scroll, content;;

    @Override
    protected void init() {
        String keepFriend = friendBox == null ? "" : friendBox.text;
        String keepSearch = searchBox == null ? null : searchBox.text;
        items.clear();
        Cfg.Data d = Cfg.D;
        for (int cs : GROUPS[tab]) sect(cs, d, keepFriend, keepSearch);
        layoutPanel(420, 320);
        relayoutItems();
    }

    private void relayoutItems() {
        int h = 0;
        for (Item it : items) h += it.h + 2;
        content = h;
        int view = ph - 56 - 20;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, h - view)));
        place(py + 56 - scroll);
    }

    private void sect(int cs, Cfg.Data d, String keepFriend, String keepSearch) {
        switch (cs) {
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
            case 3 -> {
                items.add(new Section("Trap Timer — таймер на поставленной трапке"));
                items.add(feat("trapTimer", "Trap Timer"));
                items.add(new Slider("Размер таймера", 40, 250, 10, "%", () -> d.trapScale, v -> d.trapScale = v));
                items.add(new Section("ZeroInput — меньше задержек ввода"));
                items.add(feat("zeroTcp", "TCP_NODELAY (со следующего подключения)"));
                items.add(feat("zeroJump", "Прыжок без задержки"));
                items.add(feat("zeroAttack", "Атака без кулдауна клика"));
                items.add(feat("zeroRaycast", "Свежий raycast при клике"));
                items.add(feat("zeroSnap", "SnapTap (A/D и W/S — последняя клавиша)"));
            }
            case 4 -> {
                friendBox = new TextBox("Ник друга", 16, this::addFriend);
                friendBox.text = keepFriend;
                items.add(new Section("Друзья — зелёные ники"));
                items.add(friendBox);
                items.add(new ButtonRow("Добавить", this::addFriend, "Игрок в прицеле", this::addLooked));
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.world != null && mc.player != null) {
                    List<String> near = new ArrayList<>();
                    for (PlayerEntity p : mc.world.getPlayers()) {
                        String n = p.getName().getString();
                        if (p != mc.player && !Friends.is(n) && Friends.valid(n) && near.size() < 4) near.add(n);
                    }
                    if (!near.isEmpty()) {
                        items.add(new Section("Рядом"));
                        for (String n : near) items.add(new Button("+ " + n, () -> { Friends.add(n); init(); }));
                    }
                }
                items.add(new Section("Список (" + d.friends.size() + ")"));
                int shown = 0;
                for (String f : new ArrayList<>(d.friends)) { if (shown++ < 6) items.add(new FriendRow(f)); }
                if (d.friends.size() > 6) items.add(new Section("… ещё " + (d.friends.size() - 6) + " (.friend list)"));
            }
            case 6 -> {
                searchBox = new TextBox("Название блока (diamond_ore)", 32, this::applySearch);
                searchBox.text = keepSearch != null ? keepSearch : d.srchQuery;
                items.add(new Section("Block Search — подсветка блоков по названию"));
                items.add(feat("srchOn", "Block Search"));
                items.add(searchBox);
                items.add(new ButtonRow("Применить", this::applySearch, "Алмазы", () -> setQ("diamond_ore")));
                items.add(new ButtonRow("Золото", () -> setQ("gold_ore"), "Незерит", () -> setQ("ancient_debris")));
                items.add(new ButtonRow("Изумруды", () -> setQ("emerald_ore"), "Спавнер", () -> setQ("spawner")));
                items.add(new Slider("Радиус поиска", 8, 32, 4, " бл.", () -> d.srchRange, v -> d.srchRange = v));
                items.add(new Section("Прочее"));
                items.add(feat("scanOn", "Storage Scanner (счётчик контейнеров)"));
                items.add(feat("hbOn", "Hitbox ESP (3D-рамки)"));
                items.add(feat("hbMobs", "Hitbox: и мобы"));
            }
            case 7 -> {
                items.add(new Section("AimAssist — помогает, пока ты двигаешь мышь"));
                items.add(feat("aimOn", "AimAssist"));
                items.add(new Slider("Дистанция", 1, 6, 1, " бл.", () -> d.aimDist, v -> d.aimDist = v));
                items.add(new Slider("FOV", 10, 180, 10, "°", () -> d.aimFov, v -> d.aimFov = v));
                items.add(new Slider("Сила", 10, 100, 10, "%", () -> d.aimStrength, v -> d.aimStrength = v));
                items.add(new Toggle("Игроки", () -> d.aimPlayers, v -> d.aimPlayers = v));
                items.add(new Toggle("Мобы", () -> d.aimMobs, v -> d.aimMobs = v));
                items.add(new Toggle("По вертикали", () -> d.aimVert, v -> d.aimVert = v));
                items.add(new Section("TriggerBot — удар, когда прицел на цели"));
                items.add(feat("trigOn", "TriggerBot"));
                items.add(new Slider("Дистанция", 1, 6, 1, " бл.", () -> d.trigDist, v -> d.trigDist = v));
                items.add(new Toggle("Рандомная задержка ударов", () -> d.trigRand, v -> d.trigRand = v));
                items.add(new Toggle("Только криты", () -> d.trigCrit, v -> d.trigCrit = v));
                items.add(new Toggle("Игроки", () -> d.trigPlayers, v -> d.trigPlayers = v));
                items.add(new Toggle("Мобы", () -> d.trigMobs, v -> d.trigMobs = v));
            }
            default -> {
                items.add(new Section("NameTags — ники издалека, даже у крадущихся"));
                items.add(new Toggle("NameTags", () -> d.names, v -> d.names = v));
                items.add(new Slider("Дальность ников", 16, 512, 16, " бл.", () -> d.nameRange, v -> d.nameRange = v));
                items.add(new Toggle("Показывать HP", () -> d.nameHp, v -> d.nameHp = v));
                items.add(new Toggle("Показывать броню и предмет", () -> d.nameItems, v -> d.nameItems = v));
                items.add(new Section("Hud — панель System (перетащи мышью)"));
                items.add(feat("sysHud", "System HUD (FPS / MS / TPS)"));
                items.add(feat("kbHud", "Таблица биндов (ср. кнопка по функции в меню)"));
            }
        }
    }

    private void applySearch() {
        if (searchBox != null) { Cfg.D.srchQuery = searchBox.text.trim(); Cfg.save(); }
    }

    private void setQ(String q) {
        Cfg.D.srchQuery = q;
        Cfg.D.srchOn = true;
        Cfg.save();
        init();
    }

    private void addFriend() {
        if (friendBox != null && Friends.add(friendBox.text.trim())) { friendBox.text = ""; init(); }
    }

    private void addLooked() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.targetedEntity instanceof PlayerEntity p && Friends.add(p.getName().getString())) init();
    }

    private final class FriendRow extends Item {
        final String name;
        FriendRow(String n) { name = n; }
        void draw(DrawContext c, int mx, int my) {
            hover(c, mx, my);
            Gfx.text(c, name, x, y + 5, 0xFF55FF55);
            Gfx.text(c, "x", x + w - 6, y + 5, 0xFFFF8A8A);
        }
        boolean click(int mx, int my) {
            if (mx >= x + w - 14) { Friends.remove(name); init(); }
            return false;
        }
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        c.fill(0, 0, width, height, 0x5A050810);
        Gfx.glass(c, px, py, pw, ph, 12, true);
        header(c, mx, my);
        int top = py + 54, bot = py + ph - 8;
        c.enableScissor(px, top, px + pw, bot);
        for (Item it : items) if (it.y + it.h > top && it.y < bot) it.draw(c, mx, my);
        c.disableScissor();
        int view = bot - top;
        if (content > view) {
            int bh = Math.max(16, view * view / content);
            int by = top + (int) ((view - bh) * (scroll / (float) (content - view)));
            Gfx.round(c, px + pw - 7, by, 3, bh, 1, 0x66FFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hx, double vy) {
        scroll -= (int) (vy * 18);
        relayoutItems();
        return true;
    }

    @Override
    protected void header(DrawContext c, int mx, int my) {
        Gfx.round(c, px + 16, py + 12, 8, 8, 4, Cfg.D.accent);
        Gfx.shadow(c, "Скрытое меню", px + 30, py + 12, 0xFFFFFFFF);
        int x = px + 16;
        for (int i = 0; i < TABS.length; i++) {
            int w = Gfx.tw(TABS[i]) + 10;
            Gfx.round(c, x, py + 30, w, 18, 8, i == tab ? Cfg.D.accent : 0x40FFFFFF);
            Gfx.text(c, TABS[i], x + 5, py + 35, 0xFFFFFFFF);
            x += w + 4;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x(), my = (int) click.y();
        if (click.button() == 0 && my >= py + 30 && my < py + 48) {
            int x = px + 16;
            for (int i = 0; i < TABS.length; i++) {
                int w = Gfx.tw(TABS[i]) + 10;
                if (mx >= x && mx < x + w) { if (tab != i) { tab = i; scroll = 0; init(); } return true; }
                x += w + 4;
            }
        }
        if (my >= py + 54 && my < py + ph - 8 && super.mouseClicked(click, doubled)) return true;
        if (click.button() == 0 && Cfg.D.sysHud
                && mx >= SysHud.bx && mx < SysHud.bx + SysHud.bw && my >= SysHud.by && my < SysHud.by + SysHud.bh) {
            dragHud = 1;
            offX = mx - SysHud.bx;
            offY = my - SysHud.by;
            return true;
        }
        if (click.button() == 0 && Cfg.D.kbHud
                && mx >= KeybindsHud.bx && mx < KeybindsHud.bx + KeybindsHud.bw && my >= KeybindsHud.by && my < KeybindsHud.by + KeybindsHud.bh) {
            dragHud = 2;
            offX = mx - KeybindsHud.bx;
            offY = my - KeybindsHud.by;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragHud == 1) {
            Cfg.D.sysX = (int) click.x() - offX;
            Cfg.D.sysY = (int) click.y() - offY;
            return true;
        }
        if (dragHud == 2) {
            Cfg.D.kbX = (int) click.x() - offX;
            Cfg.D.kbY = (int) click.y() - offY;
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragHud != 0) { dragHud = 0; Cfg.save(); return true; }
        return super.mouseReleased(click);
    }
}
