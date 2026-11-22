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
import net.minecraft.util.math.Vec3d;
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
        timer = 20;
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
                    Box b = new Box(be.getPos());
                    out.add(new Target(b, col));
                }
            }
        }
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        out.sort((u, v) -> Double.compare(u.box().getCenter().squaredDistanceTo(px, py, pz), v.box().getCenter().squaredDistanceTo(px, py, pz)));
        cache = out.size() > 60 ? new ArrayList<>(out.subList(0, 60)) : out;
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
            Vec3d m = b.getCenter();
            Tracers.box(c, m.x, b.minY, m.z, (b.maxX - b.minX) * 0.5, b.maxY - b.minY, t.color(), sw, sh);
        }
    }
}
