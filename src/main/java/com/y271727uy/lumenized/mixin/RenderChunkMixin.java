package com.y271727uy.lumenized.mixin;

import com.y271727uy.lumenized.client.light.ColorPointLight;
import com.y271727uy.lumenized.core.IRenderChunk;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collections;
import java.util.List;

/**
 * @author KilaBash
 * @date 2022/05/02
 * @implNote RenderChunkMixin, used to compile and save light info to the chunk.
 */
@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public abstract class RenderChunkMixin implements IRenderChunk {
    List<ColorPointLight> shimmerLights = Collections.emptyList();

    @Override
    public List<ColorPointLight> getShimmerLights() {
        return shimmerLights;
    }

    @Override
    public void setShimmerLights(List<ColorPointLight> lights) {
        shimmerLights = lights;
    }
}
