package dev.untilits.mixin;

import dev.untilits.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.hud.InGameHud")
public abstract class NrHudMixin {
    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$renderScoreboardSidebar(CallbackInfo ci) {
        if (NoRender.nr(NoRender.SCOREBOARD)) ci.cancel();
    }
    @Inject(method = "renderTitleAndSubtitle", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$renderTitleAndSubtitle(CallbackInfo ci) {
        if (NoRender.nr(NoRender.TITLES)) ci.cancel();
    }
    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$renderVignetteOverlay(CallbackInfo ci) {
        if (NoRender.nr(NoRender.VIGNETTE)) ci.cancel();
    }
    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$renderPortalOverlay(CallbackInfo ci) {
        if (NoRender.nr(NoRender.PORTAL)) ci.cancel();
    }
    @Inject(method = "renderBossBarHud", at = @At("HEAD"), cancellable = true, require = 0)
    private void untilits$renderBossBarHud(CallbackInfo ci) {
        if (NoRender.nr(NoRender.BOSSBAR)) ci.cancel();
    }

}
