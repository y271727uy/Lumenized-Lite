package com.y271727uy.lumenized.mixin.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.y271727uy.lumenized.LumenizedConstants;
import com.y271727uy.lumenized.client.light.LightManager;
import com.y271727uy.lumenized.client.postprocessing.PostProcessing;
import com.y271727uy.lumenized.core.MixinPluginShared;
import net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @author KilaBash
 * @date 2022/05/28
 * @implNote ShaderLoaderMixin
 */
@Mixin(value = ShaderLoader.class,remap = false)
public abstract class ShaderLoaderMixin {

    @SuppressWarnings({"mapping","target","unresolvable-target"})
    @ModifyReturnValue(method = "getShaderSource", at = @At("RETURN"))
    private static String transformShader(String shader, ResourceLocation name) {
        if (name.getPath().contains("block_layer_opaque")) {
            try {
                if (name.getPath().endsWith(".vsh")) {
                    shader = LightManager.sodiumVVSHInjection(shader, !MixinPluginShared.IS_IRIS_LOAD);
                } else if (name.getPath().endsWith(".fsh") && !MixinPluginShared.IS_IRIS_LOAD) {
                    shader = PostProcessing.sodiumBloomMRTFSHInjection(shader);
                }
            } catch (Exception e) {
                LumenizedConstants.LOGGER.error("Failed to inject sodium shader for {}: {}", name, e.getMessage());
                // Return original shader on failure
            }
        }
        return shader;
    }
}
