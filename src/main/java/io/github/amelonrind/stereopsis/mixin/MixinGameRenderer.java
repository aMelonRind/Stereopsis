package io.github.amelonrind.stereopsis.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.amelonrind.stereopsis.config.Config;
import io.github.amelonrind.stereopsis.Stereopsis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30C;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static io.github.amelonrind.stereopsis.Stereopsis.*;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Shadow @Final MinecraftClient client;

    @Shadow @Final private Camera camera;

    @Shadow public abstract void renderWorld(float tickDelta, long limitTime, MatrixStack matrices);

    @Shadow protected abstract void renderFloatingItem(int scaledWidth, int scaledHeight, float tickDelta);

    @Unique private static final double PI2 = Math.PI / 2;
    @Unique private static final double D2R = Math.PI / 180;
    @Unique private static final double eyeRadius = 0.1;
    @Unique private static final Identifier postId = new Identifier("stereopsis:shaders/post/stereopsis.json");
    @Unique private static PostEffectProcessor post = null;
    @Unique private static boolean flip = false;
    @Unique private static Framebuffer back = null;
    @Unique private static Framebuffer left = null;
    @Unique private static Framebuffer right = null;
    @Unique private static long lastFrameTime = System.nanoTime();

    @Unique private static Vec3d crosshair = null;
    @Unique private static Vec3d leftCrosshairPos = new Vec3d(0, 0, 0);
    @Unique private static Vec3d rightCrosshairPos = new Vec3d(0, 0, 0);
    @Unique private static final Matrix4f leftMatrix = new Matrix4f();
    @Unique private static final Matrix4f rightMatrix = new Matrix4f();

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
            loaded = false;
            enabled = false;
            Stereopsis.LOGGER.warn("Failed to load post processor", e);
            clear();
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
        screenAspectRatio = (float) client.getWindow().getFramebufferWidth() / client.getWindow().getFramebufferHeight();
        Stereopsis.resetHudOffset();
        if (enabled && !rendering) {
            rendering = true;
            ci.cancel();

            if (Config.HANDLER.instance().flipView != flip) {
                Framebuffer temp = left;
                left = right;
                right = temp;
                flip = !flip;
            }

            client.getProfiler().push("stereopsis-world");
            crosshair = null;
            leftCrosshair = null;
            rightCrosshair = null;

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
            renderWorld(tickDelta, limitTime, matrices);
            client.worldRenderer.drawEntityOutlinesFramebuffer();

            client.getProfiler().swap("right");
            Stereopsis.framebufferOverride = right;
            right.beginWrite(true);
            righting = true;
            renderWorld(tickDelta, limitTime, new MatrixStack());
            client.worldRenderer.drawEntityOutlinesFramebuffer();

            Framebuffer outlines = client.worldRenderer.getEntityOutlinesFramebuffer();
            if (outlines != null) outlines.clear(MinecraftClient.IS_SYSTEM_MAC);
            Stereopsis.framebufferOverride = null;
            client.getProfiler().swap("render");
//            if (yawOffset > 0.0) {
//                xOffset = Math.min(
//                        (float) (yawOffset / Math.atan(Math.tan(client.options.getFov().getValue() * D2R / 2.0) * screenAspectRatio) / 2),
//                        Config.HANDLER.instance().maxXOffset
//                );
//            }
            ((MixinAccessPostEffectProcessor) post).getPasses().forEach(pass -> pass.getProgram().getUniformByNameOrDummy("XOffset").set(xOffset));
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

    @Inject(at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V"), method = "renderWorld")
    public void shiftCamera(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        if (enabled) {
            ((MixinAccessCamera) camera).callMoveBy(0, 0, righting ? -eyeRadius : eyeRadius);
            if (!righting) {
                double to = 0.0;
                if (client.world != null && (client.cameraEntity != null || client.player != null)) {
                    Entity cam = client.cameraEntity != null ? client.cameraEntity : client.player;
                    Vec3d start = cam.getCameraPosVec(tickDelta);
                    Vec3d rot = cam.getRotationVec(tickDelta);
                    Vec3d end = start.add(rot.multiply(16.0));
                    HitResult res = client.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.ANY, cam));
                    double dist = res.getPos().distanceTo(start);
                    if (dist < 0.15) {
                        BlockPos pos = ((BlockHitResult) res).getBlockPos();
                        if (!client.world.getFluidState(pos).isEmpty()) {
                            res = client.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, cam));
                            dist = res.getPos().distanceTo(start);
                        } else if (client.world.getBlockState(pos).getOutlineShape(client.world, pos).isEmpty()) {
                            res = client.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, cam));
                            dist = res.getPos().distanceTo(start);
                        }
                    }
                    if (dist > 0.15) {
                        if (dist > 0.5) {
                            HitResult res2 = ProjectileUtil.raycast(cam, start, start.add(rot.multiply(dist)), cam.getBoundingBox().stretch(rot.multiply(dist)).expand(1.0), e -> !e.isSpectator() && e.canHit(), dist);
                            if (res2 != null && res2.getType() != HitResult.Type.MISS) {
                                res = res2;
                                dist = res.getPos().distanceTo(start);
                            }
                        }
                        if (dist < 0.5) dist = 0.5;
                        if (res.getType() != HitResult.Type.MISS) {
                            to = Math.min(
                                    ((PI2 - Math.atan2(dist, eyeRadius)) / Math.atan(Math.tan(client.options.getFov().getValue() * D2R / 2.0) * screenAspectRatio) / 2),
                                    Config.HANDLER.instance().maxXOffset
                            );
                        }
                    }
                    if (client.crosshairTarget == null || client.crosshairTarget.getType() == HitResult.Type.MISS) {
                        crosshair = res.getPos();
                    } else {
                        crosshair = client.crosshairTarget.getPos();
                    }
                }
                float multiplier = Config.HANDLER.instance().offsetSpeed;
                if (multiplier < 0) xOffset = (float) to;
                else if (multiplier == 0) xOffset = 0;
                else if (xOffset == to) lastFrameTime = 0;
                else {
                    if (Math.abs(xOffset - to) < Float.MIN_VALUE) {
                        xOffset = (float) to;
                        lastFrameTime = 0;
                    } else {
                        if (lastFrameTime == 0) lastFrameTime = System.currentTimeMillis();
                        else {
                            long elapsed = -(lastFrameTime - (lastFrameTime = System.currentTimeMillis()));
                            if (elapsed > 0) {
                                double fps = 1E3 / Math.max(1, Math.min(elapsed, 10E3));
                                double mul = multiplier / (fps / 2);
                                if (mul >= 1) xOffset = (float) to;
                                else xOffset += (float) ((to - xOffset) * mul);
                            }
                        }
                    }
                }
            }
            if (crosshair != null) {
                if (righting) rightCrosshairPos = crosshair.subtract(camera.getPos());
                else leftCrosshairPos = crosshair.subtract(camera.getPos());
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

    @ModifyArg(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;loadProjectionMatrix(Lorg/joml/Matrix4f;)V"))
    public Matrix4f loadProjectionMatrix(Matrix4f projectionMatrix) {
        if (rendering && !client.options.hudHidden) {
            (righting ? rightMatrix : leftMatrix).identity().mul(projectionMatrix);
        }
        return projectionMatrix;
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/Camera;F)V"))
    public void calculateCrosshairAndMoveHand(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        if (rendering && !client.options.hudHidden) {
            // could be better, but I can't  -aMelonRind
            // seems like only the x is correct, y is jumping at a large range which is obviously wrong
            Vector4f vec = transform(righting ? rightCrosshairPos : leftCrosshairPos, matrices, righting ? rightMatrix : leftMatrix);
            if (righting) rightCrosshair = vec;
            else leftCrosshair = vec;
        }
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderFloatingItem(IIF)V"))
    public void splitFloatingItem(Args args) {
        if (enabled) {
            righting = false;
            renderFloatingItem(args.get(0), args.get(1), args.get(2));
            righting = true;
        }
    }

    @ModifyArg(method = "renderFloatingItem", index = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    public float moveFloatingItem(float x) {
        if (enabled) {
            float off = Stereopsis.getHudOffset(null);
            x += righting ? -off : off;
        }
        return x;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/toast/ToastManager;draw(Lnet/minecraft/client/gui/DrawContext;)V"))
    public DrawContext moveToast(DrawContext context) {
        Stereopsis.moveSideHud("toast", context, false, () -> client.getToastManager().draw(context));
        return context;
    }

    @Unique
    private Vector4f transform(@NotNull Vec3d offset, @NotNull MatrixStack stack, Matrix4f projection) {
        stack.push();
        stack.translate(offset.x, offset.y, offset.z);
        Vector4f vec = new Vector4f().mul(stack.peek().getPositionMatrix());
        vec.mul(projection);
        stack.pop();
        return vec.div(vec.w);
    }

}
