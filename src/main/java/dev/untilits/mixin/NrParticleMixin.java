package dev.untilits.mixin;

import dev.untilits.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.particle.ParticleManager")
public abstract class NrParticleMixin {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$effect(net.minecraft.particle.ParticleEffect eff, double x, double y, double z, double vx, double vy, double vz, CallbackInfoReturnable<net.minecraft.client.particle.Particle> cir) {
        if (!NoRender.nr(NoRender.SMOKE)) return;
        var t = eff.getType();
        if (t == net.minecraft.particle.ParticleTypes.SMOKE || t == net.minecraft.particle.ParticleTypes.LARGE_SMOKE
                || t == net.minecraft.particle.ParticleTypes.CAMPFIRE_COSY_SMOKE || t == net.minecraft.particle.ParticleTypes.CAMPFIRE_SIGNAL_SMOKE)
            cir.setReturnValue(null);
    }

    @Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$particle(net.minecraft.client.particle.Particle p, CallbackInfo ci) {
        if (NoRender.nr(NoRender.BREAK) && p.getClass().getSimpleName().contains("BlockDust")) ci.cancel();
    }
}
