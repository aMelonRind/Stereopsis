package io.github.amelonrind.stereopsis.mixin.devgpupatch;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static io.github.amelonrind.stereopsis.Stereopsis.devGpuPatch;

@Mixin(value = VertexBuffer.class, priority = 69)
public class MixinVertexBuffer {

    @Inject(method = "draw(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/gl/ShaderProgram;)V", at = @At("HEAD"), cancellable = true)
    private void noop(Matrix4f viewMatrix, Matrix4f projectionMatrix, ShaderProgram program, CallbackInfo ci) {
        if (devGpuPatch) ci.cancel();
    }

}
