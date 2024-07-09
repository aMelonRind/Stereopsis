package io.github.amelonrind.stereopsis.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static io.github.amelonrind.stereopsis.Stereopsis.enabled;
import static io.github.amelonrind.stereopsis.Stereopsis.mc;

@Mixin(ChatHud.class)
public abstract class MixinChatHud {

    @Shadow public abstract boolean isChatFocused();

    @Shadow public abstract void reset();

    @Unique private int lastWidth = 0;
    @Unique private boolean notRefreshing = true;

    @Inject(method = "refresh", at = @At("HEAD"))
    private void onRefresh(CallbackInfo ci) {
        notRefreshing = false;
    }

    @Inject(method = "refresh", at = @At("TAIL"))
    private void afterRefresh(CallbackInfo ci) {
        notRefreshing = true;
    }

    @Inject(method = "getWidth()I", at = @At("RETURN"), cancellable = true)
    private void modifyWidth(CallbackInfoReturnable<Integer> cir) {
        int width = cir.getReturnValueI();
        if (enabled && !isChatFocused()) {
            int max = mc.getWindow().getScaledWidth() / 3;
            if (width > max) cir.setReturnValue(width = max);
        }
        if (width != lastWidth) {
            lastWidth = width;
            if (notRefreshing) reset();
            else notRefreshing = true; // this is safe... right?
        }
    }

}
