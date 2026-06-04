package com.y271727uy.lumenized.mixin.oculus;

import com.y271727uy.lumenized.LumenizedConstants;
import net.irisshaders.iris.gl.shader.StandardMacros;
import net.irisshaders.iris.helpers.StringPair;
import com.google.common.collect.ImmutableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StandardMacros.class, remap = false)
public abstract class StandardMacrosMixin {

    @Inject(method = "createStandardEnvironmentDefines",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void injectMacro(CallbackInfoReturnable<ImmutableList<StringPair>> cir) {
        ImmutableList<StringPair> original = cir.getReturnValue();
        cir.setReturnValue(ImmutableList.<StringPair>builder()
                .addAll(original)
                .add(new StringPair(LumenizedConstants.SHIMMER_IDENTIFIER_MACRO, "true"))
                .build());
    }
}
