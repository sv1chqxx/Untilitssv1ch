package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;

import java.util.HashMap;
import java.util.Map;

/** Кейбинды функций: нажал клавишу — функция переключилась (когда нет открытого экрана). */
public final class Binds {
    private static final Map<Integer, Boolean> prev = new HashMap<>();

    private Binds() { }

    public static String keyName(int code) {
        try { return InputUtil.fromKeyCode(new KeyInput(code, 0, 0)).getLocalizedText().getString(); }
        catch (Throwable t) { return "#" + code; }
    }

    public static void tick(MinecraftClient mc) {
        if (Cfg.D.binds.isEmpty() || mc.getWindow() == null) return;
        boolean canFire = mc.currentScreen == null && mc.world != null && mc.player != null;
        for (Map.Entry<String, Integer> e : Cfg.D.binds.entrySet()) {
            int key = e.getValue();
            boolean down = InputUtil.isKeyPressed(mc.getWindow(), key);
            boolean was = prev.getOrDefault(key, false);
            prev.put(key, down);
            if (down && !was && canFire) {
                Features.F f = Features.ALL.get(e.getKey());
                if (f != null) { boolean nv = !f.get().getAsBoolean(); f.set().accept(nv); Notif.push(f.label() + (nv ? ": ВКЛ" : ": ВЫКЛ"), nv); }
            }
        }
    }
}
