package io.github.amelonrind.stereopsis.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class ConfigPluginImpl implements IMixinConfigPlugin {

    // too lazy to do stereopsis.mixin.properties or something
    private static final boolean doDevGpuPatch = FabricLoader.getInstance().getConfigDir()
            .resolve("enable_dev_gpu_patch").toFile().exists();

    static {
        if (doDevGpuPatch) System.out.println("Enabled gpu patch on Stereopsis.");
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("io.github.amelonrind.stereopsis.mixin.devgpupatch.")) {
            return doDevGpuPatch;
        }
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

}
