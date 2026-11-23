package dev.untilits;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Конфиг мода (config/untilits.json). */
public final class Cfg {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Data D = new Data();

    /** Метка (waypoint). */
    public static final class WP {
        public String name = "Метка";
        public int x, y, z;
        public String dim = "minecraft:overworld";
        public int color = 0x5B8CFF;
        public boolean visible = true;
        public WP() { }
        public WP(String name, int x, int y, int z, String dim, int color) {
            this.name = name; this.x = x; this.y = y; this.z = z; this.dim = dim; this.color = color;
        }
    }

    public static final class Data {
        // оформление
        public int accent = 0xFF5B8CFF;
        public int glass = 0x101A2E;     // RGB тона стекла
        public int glassPct = 78;        // прозрачность стекла, %

        // HUD (аналог KronHUD)
        public boolean fps = true;
        public boolean cps = true;
        public boolean coords = false;
        public boolean ping = true;
        public boolean keys = false;
        public boolean armor = false;
        public int hudCorner = 0;        // 0 лево-верх, 1 право-верх, 2 лево-низ, 3 право-низ

        // эффекты
        public boolean damageNumbers = true;
        public int dmgScale = 100;
        public boolean blood = true;
        public int bloodAmount = 100;

        // скрытое меню
        public boolean esp = false;
        public int espRange = 128;
        public boolean weak = false;      // подсветка слабых мест (игроки с низким HP)
        public int weakHp = 8;
        public boolean chestEsp = false;
        public int chestRange = 64;
        public boolean chestSpawners = false;
        public int freecamSpeed = 5;
        public boolean espGlow = false;   // ванильный контур (тяжёлый для FPS)
        public boolean espMobs = false;
        public boolean espItems = false;
        public boolean tracers = false;
        public int tracerRange = 128;
        public boolean tracersMobs = false;
        public boolean tracersItems = false;
        public boolean names = false;
        public int nameRange = 256;
        public boolean nameHp = true;
        public boolean nameItems = true;
        public boolean sysHud = false;
        public int sysX = -1;
        public int sysY = 8;

        public boolean kbHud = false;     // таблица включённых кейбиндов
        public int kbX = -1;
        public int kbY = 40;
        public List<String> friends = new ArrayList<>();
        public Map<String, Integer> binds = new LinkedHashMap<>();

        public boolean trapTimer = false;
        public int trapScale = 100;
        public boolean zeroTcp = false;
        public boolean zeroJump = false;
        public boolean zeroAttack = false;
        public boolean zeroRaycast = false;
        public boolean zeroSnap = false;

        // обычное меню: метки, эффекты, предсказания, броня
        public boolean wpEnabled = true;
        public boolean wpDeath = true;
        public int wpMaxDist = 0;          // 0 = без ограничения
        public List<WP> waypoints = new ArrayList<>();
        public boolean potionHud = false;
        public boolean predict = false;
        public boolean predictFlying = true;
        public boolean armorOver = false;
        public boolean armorWarn = true;
        public int armorWarnPct = 15;
        public boolean tgtEsp, tgtInfo;
        public int nrMask, optMask, optDist;
        public boolean ambRain, notif = true, memOpt;
        public boolean jumpCircles, hitFx, hitmarker, zoomOn, flOn, itemPhys, shulkerPrev;
        public int hitMode, hitSound, zoomFactor = 4;
        public boolean hbOn, hbMobs, srchOn, scanOn;
        public String srchQuery = "diamond_ore";
        public int srchRange = 16;
        public int ambTint;
        public int rainDensity = 100;
        public int tgtMode, tgtSkin, tgtSize = 100, tgtHue = 265;
        public boolean tgtFlash = true, tgtGlow = true, tgtLFlash = true;
        public int tgtCount = 6, tgtSpeed = 100;
        public boolean ambOn, ambTimeLock;
        public int ambTime = 6000, ambWeather = 1;
        public boolean fastExp;
        public boolean cdOn, cdHeal = true;
        public int cdScale = 100;
        public boolean killFx;
        public int killMode, killBusDur = 9, killFpvDur = 5;
        public int jcMode, jcSize = 100, jcColor;
        public boolean killCam;
        public boolean aimOn, aimPlayers = true, aimMobs, aimVert = true, trigOn, trigCrit, trigPlayers = true, trigMobs;
        public int aimRange = 35, aimFov = 60, aimStrength = 50, trigRange = 30;

