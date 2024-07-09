package io.github.amelonrind.stereopsis.mixin.devgpupatch;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static io.github.amelonrind.stereopsis.Stereopsis.devGpuPatch;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void renderWorldStart(RenderTickCounter tickCounter, CallbackInfo ci) {
        devGpuPatch = true;
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void renderWorldEnd(RenderTickCounter tickCounter, CallbackInfo ci) {
        devGpuPatch = false;
    }

}
