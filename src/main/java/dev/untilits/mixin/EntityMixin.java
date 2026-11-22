package dev.untilits.mixin;

import dev.untilits.Esp;
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
}
