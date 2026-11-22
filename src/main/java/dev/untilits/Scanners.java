package dev.untilits;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

/** Hitbox ESP (3D-рамки хитбоксов), Block Search (поиск блоков по названию), Storage Scanner (подсчёт контейнеров). */
public final class Scanners {
    private static final float[] P = new float[3], Q = new float[3];

    // ---- Hitbox ----
    private static final List<Entity> HB = new ArrayList<>();
    private static int hbTimer;

    // ---- Block search ----
    private static List<BlockPos> found = new ArrayList<>(), building = new ArrayList<>();
    private static int slice;
    private static String qLower = "";

    // ---- Storage ----
    private static final int[] COUNTS = new int[6];
    private static final String[] NAMES = {"Сундуки", "Ловушки", "Эндер", "Бочки", "Шалкеры", "Спавнеры"};
    private static final int[] COLS = {0xF0A030, 0xE5484D, 0xB05CFF, 0xC8924A, 0xFF7ACB, 0x3DD6D0};
    private static int scanTimer;

    private Scanners() { }

    public static void tick(MinecraftClient mc) {
        Cfg.Data d = Cfg.D;
        if (mc.world == null || mc.player == null) { HB.clear(); found = new ArrayList<>(); return; }

        if (d.hbOn && hbTimer-- <= 0) {
            hbTimer = 3;
            HB.clear();
            double r2 = (double) d.espRange * d.espRange;
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player || !(e instanceof LivingEntity)) continue;
                boolean player = e instanceof net.minecraft.client.network.AbstractClientPlayerEntity;
                if (!player && !d.hbMobs) continue;
                if (e.squaredDistanceTo(mc.player) > r2) continue;
                HB.add(e);
                if (HB.size() >= 120) break;
            }
        } else if (!d.hbOn) HB.clear();

        if (d.srchOn && !d.srchQuery.isBlank()) searchStep(mc);
        else { found = new ArrayList<>(); building.clear(); slice = 0; }

        if (d.scanOn && scanTimer-- <= 0) {
            scanTimer = 40;
            scanStorage(mc);
        }
    }

    // ---------- Block Search ----------

    private static void searchStep(MinecraftClient mc) {
        Cfg.Data d = Cfg.D;
        String q = d.srchQuery.trim().toLowerCase().replace(' ', '_');
        if (!q.equals(qLower)) { qLower = q; slice = 0; building.clear(); found = new ArrayList<>(); }
        int r = d.srchRange;
        int bx = mc.player.getBlockX(), by = mc.player.getBlockY(), bz = mc.player.getBlockZ();
        int w = r * 2 + 1;
        int slices = 8;
        int x0 = -r + slice * w / slices, x1 = -r + (slice + 1) * w / slices;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = x0; dx < x1; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -r; dy <= r; dy++) {
                    pos.set(bx + dx, by + dy, bz + dz);
                    if (pos.getY() < mc.world.getBottomY() || pos.getY() >= mc.world.getTopYInclusive()) continue;
                    BlockState st = mc.world.getBlockState(pos);
                    if (st.isAir()) continue;
                    Block b = st.getBlock();
                    String id = Registries.BLOCK.getId(b).getPath();
                    if (id.contains(q)) building.add(pos.toImmutable());
                }
            }
        }
        if (++slice >= slices) {
            slice = 0;
            double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
            building.sort((a, c) -> Double.compare(a.getSquaredDistance(px, py, pz), c.getSquaredDistance(px, py, pz)));
            found = building.size() > 150 ? new ArrayList<>(building.subList(0, 150)) : building;
            building = new ArrayList<>();
        }
    }

    // ---------- Storage ----------

    private static void scanStorage(MinecraftClient mc) {
        java.util.Arrays.fill(COUNTS, 0);
        int r = 4;
        int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
        double max2 = 64.0 * 64.0;
        for (int cx = pcx - r; cx <= pcx + r; cx++) {
            for (int cz = pcz - r; cz <= pcz + r; cz++) {
                Chunk ch = mc.world.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(ch instanceof WorldChunk wc)) continue;
                for (BlockEntity be : wc.getBlockEntities().values()) {
                    if (be.getPos().getSquaredDistance(mc.player.getX(), mc.player.getY(), mc.player.getZ()) > max2) continue;
                    int k = be instanceof TrappedChestBlockEntity ? 1
                            : be instanceof ChestBlockEntity ? 0
                            : be instanceof EnderChestBlockEntity ? 2
                            : be instanceof BarrelBlockEntity ? 3
                            : be instanceof ShulkerBoxBlockEntity ? 4
                            : be instanceof MobSpawnerBlockEntity ? 5 : -1;
                    if (k >= 0) COUNTS[k]++;
                }
            }
        }
    }

    // ---------- рендер ----------

    public static void render(DrawContext c, MinecraftClient mc, float tp) {
        Cfg.Data d = Cfg.D;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        if (d.hbOn) {
            for (Entity e : HB) {
                if (!e.isAlive()) continue;
                Vec3d p = e.getLerpedPos(tp);
                double hw = e.getWidth() / 2.0, h = e.getHeight();
                int col = Gfx.withAlpha(Esp.color(e), 230);
                box3d(c, p.x, p.y, p.z, hw, h, col);
                // линия взгляда
                Vec3d eye = new Vec3d(p.x, p.y + e.getStandingEyeHeight(), p.z);
                Vec3d look = e.getRotationVector(e.getPitch(tp), e.getYaw(tp)).multiply(1.6);
                if (Projector.project(eye.x, eye.y, eye.z, P) && Projector.project(eye.x + look.x, eye.y + look.y, eye.z + look.z, Q))
                    TargetEsp.seg(c, P[0], P[1], Q[0], Q[1], 0xFFFF3030);
            }
        }
        if (d.srchOn && !found.isEmpty()) {
            int col = Gfx.withAlpha(Gfx.hsv((Math.abs(qLower.hashCode()) % 360) / 360f, 0.6f, 1f), 255);
            for (BlockPos b : found) Tracers.box(c, b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 0.5, 1.0, col, sw, sh);
        }
    }

    private static void box3d(DrawContext c, double x, double y, double z, double hw, double h, int col) {
        float[][] pts = new float[8][];
        boolean[] ok = new boolean[8];
        for (int i = 0; i < 8; i++) {
            float[] o = new float[3];
            ok[i] = Projector.project(x + ((i & 1) == 0 ? -hw : hw), y + ((i & 2) == 0 ? 0 : h), z + ((i & 4) == 0 ? -hw : hw), o);
            pts[i] = o;
        }
        int[][] edges = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] e : edges) if (ok[e[0]] && ok[e[1]]) TargetEsp.seg(c, pts[e[0]][0], pts[e[0]][1], pts[e[1]][0], pts[e[1]][1], col);
    }

    public static void renderHud(DrawContext c, MinecraftClient mc) {
        Cfg.Data d = Cfg.D;
        if (d.srchOn) {
            String t = "Поиск: " + d.srchQuery + " — " + found.size();
            int w = Gfx.tw(t) + 20;
            Gfx.glass(c, 8, c.getScaledWindowHeight() / 2 + 34, w, 18, 8, true);
            Gfx.text(c, t, 18, c.getScaledWindowHeight() / 2 + 39, Gfx.TEXT);
        }
        if (!d.scanOn) return;
        int rows = 0;
        for (int v : COUNTS) if (v > 0) rows++;
        int h = 26 + Math.max(1, rows) * 12;
        int x = 8, y = c.getScaledWindowHeight() / 2 - h - 6;
        Gfx.glass(c, x, y, 112, h, 9, true);
        Gfx.shadow(c, "Хранилища (64 бл.)", x + 10, y + 8, 0xFFFFFFFF);
        int yy = y + 22;
        if (rows == 0) Gfx.text(c, "ничего нет", x + 10, yy, Gfx.DIM);
        for (int i = 0; i < COUNTS.length; i++) {
            if (COUNTS[i] == 0) continue;
            Gfx.disc(c, x + 13, yy + 4, 3, 0xFF000000 | COLS[i]);
            Gfx.text(c, NAMES[i], x + 22, yy, Gfx.TEXT);
            String n = String.valueOf(COUNTS[i]);
            Gfx.text(c, n, x + 104 - Gfx.tw(n), yy, Gfx.DIM);
            yy += 12;
        }
    }
}
