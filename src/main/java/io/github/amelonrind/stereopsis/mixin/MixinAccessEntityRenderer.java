package io.github.amelonrind.stereopsis.mixin;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityRenderer.class)
public interface MixinAccessEntityRenderer {

    @Accessor EntityRenderDispatcher getDispatcher();

}
