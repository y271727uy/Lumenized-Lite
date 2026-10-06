package com.y271727uy.lumenized.mixin.sodium;

import net.caffeinemc.mods.sodium.client.gl.shader.GlProgram;
import net.caffeinemc.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

/**
 * Binds Lumenized's optional Bloom MRT output before Sodium links a chunk program.
 * The output is harmless when the shader does not declare bloomColor.
 */
@Mixin(GlProgram.Builder.class)
public abstract class GlProgramBuilderMixin {
    @Shadow
    public abstract GlProgram.Builder bindFragmentData(String name, int index);

    @Inject(method = "link", at = @At("HEAD"))
    private <U> void bindLumenizedBloomOutput(Function<ShaderBindingContext, U> interfaceFactory,
                                               CallbackInfoReturnable<GlProgram<U>> cir) {
        bindFragmentData("bloomColor", 1);
    }
}
