#!/bin/bash
mkdir -p inspect
echo ====ALL
find /home/runner/.gradle "$GITHUB_WORKSPACE/.gradle" "$GITHUB_WORKSPACE/build" -name "*.jar" 2>/dev/null | grep -i -E "minecraft|loom|mapped|named" | head -30
JARS=$(find /home/runner/.gradle "$GITHUB_WORKSPACE/.gradle" -name "*.jar" ! -name "*sources*" 2>/dev/null | grep -E "minecraft" | grep -E "merged|mapped|yarn|named|clientOnly|common")
for jar in $JARS; do
  echo "==== $jar"
  for c in net.minecraft.client.render.Camera net.minecraft.client.input.KeyboardInput net.minecraft.client.input.Input net.minecraft.util.PlayerInput net.minecraft.client.Mouse; do
    javap -p -cp "$jar" $c 2>&1 | head -80
  done
  javap -p -cp "$jar" net.minecraft.entity.Entity 2>&1 | grep -E "changeLookDirection|getLerpedPos"
  javap -p -cp "$jar" net.minecraft.client.network.ClientPlayerEntity 2>&1 | grep -E "tickMovement|changeLook"
  javap -p -cp "$jar" net.minecraft.world.chunk.WorldChunk 2>&1 | grep -E "getBlockEntities"
  javap -p -cp "$jar" net.minecraft.client.world.ClientChunkManager 2>&1 | grep -E "getChunk|getWorldChunk"
done
