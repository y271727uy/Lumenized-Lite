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
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import org.jetbrains.annotations.NotNull;

/**
 * @author KilaBash
 * @date: 2022/05/02
 * @implNote com.y271727uy.lumenized.client.ClientProxy
 */
@EventBusSubscriber(modid = LumenizedConstants.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientProxy extends CommonProxy implements ResourceManagerReloadListener {
    private static ClientProxy INSTANCE;

    public ClientProxy() {
        INSTANCE = this;
        LightManager.injectShaders();
        PostProcessing.injectShaders();
        ShaderpackInjections.injectShaders();
    }

    @SubscribeEvent
    public static void shaderRegistry(RegisterShadersEvent event) {
        LumenizedConstants.LOGGER.info("Registering Lumenized shader instances");
        ResourceProvider resourceProvider = event.getResourceProvider();
        try {
            event.registerShader(ReloadShaderManager.backupNewShaderInstance(resourceProvider, ResourceLocation.tryParse(LumenizedConstants.MOD_ID + ":" + "fast_blit"), DefaultVertexFormat.POSITION), shaderInstance -> {
                RenderUtils.blitShader = shaderInstance;
                LumenizedConstants.LOGGER.info("Lumenized fast_blit shader loaded");
            });
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
    public static void registerReloadableResourceManager(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(INSTANCE);
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
    public static void registerKeyBinding(RegisterKeyMappingsEvent event) {
        event.register(ShimmerFields.recordScreenColor);
    }

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiLayersEvent event) {
        event.registerBelowAll(ResourceLocation.fromNamespaceAndPath(LumenizedConstants.MOD_ID, "screen_color_pick_overlay"), (guiGraphics, deltaTracker) -> {
            Eyedropper.update(guiGraphics);
        });
        event.registerBelowAll(ResourceLocation.fromNamespaceAndPath(LumenizedConstants.MOD_ID, "screen_shimmer_light_counter"),(guiGraphics, deltaTracker) -> {
            LightCounter.Render.update(guiGraphics);
        });
    }
}
