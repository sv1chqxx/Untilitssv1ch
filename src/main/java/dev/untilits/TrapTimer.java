package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Trap Timer: по звукам пакетов сервера определяет трапку / пласт / драконку / неизбежку и показывает остаток времени над ней. */
public final class TrapTimer {
    private static final class Timer {
        final Vec3d pos; final long created, duration; final boolean scrap; final String label;
        Timer(Vec3d pos, long created, long duration, boolean scrap, String label) {
            this.pos = pos; this.created = created; this.duration = duration; this.scrap = scrap; this.label = label;
        }
        long remaining(long now) { return created + duration - now; }
    }

    private static final List<Timer> ACTIVE = new ArrayList<>();
    private static Vec3d pendDrag, pendNeiz;
    private static long pendDragT, pendNeizT;
    private static ItemStack scrapIcon, kelpIcon;
    private static final float[] P = new float[3];

    private TrapTimer() { }

    public static void clear() { ACTIVE.clear(); pendDrag = null; pendNeiz = null; }

    private static boolean near(float a, float b) { return Math.abs(a - b) < 0.01f; }

    public static void onSound(PlaySoundS2CPacket p) {
        if (!Cfg.D.trapTimer) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!mc.isOnThread()) return;
        var key = p.getSound().getKey();
        if (key.isEmpty()) return;
        String id = key.get().getValue().getPath();
        float pitch = p.getPitch(), vol = p.getVolume();
        double x = p.getX(), y = p.getY(), z = p.getZ();
        long now = System.currentTimeMillis();
        switch (id) {
            case "block.piston.extend" -> {
                if (near(pitch, 0.5f) && near(vol, 0.7f)) add("Trap", new Vec3d(x, y, z), now, 15000, true);
            }
            case "block.anvil.place" -> {
                if (near(pitch, 1.1f) && near(vol, 0.7f)) {
                    Vec3d pos = new Vec3d(x, y, z);
                    add("Plast", pos, now, plastVertical(pos) ? 20000 : 60000, false);
                }
            }
            case "entity.evoker_fangs.attack" -> { pendDrag = new Vec3d(x, y, z); pendDragT = now; }
            case "ui.toast.challenge_complete" -> {
                if (pendDrag != null && now - pendDragT <= 500 && pendDrag.squaredDistanceTo(x, y, z) < 0.5) {
                    add("Drakonka", pendDrag, now, 30000, true);
                    pendDrag = null;
                }
            }
            case "entity.wither.break_block" -> { pendNeiz = new Vec3d(x, y, z); pendNeizT = now; }
            case "entity.ender_dragon.hurt" -> {
                if (pendNeiz != null && now - pendNeizT <= 500 && pendNeiz.squaredDistanceTo(x, y, z) < 0.5) {
                    add("Neizbejka", pendNeiz, now, 15000, true);
                    pendNeiz = null;
                }
            }
            default -> { }
        }
    }

    private static void add(String label, Vec3d pos, long now, long dur, boolean scrap) {
        // эхо-звук окончания: тот же тип в последние 3 с жизни, рядом (≤5 бл.), но не в той же точке
        for (Timer t : ACTIVE) {
            if (!t.label.equals(label) || t.remaining(now) > 3000) continue;
            double d = pos.squaredDistanceTo(t.pos);
            if (d < 0.0625) continue;
            if (d <= 25.0) return;
        }
        ACTIVE.add(new Timer(pos, now, dur, scrap, label));
    }

    private static boolean plastVertical(Vec3d pos) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return false;
        PlayerEntity best = null;
        double bd = 64.0;
        for (AbstractClientPlayerEntity pl : mc.world.getPlayers()) {
            if (pl.getMainHandStack().getItem() != Items.DRIED_KELP && pl.getOffHandStack().getItem() != Items.DRIED_KELP) continue;
            double d = pl.getEntityPos().squaredDistanceTo(pos);
            if (d < bd) { bd = d; best = pl; }
        }
        return best != null && Math.abs(best.getPitch()) >= 45f;
    }

    private static String fmt(long ms) {
        if (ms >= 10000) return (ms / 1000) + "s";
        return (ms / 100 / 10) + "." + (ms / 100 % 10) + "s";
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        if (!Cfg.D.trapTimer || ACTIVE.isEmpty()) return;
        long now = System.currentTimeMillis();
        ACTIVE.removeIf(t -> t.remaining(now) <= 0);
        if (scrapIcon == null) { scrapIcon = new ItemStack(Items.NETHERITE_SCRAP); kelpIcon = new ItemStack(Items.DRIED_KELP); }
        double sc = Cfg.D.trapScale / 100.0;
        for (Timer t : ACTIVE) {
            if (!Projector.project(t.pos.x, t.pos.y + 1.0, t.pos.z, P)) continue;
            float dist = P[2];
            if (dist > 128f) continue;
            float s = (float) Math.max(0.5, Math.min(2.2, 1.8 * sc * (8.0 / (dist + 4.0))));
            long rem = t.remaining(now);
            String text = t.label + " " + fmt(rem);
            int tw = Gfx.tw(text);
            int w = 6 + 16 + 4 + tw + 6, h = 22;
            var m = c.getMatrices();
            m.pushMatrix();
            m.translate(P[0], P[1]);
            m.scale(s, s);
            int x = -w / 2, y = -h / 2;
            Gfx.round(c, x, y, w, h, 6, 0xCC101018);
            c.drawItem(t.scrap ? scrapIcon : kelpIcon, x + 6, y + 3);
            Gfx.shadow(c, text, x + 6 + 16 + 4, y + 7, rem < 5000 ? 0xFFFF6B6B : 0xFFFFFFFF);
            m.popMatrix();
        }
    }
}
