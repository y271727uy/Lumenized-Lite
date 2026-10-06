package com.y271727uy.lumenized.mixin.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CompactChunkVertex.class, remap = false)
public abstract class CompactChunkVertexMixin {

    private static final ThreadLocal<Boolean> BLOOM_LIGHT = ThreadLocal.withInitial(() -> false);

    @Redirect(method = "lambda$getEncoder$0", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/impl/CompactChunkVertex;encodeLight(I)I"))
    private static int injectLightForBloom(int light) {
        boolean bloom = (light & 0x100) != 0;
        BLOOM_LIGHT.set(bloom);
        return bloom ? (15 | 15 << 4) : light;
    }

    @Redirect(method = "lambda$getEncoder$0", at = @At(value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/impl/CompactChunkVertex;packLightAndData(III)I"))
    private static int injectMaterialForBloom(int light, int materialBits, int section) {
        if (BLOOM_LIGHT.get()) {
            materialBits |= 0x01 << 4;
        }
        BLOOM_LIGHT.remove();
        return (light & 0xFFFF) | ((materialBits & 0xFF) << 16) | ((section & 0xFF) << 24);
    }
}
