package com.y271727uy.lumenized.forge.client;

import com.y271727uy.lumenized.Configuration;
import com.y271727uy.lumenized.LumenizedConstants;
import com.y271727uy.lumenized.ShimmerFields;
import com.y271727uy.lumenized.client.LumenizedRenderTypes;
import com.y271727uy.lumenized.client.auxiliaryScreen.Eyedropper;
import com.y271727uy.lumenized.client.auxiliaryScreen.HsbColorWidget;
import com.y271727uy.lumenized.client.light.LightCounter;
import com.y271727uy.lumenized.client.light.LightManager;
import com.y271727uy.lumenized.client.model.LumenizedMetadataSection;
import com.y271727uy.lumenized.client.postprocessing.PostProcessing;
import com.y271727uy.lumenized.client.shader.ReloadShaderManager;
import com.y271727uy.lumenized.client.shader.RenderUtils;
import com.y271727uy.lumenized.client.shader.ShaderSSBO;
import com.y271727uy.lumenized.comp.iris.ShaderpackInjections;
import com.y271727uy.lumenized.forge.CommonProxy;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.util.Objects;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author KilaBash
 * @date: 2022/05/02
 * @implNote com.y271727uy.lumenized.client.ClientProxy
 */
public class ClientProxy extends CommonProxy implements ResourceManagerReloadListener {

    public ClientProxy() {
        LightManager.injectShaders();
        PostProcessing.injectShaders();
        ShaderpackInjections.injectShaders();
    }

    @SubscribeEvent
    public void shaderRegistry(RegisterShadersEvent event) {
        ResourceProvider resourceProvider = event.getResourceProvider();
        try {
            event.registerShader(ReloadShaderManager.backupNewShaderInstance(resourceProvider, ResourceLocation.tryParse(LumenizedConstants.MOD_ID + ":" + "fast_blit"), DefaultVertexFormat.POSITION), shaderInstance -> RenderUtils.blitShader = shaderInstance);
            event.registerShader(ReloadShaderManager.backupNewShaderInstance(resourceProvider, Objects.requireNonNull(ResourceLocation.tryParse(LumenizedConstants.MOD_ID + ":" + "rendertype_armor_cutout_no_cull")), DefaultVertexFormat.NEW_ENTITY),
                    shaderInstance -> LumenizedRenderTypes.EmissiveArmorRenderType.emissiveArmorGlintShader = shaderInstance);
            event.registerShader(ReloadShaderManager.backupNewShaderInstance(resourceProvider,Objects.requireNonNull(ResourceLocation.tryParse(LumenizedConstants.MOD_ID + ":" + "hsb_block")), HsbColorWidget.HSB_VERTEX_FORMAT), shaderInstance -> HsbColorWidget.hsbShader = shaderInstance);
            if (ShaderSSBO.support()){
                event.registerShader(ReloadShaderManager.backupNewShaderInstance(resourceProvider, Objects.requireNonNull(ResourceLocation.tryParse(LumenizedConstants.MOD_ID + ":" + "pick_color")), DefaultVertexFormat.POSITION), Eyedropper.ShaderStorageBufferObject::setShader);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public void registerReloadableResourceManager(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(this);
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        Configuration.load();
        LightManager.INSTANCE.loadConfig();
        PostProcessing.loadConfig();
        LumenizedMetadataSection.onResourceManagerReload();
        LightManager.onResourceManagerReload();
        LumenizedMetadataSection.clearCache();
        for (PostProcessing postProcessing : PostProcessing.values()) {
            postProcessing.onResourceManagerReload(resourceManager);
        }
    }

    @SubscribeEvent
    public void registerKeyBinding(RegisterKeyMappingsEvent event) {
        event.register(ShimmerFields.recordScreenColor);
    }

    @SubscribeEvent
    public void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerBelowAll("screen_color_pick_overly", (forgeGui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
            Eyedropper.update(guiGraphics);
        });
        event.registerBelowAll("screen_shimmer_light_counter",(forgeGui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
            LightCounter.Render.update(guiGraphics);
        });
    }
}
