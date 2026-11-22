package dev.untilits.mixin;

import dev.untilits.Cfg;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow protected int attackCooldown;

    @Inject(method = "doAttack", at = @At("HEAD"), require = 0)
    private void untilits$preAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Cfg.D.zeroRaycast) refresh();
    }

    @Inject(method = "doAttack", at = @At("RETURN"), require = 0)
    private void untilits$postAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Cfg.D.zeroAttack) attackCooldown = 0;
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), require = 0)
    private void untilits$preUse(CallbackInfo ci) {
        if (Cfg.D.zeroRaycast) refresh();
    }

    private void refresh() {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        if (mc.player == null || mc.world == null || mc.gameRenderer == null) return;
        mc.gameRenderer.updateCrosshairTarget(mc.getRenderTickCounter().getTickProgress(false));
    }
}
