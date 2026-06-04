package com.y271727uy.lumenized.mixin;

import com.y271727uy.lumenized.core.IGlslProcessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(targets = "net.minecraft.client.renderer.ShaderInstance$1")
public class ShaderInstanceGlslProcessorMixin implements IGlslProcessor {
    @Final
    @Shadow(remap = false)
    private Set<String> importedPaths;

    public void shimmer$clearImportedPathRecord() {
        importedPaths.clear();
    }

}
