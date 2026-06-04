package com.y271727uy.lumenized.mixin.rubidium;

import com.y271727uy.lumenized.client.light.ColorPointLight;
import com.y271727uy.lumenized.core.IRenderChunk;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collections;
import java.util.List;

/**
 * @author KilaBash
 * @date 2022/05/28
 * @implNote TODO
 */
@Mixin(RenderSection.class)
public abstract class RenderSectionMixin implements IRenderChunk {
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
