package io.github.amelonrind.stereopsis.mixin;

import io.github.amelonrind.stereopsis.Stereopsis;
import io.github.amelonrind.stereopsis.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.JumpingMount;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static io.github.amelonrind.stereopsis.Stereopsis.enabled;
import static io.github.amelonrind.stereopsis.Stereopsis.xOffset;
import static io.github.amelonrind.stereopsis.Stereopsis.leftCrosshair;
import static io.github.amelonrind.stereopsis.Stereopsis.rightCrosshair;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

    @Shadow @Final private MinecraftClient client;

    @Shadow @Final private ChatHud chatHud;

    @Shadow protected abstract void renderCrosshair(DrawContext context, RenderTickCounter tickCounter);

    @Shadow protected abstract void renderHotbar(DrawContext context, RenderTickCounter tickCounter);

    @Shadow protected abstract void renderStatusBars(DrawContext context);

    @Shadow protected abstract void renderMountHealth(DrawContext context);

    @Shadow protected abstract void renderMountJumpBar(JumpingMount mount, DrawContext context, int x);

    @Shadow protected abstract void renderExperienceBar(DrawContext context, int x);

    @Shadow protected abstract void renderHeldItemTooltip(DrawContext context);

    @Shadow protected abstract void renderScoreboardSidebar(DrawContext context, ScoreboardObjective objective);

    @Shadow protected abstract void renderOverlayMessage(DrawContext context, RenderTickCounter tickCounter);

    @Shadow protected abstract void renderStatusEffectOverlay(DrawContext context, RenderTickCounter tickCounter);

    @Shadow protected abstract void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter);

    @Unique private static boolean rendering = false;
    @Unique private static boolean righting = false;
    @Unique private static boolean wasPushed = false;
    @Unique private static float offset = 0.0f;

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void moveCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (enabled && !rendering) {
            ci.cancel();
            Profiler profiler = Profilers.get();
            profiler.push("stereopsis-crosshair");
            rendering = true;
            offset = context.getScaledWindowWidth() * (0.25f + (Config.get().flipView ? xOffset : -xOffset));

            righting = false;
            renderCrosshair(context, tickCounter);

            righting = true;
            renderCrosshair(context, tickCounter);

            rendering = false;
            profiler.pop();
        }
    }

    @ModifyArgs(method = "renderCrosshair", at = @At(value = "INVOKE", remap = false, target = "Lorg/joml/Matrix4fStack;translate(FFF)Lorg/joml/Matrix4f;"))
    private void move3DCrosshair(Args args) {
        if (rendering) {
            float x = args.get(0);
            float y = args.get(1);
            x += (righting ? -offset : offset);
            Vector3f crosshair = righting ? rightCrosshair : leftCrosshair;
            if (crosshair != null) {
                x += crosshair.x * client.getWindow().getScaledWidth() / 2;
                if (!Config.get().lockCrosshairY) {
                    y += crosshair.y * -client.getWindow().getScaledHeight() / 2;
                }
            }
            args.set(0, x);
            args.set(1, y);
        }
    }

    @Inject(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V", ordinal = 0))
    private void moveNormalCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (rendering) {
            float x = (righting ? -offset : offset);
            float y = 0;
            Vector3f crosshair = righting ? rightCrosshair : leftCrosshair;
            if (crosshair != null) {
                x += crosshair.x * context.getScaledWindowWidth() / 2;
                if (!Config.get().lockCrosshairY) {
                    y += crosshair.y * -context.getScaledWindowHeight() / 2;
                }
            }
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            wasPushed = true;
        }
    }

    @Inject(method = "renderCrosshair", at = @At("TAIL"))
    private void moveNormalCrosshairPop(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (wasPushed) {
            context.getMatrices().pop();
            wasPushed = false;
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void moveHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Stereopsis.moveHud("hotbar", context, ci, () -> renderHotbar(context, tickCounter));
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void moveStatusBars(DrawContext context, CallbackInfo ci) {
        Stereopsis.moveHud("status-bar", context, ci, () -> renderStatusBars(context));
    }

    @Inject(method = "renderMountHealth", at = @At("HEAD"), cancellable = true)
    private void moveMountHealth(DrawContext context, CallbackInfo ci) {
        Stereopsis.moveHud("mount-health", context, ci, () -> renderMountHealth(context));
    }

    @Inject(method = "renderMountJumpBar", at = @At("HEAD"), cancellable = true)
    private void moveMountJumpBar(JumpingMount mount, DrawContext context, int x, CallbackInfo ci) {
        Stereopsis.moveHud("mount-jump-bar", context, ci, () -> renderMountJumpBar(mount, context, x));
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void moveExperienceBar(DrawContext context, int x, CallbackInfo ci) {
        Stereopsis.moveHud("experience-bar", context, ci, () -> renderExperienceBar(context, x));
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void moveExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Stereopsis.moveHud("experience-bar", context, ci, () -> renderExperienceLevel(context, tickCounter));
    }

    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
    private void moveHeldItemTooltip(DrawContext context, CallbackInfo ci) {
        Stereopsis.moveHud("held-item-tooltip", context, ci, () -> renderHeldItemTooltip(context));
    }

    @Inject(method = "renderOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void moveActionBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Stereopsis.moveHud("overlay-actionbar", context, ci, () -> renderOverlayMessage(context, tickCounter));
    }

    @ModifyArgs(method = "renderChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V"))
    private void moveChatHud(Args args) {
        if (!enabled) return;
        DrawContext context = args.get(0);
        Stereopsis.moveSideHud("chat-hud", context, true, () -> chatHud.render(context, args.get(1), args.get(2), args.get(3), args.get(4)));
    }

    @ModifyArgs(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"))
    private void moveScoreboard(Args args) {
        DrawContext context = args.get(0);
        Stereopsis.moveSideHud("scoreboard", context, false, () -> renderScoreboardSidebar(context, args.get(1)));
    }

    @Unique private boolean renderingStatusEffects = false;

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"))
    private void moveStatusEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!renderingStatusEffects) {
            renderingStatusEffects = true;
            Stereopsis.moveSideHud("status-effects", context, false, () -> renderStatusEffectOverlay(context, tickCounter));
            renderingStatusEffects = false;
        }
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void cancelVignetteOverlay(DrawContext context, Entity entity, CallbackInfo ci) {
        if (enabled) ci.cancel();
    }

    @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void cancelSpyglassOverlay(DrawContext context, float scale, CallbackInfo ci) {
        if (enabled) ci.cancel();
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void cancelOverlay(DrawContext context, Identifier texture, float opacity, CallbackInfo ci) {
        if (enabled) ci.cancel();
    }

}
