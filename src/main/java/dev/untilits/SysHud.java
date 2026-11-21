package dev.untilits;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

/** Секретный Hud в стиле System: панель с FPS / MS / TPS. Показывается только если включён в скрытом меню. */
public final class SysHud {
    public static int bx, by, bw, bh;   // границы последней отрисовки (для перетаскивания)

    private SysHud() { }

    public static int ping(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null || mc.player == null) return 0;
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return e == null ? 0 : e.getLatency();
    }

    public static void render(DrawContext c, MinecraftClient mc) {
        if (!Cfg.D.sysHud) return;
        String fps = String.valueOf(mc.getCurrentFps());
        String ms = String.valueOf(ping(mc));
        String tps = String.format("%.1f", Tps.get()).replace(',', '.');

        int h = 18, pad = 7, gap = 6, ic = 7, sep = 9;
        int wSystem = ic + 4 + Gfx.tw("System");
        int wFps = ic + 4 + Gfx.tw(fps) + 3 + Gfx.tw("Fps");
        int wMs = ic + 4 + Gfx.tw(ms) + 3 + Gfx.tw("MS");
        int wTps = ic + 4 + Gfx.tw(tps) + 3 + Gfx.tw("TPS");
        int w = pad + wSystem + sep + wFps + sep + wMs + sep + wTps + pad;

        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        int x = Cfg.D.sysX < 0 ? sw - w - 8 : Cfg.D.sysX;
        int y = Cfg.D.sysY;
        x = Cfg.clamp(x, 0, Math.max(0, sw - w));
        y = Cfg.clamp(y, 0, Math.max(0, sh - h));
        bx = x; by = y; bw = w; bh = h;

        Gfx.glass(c, x, y, w, h, 8, false);
        int ty = y + (h - 8) / 2 + 1;
        int iy = y + (h - 7) / 2;
        int cx = x + pad;
        int acc = Gfx.lerp(Cfg.D.accent, 0xFFFFFFFF, 0.35f);

        Gfx.icon(c, cx, iy, Gfx.ICON_SYSTEM, acc);
        Gfx.text(c, "System", cx + ic + 4, ty, Gfx.TEXT);
        cx += wSystem + sep;
        Gfx.icon(c, cx, iy, Gfx.ICON_FPS, acc);
        Gfx.text(c, fps, cx + ic + 4, ty, Gfx.TEXT);
        Gfx.text(c, "Fps", cx + ic + 4 + Gfx.tw(fps) + 3, ty, Gfx.DIM);
        cx += wFps + sep;
        Gfx.icon(c, cx, iy, Gfx.ICON_PING, acc);
        Gfx.text(c, ms, cx + ic + 4, ty, Gfx.TEXT);
        Gfx.text(c, "MS", cx + ic + 4 + Gfx.tw(ms) + 3, ty, Gfx.DIM);
        cx += wMs + sep;
        Gfx.icon(c, cx, iy, Gfx.ICON_TPS, acc);
        Gfx.text(c, tps, cx + ic + 4, ty, Gfx.TEXT);
        Gfx.text(c, "TPS", cx + ic + 4 + Gfx.tw(tps) + 3, ty, Gfx.DIM);
    }
}
