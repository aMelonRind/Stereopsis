package io.github.amelonrind.stereopsis.mixin;

import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(PostEffectProcessor.class)
public interface MixinAccessPostEffectProcessor {

    @Accessor List<PostEffectPass> getPasses();

}
