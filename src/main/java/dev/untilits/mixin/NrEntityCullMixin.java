package dev.untilits.mixin;

import dev.untilits.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.render.entity.EntityRenderManager")
public abstract class NrEntityCullMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$shouldRender(net.minecraft.entity.Entity e, net.minecraft.client.render.Frustum f, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (NoRender.cull(e)) cir.setReturnValue(false);
    }

}
