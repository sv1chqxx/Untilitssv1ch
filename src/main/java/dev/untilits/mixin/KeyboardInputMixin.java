package dev.untilits.mixin;

import dev.untilits.Freecam;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void untilits$freeze(CallbackInfo ci) {
        if (Freecam.active) {
            this.playerInput = PlayerInput.DEFAULT;
            this.movementVector = Vec2f.ZERO;
        }
    }
}
