package io.github.amelonrind.stereopsis.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static io.github.amelonrind.stereopsis.Stereopsis.screenAspectRatio;
import static io.github.amelonrind.stereopsis.Stereopsis.mc;

@Mixin(FishingBobberEntityRenderer.class)
public class MixinFishingBobberEntityRenderer {

    @Unique private static final float D2R = (float) Math.PI / 180;

    @Inject(method = "getHandPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getFov()Lnet/minecraft/client/option/SimpleOption;"), cancellable = true)
    private void fixPosition(PlayerEntity player, float f, float tickDelta, CallbackInfoReturnable<Vec3d> cir, @Local int i) {
        ClientPlayerEntity p = mc.player;
        assert p != null;
        EntityRenderDispatcher dispatcher = ((MixinAccessEntityRenderer) this).getDispatcher();
        double m = 960.0 / (double)dispatcher.gameOptions.getFov().getValue();
        if (player.preferredHand == Hand.MAIN_HAND && i < 0) f = 0;
        float dPitch = (player.getPitch(tickDelta) - MathHelper.lerp(tickDelta, p.lastRenderPitch, p.renderPitch)) * 0.1F * D2R;
        float dYaw = (player.getYaw(tickDelta) - MathHelper.lerp(tickDelta, p.lastRenderYaw, p.renderYaw)) % 360 * 0.1F;
        if (dYaw > 18) dYaw -= 36;
        else if (dYaw < -18) dYaw += 36;
        dYaw *= D2R;
        MixinAccessHeldItemRenderer fpr = (MixinAccessHeldItemRenderer) mc.gameRenderer.firstPersonRenderer;
        float progress = i > 0 ? fpr.getEquipProgressMainHand() : fpr.getEquipProgressOffHand();
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d vec3d = dispatcher.camera.getProjection().getPosition( // ah yes, magic numbers
                (float) (i * (1.125 / screenAspectRatio) * (1 - f * 1.04)),
                (float) (-1.1 + progress - 0.5 * f)
        ).multiply(m).rotateY(dYaw).rotateX(dPitch);
        Vec3d camXZ = player.getCameraPosVec(tickDelta);
        float eyeH = MathHelper.lerp(tickDelta, ((MixinAccessCamera) cam).getLastCameraY(), ((MixinAccessCamera) cam).getCameraY());
        double realY = camXZ.y - player.getStandingEyeHeight() + eyeH;
        cir.setReturnValue(new Vec3d(camXZ.x, realY, camXZ.z).add(vec3d));
    }

}
