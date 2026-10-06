#ifndef LUMENIZED_SHIMMER_FOG_GLSL
#define LUMENIZED_SHIMMER_FOG_GLSL

vec4 linear_fog(vec4 inColor, float vertexDistance, float fogStart, float fogEnd, vec4 fogColor, out float fogValue) {
    fogValue = 0.0;
    if (vertexDistance <= fogStart) {
        return inColor;
    }

    float fogRange = max(fogEnd - fogStart, 0.0001);
    fogValue = vertexDistance < fogEnd ? smoothstep(fogStart, fogStart + fogRange, vertexDistance) : 1.0;
    return vec4(mix(inColor.rgb, fogColor.rgb, fogValue * fogColor.a), inColor.a);
}

#endif // LUMENIZED_SHIMMER_FOG_GLSL
