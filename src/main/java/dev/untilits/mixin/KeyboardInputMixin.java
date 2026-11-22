package dev.untilits.mixin;

import dev.untilits.Cfg;
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
    private static long fT, bT, lT, rT;
    private static boolean pf, pb, pl, pr;

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void untilits$tick(CallbackInfo ci) {
        if (Freecam.active) {
            this.playerInput = PlayerInput.DEFAULT;
            this.movementVector = Vec2f.ZERO;
            return;
        }
        if (!Cfg.D.zeroSnap) return;
        PlayerInput in = this.playerInput;
        boolean f = in.forward(), b = in.backward(), l = in.left(), r = in.right();
        long now = System.nanoTime();
        if (f && !pf) fT = now;
        if (b && !pb) bT = now;
        if (l && !pl) lT = now;
        if (r && !pr) rT = now;
        pf = f; pb = b; pl = l; pr = r;
        if (!(f && b) && !(l && r)) return;
        boolean nf = f, nb = b, nl = l, nr = r;
        if (f && b) { if (fT > bT) nb = false; else if (bT > fT) nf = false; else { nf = false; nb = false; } }
        if (l && r) { if (lT > rT) nr = false; else if (rT > lT) nl = false; else { nl = false; nr = false; } }
        this.playerInput = new PlayerInput(nf, nb, nl, nr, in.jump(), in.sneak(), in.sprint());
        this.movementVector = new Vec2f(mult(nl, nr), mult(nf, nb)).normalize();
    }

    private static float mult(boolean pos, boolean neg) { return pos == neg ? 0f : (pos ? 1f : -1f); }
}
