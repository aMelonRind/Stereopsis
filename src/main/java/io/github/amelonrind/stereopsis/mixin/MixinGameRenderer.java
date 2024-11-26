package io.github.amelonrind.stereopsis.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.amelonrind.stereopsis.StereopsisFramebufferSet;
import io.github.amelonrind.stereopsis.config.Config;
import io.github.amelonrind.stereopsis.Stereopsis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Pool;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
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

import static io.github.amelonrind.stereopsis.StereopsisFramebufferSet.back;
import static io.github.amelonrind.stereopsis.StereopsisFramebufferSet.cache;
import static io.github.amelonrind.stereopsis.StereopsisFramebufferSet.left;
import static io.github.amelonrind.stereopsis.StereopsisFramebufferSet.right;
import static io.github.amelonrind.stereopsis.StereopsisFramebufferSet.flip;

@Mixin(value = GameRenderer.class, priority = 950)
public abstract class MixinGameRenderer {

    @Shadow @Final private MinecraftClient client;
    @Shadow @Final private Camera camera;
    @Shadow @Nullable private Identifier postProcessorId;
    @Shadow @Final private Pool pool;

    @Shadow protected abstract void renderFloatingItem(DrawContext context, float tickDelta);

    @Unique private static final double PI2 = Math.PI / 2;
    @Unique private static final double D2R = Math.PI / 180;
    @Unique private static final float eyeRadius = 0.1f;
    @Unique private static long lastFrameTime = 0L;

    @Unique private static Vec3d crosshair = null;
    @Unique private static Vec3d leftCrosshairPos = new Vec3d(0, 0, 0);
    @Unique private static Vec3d rightCrosshairPos = new Vec3d(0, 0, 0);

    @Inject(method = "close", at = @At("TAIL"))
    private void clearPrograms(CallbackInfo ci) {
        StereopsisFramebufferSet.clear();
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void renderStereopsis(GameRenderer instance, RenderTickCounter renderTickCounter, Operation<Void> original) {
        if (!enabled) {
            original.call(instance, renderTickCounter);
            return;
        }
        Stereopsis.resetHudOffset();
        Config cfg = Config.get();
        boolean doMagic = cfg.magicFixForShaders;

        StereopsisFramebufferSet.setFlip(cfg.flipView);

        rendering = true;
        Profiler profiler = Profilers.get();
        profiler.push("stereopsis-world");
        crosshair = null;
        leftCrosshair = null;
        rightCrosshair = null;

        profiler.push("blit");
        back.clear();
        blit(client.getFramebuffer(), back);

        renderSide("left", doMagic ? cache : left, false, () -> original.call(instance, renderTickCounter));
        if (doMagic) { // this fixes iris shader glitches and idk why
            blit(cache, left);
            cache.clear();
        }
        renderSide("right", right, true, () -> original.call(instance, renderTickCounter));

        Framebuffer outlines = client.worldRenderer.getEntityOutlinesFramebuffer();
        if (outlines != null) outlines.clear();
        profiler.swap("render");
//        if (doMagic) blit(cache, left); // before 1.21.2
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        StereopsisFramebufferSet.render(pool);
        RenderSystem.enableCull();

        profiler.pop();
        profiler.pop();
        rendering = false;
    }

    @Unique
    private void renderSide(String name, @NotNull Framebuffer frame, boolean right, Runnable render) {
        Profilers.get().swap(name);
        frame.clear();
        blit(back, frame);
        Stereopsis.framebufferOverride = frame;
        frame.beginWrite(false);
        righting = right;
        render.run();
        client.worldRenderer.drawEntityOutlinesFramebuffer();
        frame.endWrite();
        Stereopsis.framebufferOverride = null;
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/render/Camera;update(Lnet/minecraft/world/BlockView;Lnet/minecraft/entity/Entity;ZZF)V"))
    private void shiftCamera(RenderTickCounter tickCounter, CallbackInfo ci) {
        if (enabled) {
            ((MixinAccessCamera) camera).callMoveBy(0, 0, righting ? eyeRadius : -eyeRadius);
            if (!righting) {
                double to = 0.0;
                Config cfg = Config.get();
                if (client.world != null && (client.cameraEntity != null || client.player != null)) {
                    Entity cam = client.cameraEntity != null ? client.cameraEntity : client.player;
                    float tickDelta = tickCounter.getTickDelta(true);
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
                                    cfg.maxXOffset
                            );
                        }
                    }
                    if (client.crosshairTarget == null || client.crosshairTarget.getType() == HitResult.Type.MISS) {
                        crosshair = res.getPos();
                    } else {
                        crosshair = client.crosshairTarget.getPos();
                    }
                }
                float multiplier = cfg.offsetSpeed;
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

    @Inject(method = "onResized", at = @At("TAIL"))
    private void onResized(int width, int height, CallbackInfo ci) {
        screenAspectRatio = (float) width / height;
        StereopsisFramebufferSet.resize(width, height);
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

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderHand(Lnet/minecraft/client/render/Camera;FLorg/joml/Matrix4f;)V"))
    private void calculateCrosshair(RenderTickCounter tickCounter, CallbackInfo ci, @Local(ordinal = 0) Matrix4f projection, @Local(ordinal = 2) Matrix4f position) {
        if (rendering && !client.options.hudHidden) {
            Vector3f vec = (righting ? rightCrosshairPos : leftCrosshairPos)
                    .toVector3f().mulPosition(position).mulProject(projection);
            if (righting ^ flip) rightCrosshair = vec;
            else leftCrosshair = vec;
        }
    }

    @Unique private boolean fItemSide = false;
    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderFloatingItem(Lnet/minecraft/client/gui/DrawContext;F)V"))
    private void splitFloatingItem(Args args) {
        if (enabled) {
            fItemSide = false;
            renderFloatingItem(args.get(0), args.get(1));
            fItemSide = true;
        }
    }

    @ModifyArg(method = "renderFloatingItem", index = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private float moveFloatingItem(float x) {
        if (enabled) {
            float off = Stereopsis.getHudOffset();
            x += fItemSide ? -off : off;
        }
        return x;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/toast/ToastManager;draw(Lnet/minecraft/client/gui/DrawContext;)V"))
    private DrawContext moveToast(DrawContext context) {
        Stereopsis.moveSideHud("toast", context, false, () -> client.getToastManager().draw(context));
        return context;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;postProcessorEnabled:Z"))
    private boolean onPostEffect(boolean original) {
        assert postProcessorId != null; // already checked before the mixin target
        return original && !(enabled && postProcessorId.getPath().equals("spider"));
    }

}
