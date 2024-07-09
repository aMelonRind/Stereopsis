package io.github.amelonrind.stereopsis.mixin;

import io.github.amelonrind.stereopsis.Stereopsis;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.SubtitlesHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static io.github.amelonrind.stereopsis.Stereopsis.enabled;

@Mixin(SubtitlesHud.class)
public abstract class MixinSubtitlesHud {

    @Shadow public abstract void render(DrawContext context);

    @Unique private boolean rendering = false;

    @Inject(method = "render", at = @At("HEAD"))
    private void moveSubtitle(DrawContext context, CallbackInfo ci) {
        if (!enabled || rendering) return;
        rendering = true;
        Stereopsis.moveSideHud("subtitle", context, false, () -> render(context));
        rendering = false;
    }

}
