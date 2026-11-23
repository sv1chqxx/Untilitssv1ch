package dev.untilits.mixin;

import dev.untilits.Aim;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.client.Mouse.class)
public abstract class MouseMixin {
    @Redirect(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"), require = 0)
    private void untilits$look(ClientPlayerEntity p, double dx, double dy) {
        double[] r = Aim.adjust(dx, dy);
        p.changeLookDirection(r[0], r[1]);
    }
}
