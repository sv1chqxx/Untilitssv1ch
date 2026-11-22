package dev.untilits.mixin;

import dev.untilits.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.entity.LivingEntity")
public abstract class NrStuckMixin {
    @Inject(method = "getStuckArrowCount", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$stuck(CallbackInfoReturnable<Integer> cir) {
        if (NoRender.nr(NoRender.ARROWS)) cir.setReturnValue(0);
    }
}
