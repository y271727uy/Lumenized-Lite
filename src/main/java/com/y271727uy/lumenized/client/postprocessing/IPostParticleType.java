package com.y271727uy.lumenized.client.postprocessing;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author KilaBash
 * @date 2022/05/09
 * @implNote TODO
 */
public interface IPostParticleType extends ParticleRenderType {

    ParticleRenderType getParent();
    PostProcessing getPost();

    @Override
    default BufferBuilder begin(@NotNull Tesselator pTesselator, @NotNull TextureManager pTextureManager) {
        return getParent().begin(pTesselator, pTextureManager);
    }
}
