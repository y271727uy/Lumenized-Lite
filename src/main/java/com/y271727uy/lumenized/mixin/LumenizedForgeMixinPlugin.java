package com.y271727uy.lumenized.mixin;

import com.y271727uy.lumenized.core.MixinPluginShared;
import com.y271727uy.lumenized.LumenizedConstants;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * @author KilaBash
 * @date 2022/07/01
 * Added to stop Sodium mixins from trying to load if the mod is not installed. Prevents log spam of Mixin Errors
 */
public class LumenizedForgeMixinPlugin implements IMixinConfigPlugin , MixinPluginShared {

    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (IS_OPT_LOAD) return false;
		if (mixinClassName.contains("com.y271727uy.lumenized.mixin.sodium")) {
            if (IS_IRIS_LOAD && (mixinClassName.endsWith("ShaderChunkRendererMixin")
                    || mixinClassName.endsWith("GlProgramBuilderMixin")
                    || mixinClassName.endsWith("ShaderLoaderMixin"))) {
                LumenizedConstants.LOGGER.warn(
                        "Disabling incompatible Sodium program/Bloom binding hook while Iris is loaded: {}",
                        mixinClassName);
                return false;
            }
			return IS_SODIUM_LOAD;
        }
		if (mixinClassName.contains("com.y271727uy.lumenized.mixin.iris")) {
			return IS_IRIS_LOAD;
        }
        // When Iris is loaded, disable our reload shader system to avoid conflicts with Iris's program pipeline
		if (IS_IRIS_LOAD && mixinClassName.contains("reloadShader")) {
            return false;
        }
        // When Iris is loaded, it replaces the vanilla shader pipeline and conflicts with our ProgramMixin
		if (IS_IRIS_LOAD && mixinClassName.contains("ProgramMixin")) {
            return false;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

}
