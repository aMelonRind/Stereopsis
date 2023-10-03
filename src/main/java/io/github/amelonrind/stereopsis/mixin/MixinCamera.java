package io.github.amelonrind.stereopsis.mixin;

import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface MixinCamera {

    @Invoker void callMoveBy(double x, double y, double z);

}
