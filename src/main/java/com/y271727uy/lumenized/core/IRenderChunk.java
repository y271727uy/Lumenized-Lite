package com.y271727uy.lumenized.core;

import com.y271727uy.lumenized.client.light.ColorPointLight;

import java.util.List;

public interface IRenderChunk {
    List<ColorPointLight> getShimmerLights();
    void setShimmerLights(List<ColorPointLight> lights);
}
