package dev.untilits.ui;

import dev.untilits.Cfg;
import dev.untilits.Gfx;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;

/** Ввод кода с клавиатуры (1–4 цифры), Enter — подтвердить, Backspace — стереть. */
public final class PasswordScreen extends UiScreen {
    private final StringBuilder code = new StringBuilder();
    private String msg = "";
    private long errAt = 0;

    public PasswordScreen() { super("Код"); }

    @Override
    protected void init() { layoutPanel(250, 112); }

    @Override
    protected boolean showBindHint() { return false; }

    @Override
    protected void header(DrawContext c, int mx, int my) {
        Gfx.shadow(c, "Код доступа", px + 16, py + 14, 0xFFFFFFFF);
        boolean err = System.currentTimeMillis() - errAt < 600;
        int slotW = 28, gap = 8, total = 4 * slotW + 3 * gap;
        int sx = px + (pw - total) / 2, sy = py + 38;
        for (int i = 0; i < 4; i++) {
            int bx = sx + i * (slotW + gap);
            Gfx.round(c, bx, sy, slotW, 26, 7, 0x22FFFFFF);
            Gfx.ring(c, bx, sy, slotW, 26, 7, err ? 0xAAFF5A5A : (i == code.length() ? Gfx.withAlpha(Cfg.D.accent, 230) : 0x44FFFFFF));
            if (i < code.length()) Gfx.disc(c, bx + slotW / 2, sy + 13, 4, 0xFFFFFFFF);
        }
        if (!msg.isEmpty()) Gfx.text(c, msg, px + (pw - Gfx.tw(msg)) / 2, py + 76, err ? 0xFFFF8A8A : Gfx.DIM);
        String hint = Cfg.hasPassword() ? "Enter — войти" : "Код не задан";
        if (msg.isEmpty()) Gfx.text(c, hint, px + (pw - Gfx.tw(hint)) / 2, py + 76, Gfx.DIM);
        if (!Cfg.hasPassword()) {
            String h2 = "В чате: .password set 1234";
            Gfx.text(c, h2, px + (pw - Gfx.tw(h2)) / 2, py + 90, Gfx.DIM);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int k = input.key();
        int digit = (k >= 48 && k <= 57) ? k - 48 : (k >= 320 && k <= 329 ? k - 320 : -1);
        if (digit >= 0) {
            if (code.length() < 4) code.append(digit);
            msg = "";
            return true;
        }
        if (k == 259) { // Backspace
            if (code.length() > 0) code.deleteCharAt(code.length() - 1);
            return true;
        }
        if (k == 257 || k == 335) { // Enter
            if (Cfg.checkPassword(code.toString())) {
                client.setScreen(new SecretScreen());
            } else {
                msg = Cfg.hasPassword() ? "Неверный код" : "Сначала задай код в чате";
                errAt = System.currentTimeMillis();
                code.setLength(0);
            }
            return true;
        }
        return super.keyPressed(input);
    }
}
