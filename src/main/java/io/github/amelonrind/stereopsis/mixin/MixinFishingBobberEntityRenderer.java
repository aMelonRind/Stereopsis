package io.github.amelonrind.stereopsis.mixin;

import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static io.github.amelonrind.stereopsis.Stereopsis.screenAspectRatio;

@Mixin(FishingBobberEntityRenderer.class)
public class MixinFishingBobberEntityRenderer {

    @Unique private static final float loc = 0.525f * 16 / 9;

    @ModifyArg(method = "render(Lnet/minecraft/entity/projectile/FishingBobberEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", index = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera$Projection;getPosition(FF)Lnet/minecraft/util/math/Vec3d;"))
    private float fixPosition(float x) {
        return Math.signum(x) * loc / screenAspectRatio;
    }

}
