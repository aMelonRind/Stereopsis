package io.github.amelonrind.stereopsis.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.amelonrind.stereopsis.Stereopsis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Unique private static final double PI2 = Math.PI / 2;
    @Unique private static final double D2R = Math.PI / 180;
    @Unique private static final double eyeRadius = 0.1;
    @Unique private static boolean righting = false;
    @Unique private static boolean rendering = false;
    @Unique private static boolean loaded = false;
    @Unique private static final Identifier postId = new Identifier("stereopsis:shaders/post/stereopsis.json");
    @Unique private static PostEffectProcessor post = null;
    @Unique private static Framebuffer back = null;
    @Unique private static Framebuffer left = null;
    @Unique private static Framebuffer right = null;
    @Unique private static double yawOffset = 0.0f;

    @Shadow public abstract void renderWorld(float tickDelta, long limitTime, MatrixStack matrices);

    @Shadow @Final private Camera camera;

    @Shadow @Final MinecraftClient client;

    @Inject(at = @At("TAIL"), method = "loadPrograms")
    public void loadPrograms(ResourceFactory factory, CallbackInfo ci) {
        clear();
        try {
            post = new PostEffectProcessor(client.getTextureManager(), client.getResourceManager(), client.getFramebuffer(), postId);
            post.setupDimensions(client.getWindow().getFramebufferWidth(), client.getWindow().getFramebufferHeight());
            back = post.getSecondaryTarget("back");
            left = post.getSecondaryTarget("left");
            right = post.getSecondaryTarget("right");
            loaded = true;
            Stereopsis.LOGGER.info("Loaded post processor");
        } catch (Exception e) {
            Stereopsis.LOGGER.warn("Failed to load post processor", e);
            clear();
            Stereopsis.LOGGER.info(client.getResourceManager().findResources("stereopsis:", id -> true).toString());
        }
    }

    @Inject(at = @At("TAIL"), method = "clearPrograms")
    public void clearPrograms(CallbackInfo ci) {
        clear();
    }

    @Unique
    private void clear() {
        RenderSystem.assertOnRenderThread();
        loaded = false;
        if (post != null) {
            post.close();
            post = null;
        }
        left = right = null;
    }

    @Inject(at = @At("HEAD"), method = "renderWorld", cancellable = true)
    public void renderStereopsis(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        if (Stereopsis.enabled && !rendering && loaded) {
            rendering = true;
            ci.cancel();
            client.getProfiler().push("stereopsis");

            client.getProfiler().push("blit");
            back.clear(MinecraftClient.IS_SYSTEM_MAC);
            left.clear(MinecraftClient.IS_SYSTEM_MAC);
            right.clear(MinecraftClient.IS_SYSTEM_MAC);
            blit(client.getFramebuffer(), back);
            blit(back, left);
            blit(back, right);

            client.getProfiler().swap("left");
            Stereopsis.framebufferOverride = left;
            left.beginWrite(true);
            righting = false;
            renderWorld(tickDelta, limitTime, new MatrixStack());

            client.getProfiler().swap("right");
            Stereopsis.framebufferOverride = right;
            right.beginWrite(true);
            righting = true;
            renderWorld(tickDelta, limitTime, new MatrixStack());

            Stereopsis.framebufferOverride = null;
            client.getProfiler().swap("render");
            double xOffset = 0.0;
            if (yawOffset > 0.0) {
                xOffset = yawOffset / Math.atan(Math.tan(client.options.getFov().getValue() * D2R / 2.0) * ((double) client.getWindow().getFramebufferWidth() / client.getWindow().getFramebufferHeight())) / 2;
                if (xOffset > 0.25) xOffset = 0.25;
            }
            float finalXOffset = (float) xOffset;
            ((MixinPostEffectProcessor) post).getPasses().forEach(pass -> pass.getProgram().getUniformByNameOrDummy("XOffset").set(finalXOffset));
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            post.render(tickDelta);
            client.getFramebuffer().beginWrite(false);
            RenderSystem.enableCull();

            client.getProfiler().pop();
            client.getProfiler().pop();
            rendering = false;
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;getPitch()F"), method = "renderWorld")
    public void shiftCamera(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        if (Stereopsis.enabled && loaded) {
            ((MixinCamera) camera).callMoveBy(0, 0, righting ? -eyeRadius : eyeRadius);
            if (righting) {
                double to = 0.0;
                if (client.world != null && (client.cameraEntity != null || client.player != null)) {
                    Entity cam = client.cameraEntity != null ? client.cameraEntity : client.player;
                    Vec3d start = cam.getCameraPosVec(tickDelta);
                    Vec3d rot = cam.getRotationVec(tickDelta);
                    Vec3d end = start.add(rot.multiply(16.0));
                    HitResult res = client.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.ANY, cam));
                    double dist = res.getPos().distanceTo(start);
                    if (dist > 0.15) {
                        if (dist > 0.5) {
                            HitResult res2 = ProjectileUtil.raycast(cam, start, start.add(rot.multiply(dist)), cam.getBoundingBox().stretch(rot.multiply(dist)).expand(1.0), e -> !e.isSpectator(), dist);
                            if (res2 != null && res2.getType() != HitResult.Type.MISS) {
                                res = res2;
                                dist = res.getPos().distanceTo(start);
                            }
                        }
                        if (dist < 0.5) dist = 0.5;
                        if (res.getType() != HitResult.Type.MISS) {
                            to = PI2 - Math.atan2(dist, eyeRadius);
                        }
                    }
                }
                yawOffset += (to - yawOffset) / 10.0;
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "onResized")
    public void onResized(int width, int height, CallbackInfo ci) {
        if (loaded) post.setupDimensions(width, height);
    }

    @Unique
    private void blit(@NotNull Framebuffer from, @NotNull Framebuffer to) {
        RenderSystem.assertOnRenderThreadOrInit();
        from.beginRead();
        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, from.fbo);
        GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, to.fbo);
        GlStateManager._glBlitFrameBuffer(0, 0, from.textureWidth, from.textureHeight, 0, 0, to.textureWidth, to.textureHeight, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, 0);
        from.endRead();
        if (from.useDepthAttachment && to.useDepthAttachment) {
            to.copyDepthFrom(from);
        }
    }

}
