package dev.untilits;

import dev.untilits.mixin.HandledScreenAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import java.util.List;

/** Предпросмотр содержимого шалкера при наведении в любом инвентаре. */
public final class ShulkerPreview {
    private ShulkerPreview() { }

    public static void render(Screen screen, DrawContext c, int mx, int my) {
        if (!Cfg.D.shulkerPrev || !(screen instanceof HandledScreen<?>)) return;
        Slot s = ((HandledScreenAccessor) screen).untilits$focused();
        if (s == null || !s.hasStack()) return;
        ItemStack st = s.getStack();
        ContainerComponent cc = st.get(DataComponentTypes.CONTAINER);
        if (cc == null) return;
        List<ItemStack> items = cc.stream().toList();
        if (items.stream().allMatch(ItemStack::isEmpty)) return;
        int cols = 9, rows = 3, cell = 18;
        int w = cols * cell + 12, h = rows * cell + 12;
        int x = mx + 12, y = my - h - 4;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        if (x + w > sw - 4) x = mx - w - 8;
        if (y < 4) y = my + 20;
        if (y + h > sh - 4) y = sh - 4 - h;
        MinecraftClient mc = MinecraftClient.getInstance();
        Gfx.glass(c, x, y, w, h, 8, true);
        for (int i = 0; i < Math.min(27, items.size()); i++) {
            int ix = x + 6 + (i % cols) * cell, iy = y + 6 + (i / cols) * cell;
            Gfx.round(c, ix, iy, 17, 17, 3, 0x14FFFFFF);
            ItemStack it = items.get(i);
            if (it.isEmpty()) continue;
            c.drawItem(it, ix, iy);
            c.drawStackOverlay(mc.textRenderer, it, ix, iy);
        }
    }
}
