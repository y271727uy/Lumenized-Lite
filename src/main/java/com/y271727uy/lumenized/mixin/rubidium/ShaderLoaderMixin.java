package com.y271727uy.lumenized.mixin.rubidium;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.y271727uy.lumenized.LumenizedConstants;
import com.y271727uy.lumenized.client.light.LightManager;
import com.y271727uy.lumenized.client.postprocessing.PostProcessing;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
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
    @ModifyExpressionValue(method = "loadShader",
            at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gl/shader/ShaderParser;parseShader(Ljava/lang/String;Lme/jellysquid/mods/sodium/client/gl/shader/ShaderConstants;)Ljava/lang/String;"))
    private static String transformShader(String shader, ShaderType type, ResourceLocation name) {
        if (name.getPath().contains("block_layer_opaque")) {
            try {
                if (type == ShaderType.FRAGMENT) {
                    shader = PostProcessing.embeddiumBloomMRTFSHInjection(shader);
                }
                if (type == ShaderType.VERTEX) {
                    shader = LightManager.embeddiumVVSHInjection(shader);
                }
            } catch (Exception e) {
                LumenizedConstants.LOGGER.error("Failed to inject embeddium shader for {}: {}", name, e.getMessage());
                // Return original shader on failure
            }
        }
        return shader;
    }
}
