#!/bin/bash
JAR=$(find /home/runner/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged -name "*.jar" ! -name "*sources*" | head -1)
p() { echo "#### $1 [$2]"; javap -p -cp "$JAR" $1 2>&1 | grep -E "$2"; }
p net.minecraft.client.render.entity.ItemEntityRenderer "render|class"
p net.minecraft.client.gui.screen.ingame.HandledScreen "focusedSlot|drawMouseoverTooltip"
p net.minecraft.component.type.ContainerComponent "stream|iterate|copyFirstStack|public"
p net.minecraft.client.gui.DrawContext "drawStackOverlay|drawItem\("
p net.minecraft.client.render.GameRenderer "getFov"
p net.minecraft.client.render.Camera "setRotation|update|getYaw|getPitch"
p net.minecraft.entity.Entity "changeLookDirection"
p net.minecraft.client.network.ClientPlayerEntity "swingHand|attack"
p net.minecraft.client.sound.PositionedSoundInstance "master|ui"
