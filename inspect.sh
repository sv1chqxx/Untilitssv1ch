#!/bin/bash
JAR=$(find /home/runner/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged -name "*.jar" ! -name "*sources*" | head -1)
echo "JAR=$JAR"
p() { echo "#### $1 [$2]"; javap -p -cp "$JAR" $1 2>&1 | grep -E "$2"; }
p net.minecraft.client.render.GameRenderer "fov|Fov|getCamera|updateCrosshair|Projection|Matrix"
p net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket "public"
p net.minecraft.client.network.ClientPlayNetworkHandler "onPlaySound|onPlayerList"
p net.minecraft.entity.LivingEntity "jumpingCooldown|tickMovement|getOffHandStack|getMainHandStack"
p net.minecraft.client.MinecraftClient "attackCooldown|doAttack|doItemUse|getWindow|getRenderTickCounter"
p net.minecraft.client.util.InputUtil "isKeyPressed|fromKeyCode|fromTranslationKey|UNKNOWN"
p 'net.minecraft.client.util.InputUtil$Key' "public"
p net.minecraft.client.input.KeyInput "public|key"
p net.minecraft.client.input.CharInput "public"
p net.minecraft.client.gui.widget.TextFieldWidget "TextFieldWidget\(|setText|getText|setPlaceholder|charTyped|keyPressed|setMaxLength|setFocused|setEditable"
p net.minecraft.client.gui.screen.Screen "keyPressed|charTyped|addDrawableChild|addSelectableChild|setFocused|renderWidgets|render\("
p net.minecraft.client.util.Window "getHandle"
p net.minecraft.client.option.GameOptions "getFov|fov"
p net.minecraft.client.render.Camera 'Projection'
p 'net.minecraft.client.render.Camera$Projection' "public"
