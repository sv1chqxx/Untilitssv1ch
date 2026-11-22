package dev.untilits.mixin;

import dev.untilits.Freecam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow private boolean thirdPerson;

    @Inject(method = "update", at = @At("TAIL"), require = 0)
    private void untilits$free(World world, Entity entity, boolean tp, boolean inv, float delta, CallbackInfo ci) {
        if (!Freecam.active) return;
        Vec3d p = Freecam.pos(delta);
        setRotation(Freecam.yaw, Freecam.pitch);
        setPos(p.x, p.y, p.z);
        thirdPerson = true; // чтобы было видно своё тело
    }
}
