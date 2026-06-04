package com.y271727uy.lumenized.comp.iris;

import com.y271727uy.lumenized.LumenizedConstants;
import com.y271727uy.lumenized.client.shader.ShaderSSBO;
import com.y271727uy.lumenized.mixin.MixinPluginShared;
import com.y271727uy.lumenized.platform.Services;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public interface IrisHandle {
    @Nullable Pair<ShaderSSBO, ShaderSSBO> getBuffer();

    /**
     * @param buffers should be ShaderStorageBuffer[]
     */
    void updateInfo(Object buffers);

    /**
     * don't destroy ssbo buffer, it will be destroyed by iris since we delegate to it
     */
    void onSSBODestroyed();

    void setLightsIndex(int index);

    int getLightsIndex();

    void setEnvIndex(int index);

    int getEnvIndex();

    static <T> T make(Supplier<T> supplier) {
        return supplier.get();
    }

    IrisHandle INSTANCE = make(() -> {
        if (!MixinPluginShared.IS_IRIS_LOAD) return null;
        var platformName = Services.PLATFORM.getPlatformName();
        var classLocation = switch (platformName) {
            case "Fabric" -> "com.y271727uy.lumenized.fabric.compact.iris.FabricIrisHandle";
            case "Forge" -> "com.y271727uy.lumenized.forge.compat.oculus.OculusHandle";
            default -> throw new RuntimeException("unknown loader " + platformName);
        };
        IrisHandle handle = null;
        try {
            handle = (IrisHandle) Class.forName(classLocation).getConstructor().newInstance();
        } catch (Exception e) {
            LumenizedConstants.LOGGER.catching(e);
        }
        return handle;
    });

    boolean underShaderPack();

    boolean underShadowPass();

    static void analyzeShaderProperties(String shaderProperties) {
        shaderProperties.lines().forEach(str -> {
            var s = str.replaceAll(" ", "");
            if (s.indexOf(LumenizedConstants.SHIMMER_SHADERS_PROPERTIES_PREFIX) == 0) {
                var subsequent = s.replace(LumenizedConstants.SHIMMER_SHADERS_PROPERTIES_PREFIX,"");
                var analyze = subsequent.split("=");
                if (analyze.length == 2) {
                    try {
                        final int index = Integer.parseInt(analyze[1]);
                        switch (analyze[0]) {
                            case LumenizedConstants.SHIMMER_SHADERS_PROPERTIES_LIGHTS_IDENTIFIER -> {
                                LumenizedConstants.LOGGER.info("detect LIGHT BUFFER ssbo index:" + index);
                                INSTANCE.setLightsIndex(index);
                            }
                            case LumenizedConstants.SHIMMER_SHADERS_PROPERTIES_ENV_IDENTIFIER -> {
                                LumenizedConstants.LOGGER.info("detect ENV BUFFER ssbo index:" + index);
                                INSTANCE.setEnvIndex(index);
                            }
                        }
                    } catch (NumberFormatException ignored) {

                    }
                }
            }
        });
    }

    void bindWriteMain();

    int getCompositeId();
}
