package dev.untilits;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Реестр переключаемых функций: к ним можно привязать клавишу (СКМ по функции в меню, затем клавиша). */
public final class Features {
    public record F(String id, String label, BooleanSupplier get, Consumer<Boolean> set) { }

    public static final Map<String, F> ALL = new LinkedHashMap<>();

    private static void reg(String id, String label, BooleanSupplier g, Consumer<Boolean> s) { ALL.put(id, new F(id, label, g, s)); }

    static {
        reg("esp", "ESP игроки", () -> Cfg.D.esp, v -> Cfg.D.esp = v);
        reg("espMobs", "ESP мобы", () -> Cfg.D.espMobs, v -> Cfg.D.espMobs = v);
        reg("espItems", "ESP предметы", () -> Cfg.D.espItems, v -> Cfg.D.espItems = v);
        reg("espGlow", "ESP контур-glow", () -> Cfg.D.espGlow, v -> Cfg.D.espGlow = v);
        reg("weak", "Слабые места", () -> Cfg.D.weak, v -> Cfg.D.weak = v);
        reg("tracers", "Tracers", () -> Cfg.D.tracers, v -> Cfg.D.tracers = v);
        reg("tracersMobs", "Tracers мобы", () -> Cfg.D.tracersMobs, v -> Cfg.D.tracersMobs = v);
        reg("tracersItems", "Tracers предметы", () -> Cfg.D.tracersItems, v -> Cfg.D.tracersItems = v);
        reg("chestEsp", "Chest ESP", () -> Cfg.D.chestEsp, v -> Cfg.D.chestEsp = v);
        reg("chestSpawners", "Спавнеры", () -> Cfg.D.chestSpawners, v -> Cfg.D.chestSpawners = v);
        reg("freecam", "FreeCam", () -> Freecam.active, Freecam::set);
        reg("names", "NameTags", () -> Cfg.D.names, v -> Cfg.D.names = v);
        reg("sysHud", "System HUD", () -> Cfg.D.sysHud, v -> Cfg.D.sysHud = v);
        reg("kbHud", "Таблица кейбиндов", () -> Cfg.D.kbHud, v -> Cfg.D.kbHud = v);
        reg("trapTimer", "Trap Timer", () -> Cfg.D.trapTimer, v -> Cfg.D.trapTimer = v);
        reg("zeroTcp", "TCP NoDelay", () -> Cfg.D.zeroTcp, v -> Cfg.D.zeroTcp = v);
        reg("zeroJump", "Без задержки прыжка", () -> Cfg.D.zeroJump, v -> Cfg.D.zeroJump = v);
        reg("zeroAttack", "Без штрафа атаки", () -> Cfg.D.zeroAttack, v -> Cfg.D.zeroAttack = v);
        reg("zeroRaycast", "Raycast перед ударом", () -> Cfg.D.zeroRaycast, v -> Cfg.D.zeroRaycast = v);
        reg("zeroSnap", "SnapTap (SOCD)", () -> Cfg.D.zeroSnap, v -> Cfg.D.zeroSnap = v);
        reg("tgtEsp", "Target ESP", () -> Cfg.D.tgtEsp, v -> Cfg.D.tgtEsp = v);
        reg("tgtFlash", "Target ESP: вспышка при ударе", () -> Cfg.D.tgtFlash, v -> Cfg.D.tgtFlash = v);
        reg("tgtInfo", "Карточка цели", () -> Cfg.D.tgtInfo, v -> Cfg.D.tgtInfo = v);
        reg("ambRain", "Дождь-оверлей", () -> Cfg.D.ambRain, v -> Cfg.D.ambRain = v);
        reg("notif", "Уведомления", () -> Cfg.D.notif, v -> Cfg.D.notif = v);
        reg("memOpt", "Memory Optimizer", () -> Cfg.D.memOpt, v -> Cfg.D.memOpt = v);
        reg("jumpCircles", "Jump Circles", () -> Cfg.D.jumpCircles, v -> Cfg.D.jumpCircles = v);
        reg("hitFx", "Hit Particles", () -> Cfg.D.hitFx, v -> Cfg.D.hitFx = v);
        reg("hitmarker", "Hitmarker", () -> Cfg.D.hitmarker, v -> Cfg.D.hitmarker = v);
        reg("zoomOn", "Zoom (клавиша C)", () -> Cfg.D.zoomOn, v -> Cfg.D.zoomOn = v);
        reg("flOn", "FreeLook (Left Alt)", () -> Cfg.D.flOn, v -> Cfg.D.flOn = v);
        reg("itemPhys", "Item Physics", () -> Cfg.D.itemPhys, v -> Cfg.D.itemPhys = v);
        reg("shulkerPrev", "Shulker Preview", () -> Cfg.D.shulkerPrev, v -> Cfg.D.shulkerPrev = v);
        reg("hbOn", "Hitbox ESP", () -> Cfg.D.hbOn, v -> Cfg.D.hbOn = v);
        reg("hbMobs", "Hitbox: мобы", () -> Cfg.D.hbMobs, v -> Cfg.D.hbMobs = v);
        reg("srchOn", "Block Search", () -> Cfg.D.srchOn, v -> Cfg.D.srchOn = v);
        reg("aimOn", "AimAssist", () -> Cfg.D.aimOn, v -> Cfg.D.aimOn = v);
        reg("kaOn", "KillAura", () -> Cfg.D.kaOn, v -> Cfg.D.kaOn = v);
        reg("trigOn", "TriggerBot", () -> Cfg.D.trigOn, v -> Cfg.D.trigOn = v);
        reg("fullbright", "Fullbright", () -> Cfg.D.fullbright, v -> Cfg.D.fullbright = v);
        reg("bpsHud", "Скорость (BPS)", () -> Cfg.D.bpsHud, v -> Cfg.D.bpsHud = v);
        reg("clockHud", "Часы", () -> Cfg.D.clockHud, v -> Cfg.D.clockHud = v);
        reg("distHud", "Дистанция до цели", () -> Cfg.D.distHud, v -> Cfg.D.distHud = v);
        reg("comboHud", "Счётчик комбо", () -> Cfg.D.comboHud, v -> Cfg.D.comboHud = v);
        reg("unhook", "Unhook", () -> Unhook.active, Unhook::set);
        reg("scanOn", "Storage Scanner", () -> Cfg.D.scanOn, v -> Cfg.D.scanOn = v);
        reg("ambOn", "Ambience", () -> Cfg.D.ambOn, v -> Cfg.D.ambOn = v);
        reg("ambTimeLock", "Ambience: фикс. время", () -> Cfg.D.ambTimeLock, v -> Cfg.D.ambTimeLock = v);
        reg("fastExp", "FastEXP", () -> Cfg.D.fastExp, v -> Cfg.D.fastExp = v);
        reg("cdOn", "Cooldowns", () -> Cfg.D.cdOn, v -> Cfg.D.cdOn = v);
        reg("killFx", "Kill Effects", () -> Cfg.D.killFx, v -> Cfg.D.killFx = v);
        reg("potionHud", "Potion HUD", () -> Cfg.D.potionHud, v -> Cfg.D.potionHud = v);
        reg("predict", "Predictions", () -> Cfg.D.predict, v -> Cfg.D.predict = v);
        reg("predictFlying", "Предикт чужих снарядов", () -> Cfg.D.predictFlying, v -> Cfg.D.predictFlying = v);
        reg("armorOver", "Броня над игроком", () -> Cfg.D.armorOver, v -> Cfg.D.armorOver = v);
        reg("armorWarn", "Предупреждение о броне", () -> Cfg.D.armorWarn, v -> Cfg.D.armorWarn = v);
        reg("wpEnabled", "Метки (waypoints)", () -> Cfg.D.wpEnabled, v -> Cfg.D.wpEnabled = v);
        reg("wpDeath", "Метка смерти", () -> Cfg.D.wpDeath, v -> Cfg.D.wpDeath = v);
        reg("fps", "FPS", () -> Cfg.D.fps, v -> Cfg.D.fps = v);
        reg("cps", "CPS", () -> Cfg.D.cps, v -> Cfg.D.cps = v);
        reg("ping", "Пинг", () -> Cfg.D.ping, v -> Cfg.D.ping = v);
        reg("coords", "Координаты", () -> Cfg.D.coords, v -> Cfg.D.coords = v);
        reg("keys", "Клавиши", () -> Cfg.D.keys, v -> Cfg.D.keys = v);
        reg("armor", "Броня", () -> Cfg.D.armor, v -> Cfg.D.armor = v);
        reg("damageNumbers", "Цифры урона", () -> Cfg.D.damageNumbers, v -> Cfg.D.damageNumbers = v);
        reg("blood", "Кровь", () -> Cfg.D.blood, v -> Cfg.D.blood = v);
    }

    private Features() { }
}
