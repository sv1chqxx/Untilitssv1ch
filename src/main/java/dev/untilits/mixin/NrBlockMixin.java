package dev.untilits.mixin;

import dev.untilits.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.block.Block")
public abstract class NrBlockMixin {
    /** Simplified Leaves: не рисовать грани между соседними листьями (после включения — F3+A). */
    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true, require = 0)
    private static void untilits$leaves(net.minecraft.block.BlockState state, net.minecraft.block.BlockState neighbor,
                                        net.minecraft.util.math.Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (NoRender.opt(NoRender.O_LEAVES)
                && state.getBlock() instanceof net.minecraft.block.LeavesBlock
                && neighbor.getBlock() instanceof net.minecraft.block.LeavesBlock) cir.setReturnValue(false);
    }
}
