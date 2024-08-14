package io.github.amelonrind.stereopsis;

import io.github.amelonrind.stereopsis.config.Config;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class Stereopsis implements ClientModInitializer {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final String MOD_ID = "stereopsis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int HUD_HALF_WIDTH = 125;

    public static boolean devGpuPatch = false;
    public static boolean enabled = false;
    public static boolean loaded = false;
    public static boolean rendering = false;
    public static boolean righting = false;
    public static @Nullable Framebuffer framebufferOverride = null;

    public static float screenAspectRatio = 1.0f;
    public static float xOffset = 0.0f;
    public static Vector3f leftCrosshair;
    public static Vector3f rightCrosshair;

    private static boolean renderingHud = false;

    private static float hudOffset = -1.0f;

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull MutableText translatable(String key) {
        return Text.translatable(MOD_ID + "." + key);
    }

    @Override
    public void onInitializeClient() {
        Config.HANDLER.load();
        Config.get().fixValues();
        if (Config.get().enableOnLaunch) enabled = true;
        KeyBinding key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.stereopsis.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.stereopsis.category"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (key.wasPressed()) {
                enabled = loaded && !enabled;
                if (loaded) LOGGER.info("Stereopsis has been {}", enabled ? "enabled" : "disabled");
                else LOGGER.info("Cannot enable stereopsis: post shader not loaded");
            }
        });
    }

    public static void resetHudOffset() {
        hudOffset = -1.0f;
    }

    public static float getHudOffset() {
        if (hudOffset == -1.0f) {
            int width = mc.getWindow().getScaledWidth();
            hudOffset = width / 4.0f
                    - (Config.get().inverseHudXOffsetDirection ? -xOffset : xOffset) * width
                    + (120 + Config.get().hudOffset) / screenAspectRatio;
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
            context.getMatrices().translate((toRight ? 2 : -2) * Stereopsis.getHudOffset(), 0, 0);
            render.run();
            context.getMatrices().pop();
            mc.getProfiler().pop();
        }
    }

    public static void offsetHudPush(@NotNull DrawContext context, boolean righting) {
        context.getMatrices().push();
        float off = getHudOffset();
        context.getMatrices().translate(righting ? -off : off, 0, 0);
    }

}
