package dev.untilits.mixin;

import dev.untilits.Cfg;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow private int jumpingCooldown;

    @Inject(method = "tickMovement", at = @At("HEAD"), require = 0)
    private void untilits$noJumpDelay(CallbackInfo ci) {
        if (Cfg.D.zeroJump && (Object) this == MinecraftClient.getInstance().player) jumpingCooldown = 0;
    }
}
