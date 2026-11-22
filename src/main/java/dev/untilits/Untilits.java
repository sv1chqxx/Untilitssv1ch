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
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("untilits", "main"));
    private static KeyBinding menuKey;
    private static KeyBinding freecamKey;
    private static boolean pDown;

    @Override
    public void onInitializeClient() {
        Cfg.load();

        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.untilits.menu", GLFW.GLFW_KEY_P,
                CATEGORY));

        freecamKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.untilits.freecam", GLFW.GLFW_KEY_V,
                CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            HudModules.tick(mc);
            DamageFx.tick(mc);
            ChestEsp.tick(mc);
            Binds.tick(mc);
            Tracers.tick(mc);
            Waypoints.tick(mc);
            Predictions.tick(mc);
            ArmorWatch.tick(mc);
            TargetEsp.tick(mc);
            VisualFx.tick(mc);
            if (mc.world == null) TrapTimer.clear();
            Freecam.tick(mc);
            while (freecamKey.wasPressed()) { if (mc.world != null) Freecam.set(!Freecam.active); }
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
                    PotionHud.render(ctx, mc);
                    VisualFx.render(ctx, mc);
                    ArmorWatch.renderWarning(ctx, mc);
                    if (Projector.setup(mc, tp, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight())) {
                        DamageFx.render(ctx, mc, tp);
                        Predictions.render(ctx, mc, tp);
                        TargetEsp.render(ctx, mc, tp);
                        Tracers.render(ctx, mc, tp);
                        ChestEsp.render(ctx, mc, tp);
                        Waypoints.render(ctx, mc);
                        TrapTimer.render(ctx, mc);
                        ArmorWatch.renderOverhead(ctx, mc, tp);
                        NameTags.render(ctx, mc, tp);
                    }
                    SysHud.render(ctx, mc);
                    KeybindsHud.render(ctx, mc);
                });

        // .password set <1-4 цифры> — обрабатывается локально и в чат НЕ отправляется
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            String m = message.trim();
            MinecraftClient mc = MinecraftClient.getInstance();
            String[] p = m.split("\\s+");
            String reply;
            if (m.equals(".friend") || m.startsWith(".friend ")) {
                if (p.length == 3 && p[1].equals("add")) reply = Friends.add(p[2]) ? "Друг добавлен: " + p[2] : "Не удалось добавить";
                else if (p.length == 3 && p[1].equals("remove")) reply = Friends.remove(p[2]) ? "Удалён: " + p[2] : "Нет такого друга";
                else if (p.length == 2 && p[1].equals("list")) reply = Cfg.D.friends.isEmpty() ? "Друзей нет" : String.join(", ", Cfg.D.friends);
                else reply = ".friend add|remove <ник> | list";
                if (mc.inGameHud != null) mc.inGameHud.setOverlayMessage(Text.literal(reply), false);
                return false;
            }
            if (m.equals(".wp") || m.startsWith(".wp ")) {
                if (mc.player == null) return false;
                if (p.length >= 2 && p[1].equals("add")) {
                    Cfg.WP w = Waypoints.addHere(mc, p.length > 2 ? m.substring(m.indexOf("add") + 4).trim() : "");
                    reply = w == null ? "Не удалось" : "Метка: " + w.name;
                } else if (p.length >= 3 && p[1].equals("del")) {
                    reply = Waypoints.remove(m.substring(m.indexOf("del") + 4).trim()) ? "Удалено" : "Нет такой метки";
                } else if (p.length == 2 && p[1].equals("list")) {
                    reply = Waypoints.list().isEmpty() ? "Меток нет" : Waypoints.list().size() + " меток (см. меню P)";
                } else reply = ".wp add [имя] | del <имя> | list";
                if (mc.inGameHud != null) mc.inGameHud.setOverlayMessage(Text.literal(reply), false);
                return false;
            }
            if (!m.equals(".password") && !m.startsWith(".password ")) return true;
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
