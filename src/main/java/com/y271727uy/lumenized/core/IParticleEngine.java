package com.y271727uy.lumenized.core;

import com.y271727uy.lumenized.client.postprocessing.PostProcessing;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;

public interface IParticleEngine {
    Particle createPostParticle(PostProcessing postProcessing, ParticleOptions pParticleData, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed);

}
