package io.github.amelonrind.stereopsis.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static io.github.amelonrind.stereopsis.Stereopsis.enabled;

@Mixin(ChatHud.class)
public abstract class MixinChatHud {

    @Shadow protected abstract boolean isChatFocused();

    @Shadow public abstract void reset();

    @Unique private DrawContext context = null;
    @Unique private int lastWidth = 1;

    @Inject(method = "render", at = @At("HEAD"))
    public void updateContext(DrawContext context, int currentTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (enabled) this.context = context;
    }

    @Inject(method = "getWidth()I", at = @At("RETURN"), cancellable = true)
    public void modifyWidth(CallbackInfoReturnable<Integer> cir) {
        int width = cir.getReturnValueI();
        if (enabled && !isChatFocused() && context != null) {
            int max = context.getScaledWindowWidth() / 3;
            if (width > max) cir.setReturnValue(width = max);
        }
        if (width != lastWidth) {
            lastWidth = width;
            reset();
        }
    }

}
