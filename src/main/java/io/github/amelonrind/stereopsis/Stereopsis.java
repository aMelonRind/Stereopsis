package io.github.amelonrind.stereopsis;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class Stereopsis implements ClientModInitializer {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final String MOD_ID = "stereopsis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int HUD_HALF_WIDTH = 125;

    public static boolean enabled = false;
    public static boolean loaded = false;
    public static boolean rendering = false;
    public static boolean righting = false;
    public static @Nullable Framebuffer framebufferOverride = null;

    public static float screenAspectRatio = 1.0f;
    public static float xOffset = 0.0f;
    public static Vector4f leftCrosshair;
    public static Vector4f rightCrosshair;

    private static boolean renderingHud = false;

    private static float hudOffset = -1.0f;

    @Override
    public void onInitializeClient() {
        KeyBinding key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.stereopsis.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.stereopsis.category"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (key.wasPressed()) {
                enabled = loaded && !enabled;
                if (loaded) LOGGER.info("Stereopsis has been " + (enabled ? "enabled" : "disabled"));
                else LOGGER.info("Cannot enable stereopsis: post shader not loaded");
            }
        });
    }

    public static void resetHudOffset() {
        hudOffset = -1.0f;
    }

    public static float getHudOffset(@Nullable DrawContext context) {
        if (hudOffset == -1.0f) {
            int width = context == null ? mc.getWindow().getScaledWidth() : context.getScaledWindowWidth();
            hudOffset = width / 4.0f - xOffset * width + 120 / screenAspectRatio;
            if (hudOffset < HUD_HALF_WIDTH) hudOffset = HUD_HALF_WIDTH;
            int max = width / 2 - HUD_HALF_WIDTH;
            if (hudOffset > max) hudOffset = max;
        }
        return hudOffset;
    }

    public static void moveHud(String name, DrawContext context, CallbackInfo ci, Runnable render) {
        if (enabled && !renderingHud) {
            ci.cancel();
            mc.getProfiler().push("stereopsis-" + name);
            renderingHud = true;

            offsetHudPush(context, false);
            render.run();
            context.getMatrices().pop();

            offsetHudPush(context, true);
            render.run();
            context.getMatrices().pop();

            renderingHud = false;
            mc.getProfiler().pop();
        }
    }

    public static void moveSideHud(String name, DrawContext context, boolean toRight, Runnable render) {
        if (enabled) {
            mc.getProfiler().push("stereopsis-" + name);
            context.getMatrices().push();
            context.getMatrices().translate((toRight ? 2 : -2) * Stereopsis.getHudOffset(context), 0, 0);
            render.run();
            context.getMatrices().pop();
            mc.getProfiler().pop();
        }
    }

    public static void offsetHudPush(@NotNull DrawContext context, boolean righting) {
        context.getMatrices().push();
        float off = getHudOffset(context);
        context.getMatrices().translate(righting ? -off : off, 0, 0);
    }

    // for debug purpose
    public static void renderMatrix(TextRenderer textRenderer, DrawContext context, Matrix4f mat, int x, int y) {
        if (mat == null) return;
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) {
            String str = Float.toString(mat.get(c, r));
            if (str.length() > 6) str = str.substring(0, 6);
            context.drawTextWithShadow(textRenderer, str, x + c * 48, y + r * 12, 0xffffff);
        }
    }

    // for debug purpose
    public static void renderVector(TextRenderer textRenderer, DrawContext context, Vector4f vec, int x, int y) {
        if (vec == null) return;
        for (int i = 0; i < 4; i++) {
            String str = Float.toString(vec.get(i));
            if (str.length() > 6) str = str.substring(0, 6);
            context.drawTextWithShadow(textRenderer, str, x + i * 48, y, 0xffffff);
        }
    }

}
