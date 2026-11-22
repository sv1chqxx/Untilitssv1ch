package dev.untilits.mixin;

import dev.untilits.Esp;
import dev.untilits.Freecam;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$glow(CallbackInfoReturnable<Boolean> cir) {
        if (Esp.shouldGlow((Entity) (Object) this)) cir.setReturnValue(true);
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$glowColor(CallbackInfoReturnable<Integer> cir) {
        if (Esp.shouldGlow((Entity) (Object) this)) cir.setReturnValue(Esp.color((Entity) (Object) this));
    }

    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$look(double dx, double dy, CallbackInfo ci) {
        if (Freecam.active && (Object) this == MinecraftClient.getInstance().player) {
            Freecam.look(dx, dy);
            ci.cancel();
        }
    }
}
