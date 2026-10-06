package com.y271727uy.lumenized.mixin.iris;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.y271727uy.lumenized.comp.iris.ShaderpackInjection;
import net.irisshaders.iris.shaderpack.include.AbsolutePackPath;
import net.irisshaders.iris.shaderpack.programs.ProgramSet;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

@Mixin(ProgramSet.class)
public abstract class ProgramSetMixin {
    private static final String READ_PROGRAM_SOURCE =
            "readProgramSource(Lnet/irisshaders/iris/shaderpack/include/AbsolutePackPath;"
                    + "Ljava/util/function/Function;Ljava/lang/String;"
                    + "Lnet/irisshaders/iris/shaderpack/programs/ProgramSet;"
                    + "Lnet/irisshaders/iris/shaderpack/properties/ShaderProperties;"
                    + "Lnet/irisshaders/iris/gl/blending/BlendModeOverride;Z)"
                    + "Lnet/irisshaders/iris/shaderpack/programs/ProgramSource;";

    @ModifyExpressionValue(
            method = READ_PROGRAM_SOURCE,
            at = @At(value = "INVOKE",
                    target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;",
                    ordinal = 0),
            remap = false)
    private static Object injectShaderpackVsh(Object value, AbsolutePackPath directory,
                                               Function<AbsolutePackPath, String> sourceProvider,
                                               String program, ProgramSet programSet,
                                               ShaderProperties properties) {
        if ("gbuffers_terrain".equals(program) && value instanceof String vsh) {
            return ShaderpackInjection.TERRAIN.injectTerrainVsh(vsh);
        }
        return value;
    }

    @ModifyExpressionValue(
            method = READ_PROGRAM_SOURCE,
            // Iris reads vsh, gsh, tcs, tes, then fsh in this overload.
            at = @At(value = "INVOKE",
                    target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;",
                    ordinal = 4),
            remap = false)
    private static Object injectShaderpackFsh(Object value, AbsolutePackPath directory,
                                               Function<AbsolutePackPath, String> sourceProvider,
                                               String program, ProgramSet programSet,
                                               ShaderProperties properties) {
        if ("gbuffers_terrain".equals(program) && value instanceof String fsh) {
            return ShaderpackInjection.TERRAIN.injectTerrainFsh(fsh);
        }
        return value;
    }
}
