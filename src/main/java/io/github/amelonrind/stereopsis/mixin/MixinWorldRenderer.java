package io.github.amelonrind.stereopsis.mixin;

import io.github.amelonrind.stereopsis.StereopsisFramebufferSet;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Inject(method = "reload(Lnet/minecraft/resource/ResourceManager;)V", at = @At("RETURN"))
    private void onReload(CallbackInfo ci) {
        StereopsisFramebufferSet.reload();
    }

    @Inject(method = "reload()V", at = @At("RETURN"))
    private void onReload2(CallbackInfo ci) {
        StereopsisFramebufferSet.reload();
    }

}
