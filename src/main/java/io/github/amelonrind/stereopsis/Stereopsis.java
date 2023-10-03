package io.github.amelonrind.stereopsis;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stereopsis implements ClientModInitializer {
    public static final String MOD_ID = "stereopsis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static boolean enabled = false;
    public static @Nullable Framebuffer framebufferOverride = null;

    @Override
    public void onInitializeClient() {
        KeyBinding key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.stereopsis.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "key.stereopsis.category"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (key.wasPressed()) Stereopsis.enabled = !Stereopsis.enabled;
        });
    }

}
