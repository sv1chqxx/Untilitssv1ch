package dev.untilits;

import java.util.LinkedHashMap;
import java.util.Map;

/** Unhook: выключает все «читовые» функции и прячет кнопку секретного меню до повторного нажатия бинда. */
public final class Unhook {
    public static boolean active;
    private static final String[] IDS = {"esp", "espMobs", "espItems", "espGlow", "weak", "tracers", "tracersMobs", "tracersItems",
            "chestEsp", "chestSpawners", "freecam", "names", "sysHud", "kbHud", "trapTimer", "zeroTcp", "zeroJump", "zeroAttack",
            "zeroRaycast", "zeroSnap", "hbOn", "hbMobs", "srchOn", "scanOn", "aimOn", "trigOn", "fullbright"};
    private static final Map<String, Boolean> SAVED = new LinkedHashMap<>();

    private Unhook() { }

    public static void set(boolean on) {
        if (on == active) return;
        if (on) {
            SAVED.clear();
            for (String id : IDS) {
                Features.F f = Features.ALL.get(id);
                if (f == null) continue;
                boolean cur = f.get().getAsBoolean();
                SAVED.put(id, cur);
                if (cur) f.set().accept(false);
            }
            Kill3D.panic();
            active = true;
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc.currentScreen instanceof dev.untilits.ui.UiScreen) mc.setScreen(null);
        } else {
            active = false;
            for (Map.Entry<String, Boolean> e : SAVED.entrySet()) {
                Features.F f = Features.ALL.get(e.getKey());
                if (f != null && e.getValue()) f.set().accept(true);
            }
            SAVED.clear();
        }
    }
}
