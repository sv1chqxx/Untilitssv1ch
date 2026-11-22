package dev.untilits.mixin;

import dev.untilits.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.hud.InGameOverlayRenderer")
public abstract class NrOverlayMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$renderFireOverlay(CallbackInfo ci) {
        if (NoRender.nr(NoRender.FIRE)) ci.cancel();
    }
    @Inject(method = "renderUnderwaterOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$renderUnderwaterOverlay(CallbackInfo ci) {
        if (NoRender.nr(NoRender.WATER)) ci.cancel();
    }

}
