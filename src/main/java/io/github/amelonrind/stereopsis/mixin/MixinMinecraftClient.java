package io.github.amelonrind.stereopsis.mixin;

import io.github.amelonrind.stereopsis.Stereopsis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static io.github.amelonrind.stereopsis.Stereopsis.skipNextTick;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Inject(method = "getFramebuffer", at = @At("HEAD"), cancellable = true)
    public void overrideFramebuffer(CallbackInfoReturnable<Framebuffer> cir) {
        if (Stereopsis.framebufferOverride != null) cir.setReturnValue(Stereopsis.framebufferOverride);
    }

    @Unique private boolean overrideTick = false;
    @ModifyArg(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;render(Z)V"))
    public boolean skipTick(boolean tick) {
        if (skipNextTick) {
            overrideTick = tick;
            return false;
        } else {
            overrideTick = false;
            return tick;
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;render(FJZ)V"))
    public boolean dontSkipRender(boolean tick) {
        if (overrideTick) {
            overrideTick = false;
            return true;
        }
        return tick;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Mouse;updateMouse()V"))
    public void skipMouse(Mouse instance) {
        if (skipNextTick) {
            skipNextTick = false;
            return;
        }
        instance.updateMouse();
    }

}
