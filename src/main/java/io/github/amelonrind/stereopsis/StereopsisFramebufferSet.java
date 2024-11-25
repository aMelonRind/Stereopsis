package io.github.amelonrind.stereopsis;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static io.github.amelonrind.stereopsis.Stereopsis.*;

public class StereopsisFramebufferSet implements PostEffectProcessor.FramebufferSet {
    private static final Set<Identifier> TARGETS = Set.of(
            DefaultFramebufferSet.MAIN,
            Identifier.of(MOD_ID, "back"),
            Identifier.of(MOD_ID, "left"),
            Identifier.of(MOD_ID, "right")
    );
    private static final Identifier SHADER_ID = Identifier.of(MOD_ID, "stereopsis");
    private static final StereopsisFramebufferSet INSTANCE = new StereopsisFramebufferSet();
    public static boolean flip = false;
    public static Framebuffer back = null;
    public static Framebuffer cache = null;
    public static Framebuffer left = null;
    public static Framebuffer right = null;
    private static Framebuffer _left = null;
    private static Framebuffer _right = null;

    public @Nullable Handle<Framebuffer> mainH = null;
    public @Nullable Handle<Framebuffer> backH = null;
    public @Nullable Handle<Framebuffer> leftH = null;
    public @Nullable Handle<Framebuffer> rightH = null;

    private static PostEffectProcessor getPost() {
        return mc.getShaderLoader().loadPostEffect(SHADER_ID, TARGETS);
    }

    public static void setFlip(boolean shouldFlip) {
        if (flip != shouldFlip) {
            flip = shouldFlip;
            if (flip) {
                left = _right;
                right = _left;
            } else {
                left = _left;
                right = _right;
            }
        }
    }

    public static void render(ObjectAllocator pool) {
        PostEffectProcessor post = getPost();
        post.setUniforms("XOffset", flip ? -xOffset : xOffset);
        FrameGraphBuilder frameGraphBuilder = new FrameGraphBuilder();
        INSTANCE.update(frameGraphBuilder);
        post.render(frameGraphBuilder, back.textureWidth, back.textureHeight, INSTANCE);
        frameGraphBuilder.run(pool);
    }

    public static void reload() {
        clear();
        try {
            if (getPost() == null) {
                throw new RuntimeException();
            }
            int width = mc.getWindow().getFramebufferWidth();
            int height = mc.getWindow().getFramebufferHeight();
            back = createFramebuffer(width, height);
            cache = createFramebuffer(width, height);
            left = _left = createFramebuffer(width, height);
            right = _right = createFramebuffer(width, height);
            flip = false;
            loaded = true;
            Stereopsis.LOGGER.info("Loaded post processor");
        } catch (Exception e) {
            loaded = false;
            enabled = false;
            Stereopsis.LOGGER.warn("Failed to load post processor", e);
            clear();
        }
    }

    private static SimpleFramebuffer createFramebuffer(int width, int height) {
        SimpleFramebuffer buffer = new SimpleFramebuffer(width, height, true);
        buffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        return buffer;
    }

    public static void resize(int width, int height) {
        if (!loaded) return;
        if (back != null) back.resize(width, height);
        if (cache != null) cache.resize(width, height);
        if (left != null) left.resize(width, height);
        if (right != null) right.resize(width, height);
    }

    public static void clear() {
        RenderSystem.assertOnRenderThread();
        loaded = false;
        if (back != null) {
            back.delete();
            back = null;
        }
        if (cache != null) {
            cache.delete();
            cache = null;
        }
        if (left != null) {
            left.delete();
            left = _left = null;
        }
        if (right != null) {
            right.delete();
            right = _right = null;
        }
    }

    private void update(FrameGraphBuilder builder) {
        mainH = builder.createObjectNode("minecraft:main", mc.getFramebuffer());
        backH = builder.createObjectNode("stereopsis:back", back);
        leftH = builder.createObjectNode("stereopsis:left", _left);
        rightH = builder.createObjectNode("stereopsis:right", _right);
    }

    @Override
    public void set(Identifier id, Handle<Framebuffer> framebuffer) {
        switch (id.getPath()) {
            case "main" -> mainH = framebuffer;
            case "back" -> backH = framebuffer;
            case "left" -> leftH = framebuffer;
            case "right" -> rightH = framebuffer;
        }
    }

    @Nullable
    @Override
    public Handle<Framebuffer> get(Identifier id) {
        return switch (id.getPath()) {
            case "main" -> mainH;
            case "back" -> backH;
            case "left" -> leftH;
            case "right" -> rightH;
            default -> null;
        };
    }
}
