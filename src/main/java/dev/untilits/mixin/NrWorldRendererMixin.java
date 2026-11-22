package dev.untilits.mixin;

import dev.untilits.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.render.WorldRenderer")
public abstract class NrWorldRendererMixin {
    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$renderWeather(CallbackInfo ci) {
        if (NoRender.nr(NoRender.WEATHER)) ci.cancel();
    }

}
