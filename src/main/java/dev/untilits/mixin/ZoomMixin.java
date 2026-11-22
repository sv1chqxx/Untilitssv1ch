package dev.untilits.mixin;

import dev.untilits.CombatFx;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class ZoomMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void untilits$zoom(net.minecraft.client.render.Camera cam, float tp, boolean changing, CallbackInfoReturnable<Float> cir) {
        float z = CombatFx.zoom();
        if (z > 1.01f) cir.setReturnValue(cir.getReturnValue() / z);
    }
}
