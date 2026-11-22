package dev.untilits.mixin;

import dev.untilits.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.item.ItemStack")
public abstract class NrGlintMixin {
    @Inject(method = "hasGlint", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$glint(CallbackInfoReturnable<Boolean> cir) {
        if (NoRender.opt(NoRender.O_GLINT)) cir.setReturnValue(false);
    }
}
