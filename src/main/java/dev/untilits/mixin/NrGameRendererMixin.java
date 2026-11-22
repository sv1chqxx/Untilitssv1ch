package dev.untilits.mixin;

import dev.untilits.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.render.GameRenderer")
public abstract class NrGameRendererMixin {
    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$showFloatingItem(CallbackInfo ci) {
        if (NoRender.nr(NoRender.TOTEM)) ci.cancel();
    }

}
