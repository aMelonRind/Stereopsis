package io.github.amelonrind.stereopsis.mixin;

import io.github.amelonrind.stereopsis.Stereopsis;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {

    @Inject(method = "getFramebuffer", at = @At("HEAD"), cancellable = true)
    public void overrideFramebuffer(CallbackInfoReturnable<Framebuffer> cir) {
        if (Stereopsis.framebufferOverride != null) cir.setReturnValue(Stereopsis.framebufferOverride);
    }

}
