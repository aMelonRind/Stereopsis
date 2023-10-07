package io.github.amelonrind.stereopsis.mixin;

import net.minecraft.client.gl.PostEffectProcessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static io.github.amelonrind.stereopsis.Stereopsis.enabled;

@Mixin(PostEffectProcessor.class)
public class MixinPostEffectProcessor {

    @Shadow @Final private String name;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void blockSpiderPostShader(float tickDelta, CallbackInfo ci) {
        if (enabled && name.equals("minecraft:shaders/post/spider.json")) ci.cancel();
    }

}
