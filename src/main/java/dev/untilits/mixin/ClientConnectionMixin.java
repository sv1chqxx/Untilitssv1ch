package dev.untilits.mixin;

import dev.untilits.Cfg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
    @Inject(method = "channelActive", at = @At("TAIL"), require = 0)
    private void untilits$nodelay(ChannelHandlerContext ctx, CallbackInfo ci) {
        if (!Cfg.D.zeroTcp) return;
        try { ctx.channel().config().setOption(ChannelOption.TCP_NODELAY, true); } catch (Throwable ignored) { }
    }
}
