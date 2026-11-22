package dev.untilits;

import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

/** Chest ESP: рамки сундуков, бочек, шалкеров, эндер-сундуков (и спавнеров) поверх мира. */
public final class ChestEsp {
    private record Target(Box box, int color) { }

    private static List<Target> cache = new ArrayList<>();
    private static int timer;
    private static final float[] P = new float[3];

    private ChestEsp() { }

    public static void tick(MinecraftClient mc) {
        if (!Cfg.D.chestEsp || mc.world == null || mc.player == null) { cache = new ArrayList<>(); return; }
        if (timer-- > 0) return;
        timer = 10;
        List<Target> out = new ArrayList<>();
        int r = (Cfg.D.chestRange >> 4) + 1;
        int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
        double max2 = (double) Cfg.D.chestRange * Cfg.D.chestRange;
        for (int cx = pcx - r; cx <= pcx + r; cx++) {
            for (int cz = pcz - r; cz <= pcz + r; cz++) {
                Chunk ch = mc.world.getChunkManager().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(ch instanceof WorldChunk wc)) continue;
                for (BlockEntity be : wc.getBlockEntities().values()) {
                    int col = colorOf(be);
                    if (col == 0) continue;
                    if (be.getPos().getSquaredDistance(mc.player.getX(), mc.player.getY(), mc.player.getZ()) > max2) continue;
                    Box b;
                    try {
                        var shape = be.getCachedState().getOutlineShape(mc.world, be.getPos());
                        b = shape.isEmpty() ? new Box(be.getPos()) : shape.getBoundingBox().offset(be.getPos());
                    } catch (Throwable t) { b = new Box(be.getPos()); }
                    out.add(new Target(b, col));
                    if (out.size() >= 400) break;
                }
            }
        }
        cache = out;
    }

    private static int colorOf(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return 0xFFE5484D;
        if (be instanceof ChestBlockEntity) return 0xFFF0A030;
        if (be instanceof EnderChestBlockEntity) return 0xFFB05CFF;
        if (be instanceof BarrelBlockEntity) return 0xFFC8924A;
        if (be instanceof ShulkerBoxBlockEntity) return 0xFFFF7ACB;
        if (be instanceof MobSpawnerBlockEntity) return Cfg.D.chestSpawners ? 0xFF3DD6D0 : 0;
        return 0;
    }

    public static void render(DrawContext c, MinecraftClient mc, float tp) {
        if (!Cfg.D.chestEsp || cache.isEmpty()) return;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        for (Target t : cache) {
            Box b = t.box();
            float[][] pts = new float[8][];
            boolean[] ok = new boolean[8];
            for (int i = 0; i < 8; i++) {
                double x = (i & 1) == 0 ? b.minX : b.maxX;
                double y = (i & 2) == 0 ? b.minY : b.maxY;
                double z = (i & 4) == 0 ? b.minZ : b.maxZ;
                ok[i] = Projector.project(x, y, z, P);
                pts[i] = new float[]{P[0], P[1]};
            }
            int[][] edges = {{0,1},{2,3},{4,5},{6,7},{0,2},{1,3},{4,6},{5,7},{0,4},{1,5},{2,6},{3,7}};
            for (int[] e : edges) {
                if (!ok[e[0]] || !ok[e[1]]) continue;
                Tracers.line(c, Math.round(pts[e[0]][0]), Math.round(pts[e[0]][1]),
                        Math.round(pts[e[1]][0]), Math.round(pts[e[1]][1]), t.color(), sw, sh);
            }
        }
    }
}