        public String pass = "";
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("untilits.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (Files.exists(f)) {
                try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                    Data d = GSON.fromJson(r, Data.class);
                    if (d != null) D = d;
                }
            }
        } catch (Throwable ignored) { }
        sanitize();
    }

    private static void sanitize() {
        D.accent |= 0xFF000000;
        D.glass &= 0xFFFFFF;
        D.glassPct = clamp(D.glassPct, 30, 98);
        D.dmgScale = clamp(D.dmgScale, 50, 250);
        D.bloodAmount = clamp(D.bloodAmount, 10, 300);
        D.espRange = clamp(D.espRange, 8, 512);
        D.tracerRange = clamp(D.tracerRange, 8, 512);
        D.tgtMode = clamp(D.tgtMode, 0, 10);
        D.tgtCount = clamp(D.tgtCount, 1, 24);
        D.tgtSpeed = clamp(D.tgtSpeed, 30, 300);
        D.optDist = clamp(D.optDist, 0, 256);
        D.hitMode = clamp(D.hitMode, 0, 3);
        D.hitSound = clamp(D.hitSound, 0, 4);
        D.zoomFactor = clamp(D.zoomFactor, 2, 12);
        D.srchRange = clamp(D.srchRange, 8, 32);
        if (D.srchQuery == null) D.srchQuery = "";
        D.rainDensity = clamp(D.rainDensity, 20, 300);
        D.tgtSkin = clamp(D.tgtSkin, 0, 3);
        D.tgtSize = clamp(D.tgtSize, 40, 250);
        D.tgtHue = clamp(D.tgtHue, 0, 360);
        D.ambTime = clamp(D.ambTime, 0, 24000);
        D.ambWeather = clamp(D.ambWeather, 0, 3);
        D.cdScale = clamp(D.cdScale, 50, 200);
        D.killMode = clamp(D.killMode, 0, 6);
        D.killBusDur = clamp(D.killBusDur, 6, 14);
        D.killFpvDur = clamp(D.killFpvDur, 3, 8);
        D.aimRange = clamp(D.aimRange, 20, 60);
        D.aimFov = clamp(D.aimFov, 10, 180);
        D.aimStrength = clamp(D.aimStrength, 10, 100);
        D.trigRange = clamp(D.trigRange, 20, 60);
        D.jcColor = clamp(D.jcColor, 0, 3);
        D.jcMode = clamp(D.jcMode, 0, 5);
        D.jcSize = clamp(D.jcSize, 50, 220);
        D.ambTint = clamp(D.ambTint, 0, 7);
        D.weakHp = clamp(D.weakHp, 1, 20);
        D.chestRange = clamp(D.chestRange, 8, 128);
        D.freecamSpeed = clamp(D.freecamSpeed, 1, 30);
        D.nameRange = clamp(D.nameRange, 8, 512);
        D.hudCorner = clamp(D.hudCorner, 0, 3);
        D.trapScale = clamp(D.trapScale, 40, 250);
        if (D.friends == null) D.friends = new ArrayList<>();
        if (D.binds == null) D.binds = new LinkedHashMap<>();
        if (D.waypoints == null) D.waypoints = new ArrayList<>();
        D.armorWarnPct = clamp(D.armorWarnPct, 5, 50);
        D.wpMaxDist = clamp(D.wpMaxDist, 0, 5000);
        if (D.pass == null) D.pass = "";
    }

    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    public static int glassAlpha() { return D.glassPct * 255 / 100; }

    public static void save() {
        sanitize();
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            try (Writer w = Files.newBufferedWriter(f, StandardCharsets.UTF_8)) { GSON.toJson(D, w); }
        } catch (Throwable t) {
            System.err.println("[untilits] не удалось сохранить конфиг: " + t);
        }
    }

    private static String hash(String code) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(("untilits:" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public static void setPassword(String code) { D.pass = hash(code); save(); }
    public static boolean hasPassword() { return D.pass != null && !D.pass.isEmpty(); }
    public static boolean checkPassword(String code) { return hasPassword() && D.pass.equals(hash(code)); }

    private Cfg() { }
}
