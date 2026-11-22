package dev.untilits;

import dev.untilits.ui.MainScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class Untilits implements ClientModInitializer {
    private static KeyBinding menuKey;
    private static boolean pDown;

    @Override
    public void onInitializeClient() {
        Cfg.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.untilits.menu", GLFW.GLFW_KEY_P,
                KeyBinding.Category.create(Identifier.of("untilits", "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            HudModules.tick(mc);
            DamageFx.tick(mc);
            boolean down = mc.getWindow() != null
                    && InputUtil.isKeyPressed(mc.getWindow(), GLFW.GLFW_KEY_P);
            boolean edge = down && !pDown;
            pDown = down;
            if (edge && mc.currentScreen != null
                    && mc.currentScreen.getClass().getSimpleName().equals("SocialInteractionsScreen")) {
                mc.setScreen(null);
            }
            if (edge && mc.currentScreen == null && mc.world != null) mc.setScreen(new MainScreen());
            while (menuKey.wasPressed()) {
                // P в ваниле открывает «Социальные взаимодействия» — перехватываем и их
                boolean free = mc.currentScreen == null
                        || mc.currentScreen.getClass().getSimpleName().equals("SocialInteractionsScreen");
                if (free) mc.setScreen(new MainScreen());
            }
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of("untilits", "hud"),
                (ctx, tickCounter) -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
                    float tp = tickCounter.getTickProgress(false);
                    HudModules.render(ctx, mc);
                    if (Projector.setup(mc, tp, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight())) {
                        DamageFx.render(ctx, mc, tp);
                        Tracers.render(ctx, mc, tp);
                        NameTags.render(ctx, mc, tp);
                    }
                    SysHud.render(ctx, mc);
                });

        // .password set <1-4 цифры> — обрабатывается локально и в чат НЕ отправляется
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            String m = message.trim();
            if (!m.equals(".password") && !m.startsWith(".password ")) return true;
            MinecraftClient mc = MinecraftClient.getInstance();
            String[] p = m.split("\\s+");
            String reply;
            if (p.length == 3 && p[1].equals("set") && p[2].matches("[0-9]{1,4}")) {
                Cfg.setPassword(p[2]);
                reply = "Код сохранён";
            } else {
                reply = ".password set <1-4 цифры>";
            }
            if (mc.inGameHud != null) mc.inGameHud.setOverlayMessage(Text.literal(reply), false);
            return false;
        });
    }
}
