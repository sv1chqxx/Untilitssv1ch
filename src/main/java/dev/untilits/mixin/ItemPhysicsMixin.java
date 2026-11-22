package dev.untilits.mixin;

import dev.untilits.Cfg;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemPhysicsMixin {
    /** Item Physics: предметы лежат на земле плашмя. */
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At("HEAD"), require = 0)
    private void untilits$flat(ItemEntityRenderState s, MatrixStack m, OrderedRenderCommandQueue q, CameraRenderState cam, CallbackInfo ci) {
        if (!Cfg.D.itemPhys) return;
        m.translate(0.0, 0.06, 0.0);
        m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));
    }
}
