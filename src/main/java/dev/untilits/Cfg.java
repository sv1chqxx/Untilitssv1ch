package dev.untilits;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
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
        D.weakHp = clamp(D.weakHp, 1, 20);
        D.chestRange = clamp(D.chestRange, 8, 128);
        D.freecamSpeed = clamp(D.freecamSpeed, 1, 30);
        D.nameRange = clamp(D.nameRange, 8, 512);
        D.hudCorner = clamp(D.hudCorner, 0, 3);
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
