package io.github.amelonrind.stereopsis.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.amelonrind.stereopsis.Stereopsis;
import io.github.amelonrind.stereopsis.config.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.SubtitlesHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.ChatVisibility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.JumpingMount;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;
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

import static io.github.amelonrind.stereopsis.Stereopsis.enabled;
import static io.github.amelonrind.stereopsis.Stereopsis.xOffset;
import static io.github.amelonrind.stereopsis.Stereopsis.leftCrosshair;
import static io.github.amelonrind.stereopsis.Stereopsis.rightCrosshair;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

    @Shadow @Final private MinecraftClient client;

    @Shadow private int scaledWidth;

    @Shadow @Final private ChatHud chatHud;

    @Shadow @Final private SubtitlesHud subtitlesHud;

    @Shadow protected abstract void renderCrosshair(DrawContext context);

    @Shadow protected abstract void renderHotbar(float tickDelta, DrawContext context);

    @Shadow protected abstract void renderStatusBars(DrawContext context);

    @Shadow protected abstract void renderMountHealth(DrawContext context);

    @Shadow public abstract void renderMountJumpBar(JumpingMount mount, DrawContext context, int x);

    @Shadow public abstract void renderExperienceBar(DrawContext context, int x);

    @Shadow public abstract void renderHeldItemTooltip(DrawContext context);

    @Shadow protected abstract void drawTextBackground(DrawContext context, TextRenderer textRenderer, int yOffset, int width, int color);

    @Shadow protected abstract void renderScoreboardSidebar(DrawContext context, ScoreboardObjective objective);

    @Shadow protected abstract void renderStatusEffectOverlay(DrawContext context);

    @Unique private static boolean rendering = false;
    @Unique private static boolean righting = false;
    @Unique private static boolean wasPushed = false;
    @Unique private static float offset = 0.0f;

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void moveCrosshair(DrawContext context, CallbackInfo ci) {
        if (enabled && !rendering) {
            ci.cancel();
            client.getProfiler().push("stereopsis-crosshair");
            rendering = true;
            offset = scaledWidth * (0.25f + (Config.get().flipView ? xOffset : -xOffset));

            righting = false;
            renderCrosshair(context);

            righting = true;
            renderCrosshair(context);

            rendering = false;
            client.getProfiler().pop();
        }
    }

    @ModifyArg(method = "renderCrosshair", index = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private float move3DCrosshair(float x) {
        if (rendering) {
            x += (righting ? -offset : offset);
            Vector4f crosshair = righting ? rightCrosshair : leftCrosshair;
            if (crosshair != null) x += crosshair.x * scaledWidth / 2;
        }
        return x;
    }

    @Inject(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;blendFuncSeparate(Lcom/mojang/blaze3d/platform/GlStateManager$SrcFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DstFactor;Lcom/mojang/blaze3d/platform/GlStateManager$SrcFactor;Lcom/mojang/blaze3d/platform/GlStateManager$DstFactor;)V"))
    private void moveNormalCrosshair(DrawContext context, CallbackInfo ci) {
        if (rendering) {
            float off = (righting ? -offset : offset);
            Vector4f crosshair = righting ? rightCrosshair : leftCrosshair;
            if (crosshair != null) off += crosshair.x * scaledWidth / 2;
            context.getMatrices().push();
            context.getMatrices().translate(off, 0, 0);
            wasPushed = true;
        }
    }

    @Inject(method = "renderCrosshair", at = @At("TAIL"))
    private void moveNormalCrosshairPop(DrawContext context, CallbackInfo ci) {
        if (wasPushed) {
            context.getMatrices().pop();
            wasPushed = false;
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void moveHotbar(float tickDelta, DrawContext context, CallbackInfo ci) {
        Stereopsis.moveHud("hotbar", context, ci, () -> {
            RenderSystem.enableBlend();
            renderHotbar(tickDelta, context);
        });
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

    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
    private void moveHeldItemTooltip(DrawContext context, CallbackInfo ci) {
        Stereopsis.moveHud("held-item-tooltip", context, ci, () -> renderHeldItemTooltip(context));
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/gui/DrawContext;III)V"))
    private void moveChatHud(Args args) {
        if (!enabled || client.options.getChatVisibility().getValue() == ChatVisibility.HIDDEN || (client.currentScreen instanceof ChatScreen)) return;
        DrawContext context = args.get(0);
        Stereopsis.moveSideHud("chat-hud", context, true, () -> chatHud.render(context, args.get(1), args.get(2), args.get(3)));
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"))
    private void moveScoreboard(Args args) {
        DrawContext context = args.get(0);
        Stereopsis.moveSideHud("scoreboard", context, false, () -> renderScoreboardSidebar(context, args.get(1)));
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/SubtitlesHud;render(Lnet/minecraft/client/gui/DrawContext;)V"))
    private DrawContext moveSubtitle(DrawContext context) {
        Stereopsis.moveSideHud("subtitle", context, false, () -> subtitlesHud.render(context));
        return context;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;)V"))
    private DrawContext moveStatusEffects(DrawContext context) {
        Stereopsis.moveSideHud("status-effects", context, false, () -> renderStatusEffectOverlay(context));
        return context;
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

    @Unique private boolean renderingActionbar = false;
    @Unique private DrawContext context = null;

    @Inject(method = "render", at = @At(value = "INVOKE_STRING", args = "ldc=overlayMessage", target = "Lnet/minecraft/util/profiler/Profiler;push(Ljava/lang/String;)V"))
    private void onActionbarRender(DrawContext context, float tickDelta, CallbackInfo ci) {
        if (enabled) renderingActionbar = true;
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTextBackground(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;III)V"))
    private void renderActionbarBackground(Args args) {
        if (!renderingActionbar) return;
        context = args.get(0);
        Stereopsis.offsetHudPush(context, false);
        drawTextBackground(context, args.get(1), args.get(2), args.get(3), args.get(4));
        context.getMatrices().pop();
        Stereopsis.offsetHudPush(context, true);
    }

    @ModifyArgs(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I"))
    private void renderActionbarText(Args args) {
        if (!renderingActionbar) return;
        context.getMatrices().pop();
        Stereopsis.offsetHudPush(context, false);
        context.drawTextWithShadow(args.get(0), (Text) args.get(1), args.get(2), args.get(3), args.get(4));
        context.getMatrices().pop();
        Stereopsis.offsetHudPush(context, true);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V"))
    private void endActionbarRender(DrawContext context, float tickDelta, CallbackInfo ci) {
        if (renderingActionbar) {
            context.getMatrices().pop();
            renderingActionbar = false;
        }
    }

}
