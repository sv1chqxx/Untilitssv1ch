#!/bin/bash
JAR=$(find /home/runner/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged -name "*.jar" ! -name "*sources*" | head -1)
echo "JAR=$JAR"
p() { echo "#### $1 [$2]"; javap -p -cp "$JAR" $1 2>&1 | grep -E "$2"; }
p net.minecraft.client.gui.hud.InGameHud "renderVignette|renderScoreboard|renderTitle|renderSpyglass|renderPortal|renderOverlay|renderMiscOverlays|renderCrosshair"
p net.minecraft.client.gui.hud.BossBarHud "render"
p net.minecraft.client.render.item.HeldItemRenderer "renderFireOverlay|renderUnderwater|renderInWall"
p net.minecraft.client.gui.hud.InGameOverlayRenderer "render|Overlay"
p net.minecraft.client.render.GameRenderer "showFloatingItem"
p net.minecraft.client.particle.ParticleManager "addBlockBreak|addParticle|addBlockBreaking"
p net.minecraft.client.render.entity.EntityRenderManager "shouldRender"
p net.minecraft.client.render.entity.EntityRenderDispatcher "shouldRender"
p net.minecraft.client.render.WorldRenderer "renderWeather|tickRainSplash|addWeather|renderClouds|renderSky"
p net.minecraft.client.render.block.entity.BlockEntityRenderManager "render"
p net.minecraft.client.world.ClientWorld "setRainGradient|setThunderGradient|setTime|getLevelProperties"
p net.minecraft.client.render.fog.FogRenderer "applyFog|getFogColor"
p net.minecraft.client.gui.hud.InGameHud "render|Hud"
