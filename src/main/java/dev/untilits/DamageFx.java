package dev.untilits;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Цифры урона и кровь: по падению здоровья у сущностей вокруг (работает на любом сервере). */
public final class DamageFx {
    private static final Random RND = new Random();
    private static final Map<Integer, Float> LAST = new HashMap<>();
    private static final List<Pop> POPS = new ArrayList<>();
    private static final float[] P = new float[3];
    private static final int LIFE = 32;

    private static final class Pop {
        double x, y, z, ox;
        float val;
        int age;
        boolean heal;
    }

    private DamageFx() { }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) { LAST.clear(); POPS.clear(); return; }
        Set<Integer> seen = new HashSet<>();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || le == mc.player) continue;
            if (le.squaredDistanceTo(mc.player) > 64 * 64) continue;
            int id = le.getId();
            seen.add(id);
            float hp = le.getHealth() + le.getAbsorptionAmount();
            Float old = LAST.put(id, hp);
            if (old == null) continue;
            float delta = old - hp;
            if (delta > 0.05f) hit(mc, le, delta);
        }
        LAST.keySet().retainAll(seen);

        for (int i = POPS.size() - 1; i >= 0; i--) {
            Pop p = POPS.get(i);
            if (++p.age > LIFE) POPS.remove(i);
        }
    }

    private static void hit(MinecraftClient mc, LivingEntity le, float dmg) {
        double cx = le.getX(), cy = le.getY() + le.getHeight() * 0.65, cz = le.getZ();
        if (Cfg.D.damageNumbers && POPS.size() < 80) {
            Pop p = new Pop();
            p.x = cx; p.y = le.getY() + le.getHeight() + 0.1; p.z = cz;
            p.ox = (RND.nextDouble() - 0.5) * 0.6;
            p.val = dmg;
            POPS.add(p);
        }
        if (Cfg.D.blood) {
            int count = (int) Math.min(60, (5 + dmg * 2.5f) * Cfg.D.bloodAmount / 100f);
            ParticleEffect a = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState());
            ParticleEffect b = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.NETHER_WART_BLOCK.getDefaultState());
            try {
                for (int i = 0; i < count; i++) {
                    double vx = (RND.nextDouble() - 0.5) * 0.30;
                    double vy = RND.nextDouble() * 0.22 + 0.02;
                    double vz = (RND.nextDouble() - 0.5) * 0.30;
                    mc.particleManager.addParticle(i % 3 == 0 ? b : a,
                            cx + (RND.nextDouble() - 0.5) * 0.3, cy + (RND.nextDouble() - 0.5) * 0.4,
                            cz + (RND.nextDouble() - 0.5) * 0.3, vx, vy, vz);
                }
            } catch (Throwable ignored) { }
        }
    }

    public static void render(DrawContext c, MinecraftClient mc, float tp) {
        if (!Cfg.D.damageNumbers || POPS.isEmpty()) return;
        for (Pop p : POPS) {
            float age = p.age + tp;
            float t = age / LIFE;
            double rise = 0.9 * (1 - Math.pow(1 - t, 2));
            if (!Projector.project(p.x + p.ox, p.y + rise, p.z, P)) continue;
            int alpha = (int) (255 * Math.min(1f, (1f - t) * 2.2f));
            if (alpha < 12) continue;
            float pop = t < 0.12f ? 0.6f + t / 0.12f * 0.6f : 1.2f - Math.min(0.2f, (t - 0.12f) * 0.6f);
            float dist = Math.max(1f, P[2]);
            float s = Math.max(0.7f, Math.min(1.6f, 4.5f / (float) Math.sqrt(dist))) * pop * Cfg.D.dmgScale / 100f;
            String txt = p.val >= 1f && Math.abs(p.val - Math.round(p.val)) < 0.05f
                    ? "-" + Math.round(p.val) : String.format("-%.1f", p.val).replace(',', '.');
            int col = (alpha << 24) | 0xFF4B4B;
            var m = c.getMatrices();
            m.pushMatrix();
            m.translate(P[0], P[1]);
            m.scale(s, s);
            int w = Gfx.tw(txt);
            c.drawText(mc.textRenderer, txt, -w / 2 + 1, -3, ((alpha / 2) << 24), false);
            c.drawText(mc.textRenderer, txt, -w / 2, -4, col, false);
            m.popMatrix();
        }
    }
}
