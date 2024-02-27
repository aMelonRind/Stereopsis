package io.github.amelonrind.stereopsis.mixin.devgpupatch;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static io.github.amelonrind.stereopsis.Stereopsis.devGpuPatch;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void renderWorldStart(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        devGpuPatch = true;
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void renderWorldEnd(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        devGpuPatch = false;
    }

}
