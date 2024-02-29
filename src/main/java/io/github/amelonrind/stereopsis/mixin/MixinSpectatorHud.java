package io.github.amelonrind.stereopsis.mixin;

import io.github.amelonrind.stereopsis.Stereopsis;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.SpectatorHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpectatorHud.class)
public abstract class MixinSpectatorHud {

    @Shadow public abstract void renderSpectatorMenu(DrawContext context);

    @Shadow public abstract void render(DrawContext context);

    @Inject(method = "renderSpectatorMenu(Lnet/minecraft/client/gui/DrawContext;)V", at = @At("HEAD"), cancellable = true)
    private void moveSpectatorMenu(DrawContext context, CallbackInfo ci) {
        Stereopsis.moveHud("spectator-menu", context, ci, () -> renderSpectatorMenu(context));
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void move(DrawContext context, CallbackInfo ci) {
        Stereopsis.moveHud("spectator", context, ci, () -> render(context));
    }

}
