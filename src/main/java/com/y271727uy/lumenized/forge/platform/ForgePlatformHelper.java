package com.y271727uy.lumenized.forge.platform;

import com.y271727uy.lumenized.event.LumenizedLoadConfigEvent;
import com.y271727uy.lumenized.event.LumenizedReloadEvent;
import com.y271727uy.lumenized.LumenizedConstants;
import com.y271727uy.lumenized.forge.LumenizedForgeConfig;
import com.y271727uy.lumenized.client.postprocessing.PostParticle;
import com.y271727uy.lumenized.client.postprocessing.PostProcessing;
import com.y271727uy.lumenized.platform.services.IPlatformHelper;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.particle.Particle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.nio.file.Path;
import java.util.List;

/**
 * @author HypherionSA
 * @date 2022/06/09
 */
public class ForgePlatformHelper implements IPlatformHelper {

	@Override
	public String getPlatformName() {
		return "NeoForge";
	}

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public List<String> getLoadedMods() {
		return ModList.get().getMods().stream().map(IModInfo::getModId).toList();
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return !FMLLoader.isProduction();
	}

	@Override
	public boolean isStencilEnabled(RenderTarget target) {
		return target.isStencilEnabled();
	}

	@Override
	public boolean useCombinedDepthStencilAttachment() {
		return false;
	}

	@Override
	public void enableStencil(RenderTarget renderTarget) {
		renderTarget.enableStencil();
	}

	@Override
	public int getUniformBufferObjectOffset() {
		int configValue = LumenizedForgeConfig.getUboOffset().get();
		if (configValue == -1) {
			if (ModList.get().isLoaded("modernui")) {
				return 6;
			} else {
				return 1;
			}
		} else {
			return Mth.clamp(configValue, 0, GL11.glGetInteger(GL31.GL_MAX_UNIFORM_BUFFER_BINDINGS) - 1);
		}
	}

	@Override
	public boolean useBlockBloom() {
		return LumenizedForgeConfig.getBlockBloom().get();
	}

	@Override
	public boolean useLightMap() {
		return LumenizedForgeConfig.getLightMap().get();
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public PostParticle createPostParticle(Particle parent, PostProcessing postProcessing) {
		return new PostParticle(parent, postProcessing) {
		};

	}

	@Override
	public boolean isLoadingStateValid() {
        return true;
	}

	@Override
	public boolean isColoredLightEnable() {
		return LumenizedForgeConfig.getColoredLightEnable().get();
	}

	@Override
	public boolean isBloomEnable() {
		return LumenizedForgeConfig.getBloomEnable().get();
	}

	@Override
	public boolean isAdditiveBlend() {
		return LumenizedForgeConfig.getAdditiveBlend().get();
	}

	@Override
	public LumenizedLoadConfigEvent postLoadConfigurationEvent(LumenizedLoadConfigEvent event) {
		return event;
	}

	@Override
	public LumenizedReloadEvent postReloadEvent(LumenizedReloadEvent event){
		return event;
	}

    @Override
    public int getBloomColorAttachmentNumber() {
        // Attachment 1 is required for the bloom MRT.  Some drivers expose
        // only one draw buffer; never submit an invalid enum in that case.
        int maxDrawBuffers = GL11.glGetInteger(GL30.GL_MAX_DRAW_BUFFERS);
        if (maxDrawBuffers < 2) {
            LumenizedConstants.LOGGER.warn("Bloom MRT disabled: GL_MAX_DRAW_BUFFERS={}", maxDrawBuffers);
            return GL30.GL_COLOR_ATTACHMENT0;
        }
        return GL30.GL_COLOR_ATTACHMENT1;
    }

    @Override
    public boolean isEnableInsetShaderInfo() {
        //TODO config hasn't load when we need it, need change
        return isDevelopmentEnvironment();
    }

	@Override
	public ResourceLocation getFluidTextureLocation(Fluid fluid, boolean isStill) {
		IClientFluidTypeExtensions fluidTypeExtensions = IClientFluidTypeExtensions.of(fluid);
		return isStill ? fluidTypeExtensions.getStillTexture() : fluidTypeExtensions.getFlowingTexture();
	}

	@Override
	public int getFluidColor(Fluid fluid) {
		return IClientFluidTypeExtensions.of(fluid).getTintColor();
	}

	@Override
	public Path getConfigDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public boolean isRenderDocEnable() {
		//TODO config hasn't load when we need it, need change
		if (FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL)) return false;
		return isDevelopmentEnvironment();
	}

	@Override
	public boolean enableBuildinSetting() {
		return LumenizedForgeConfig.getEnableBuildinSetting().get();
	}

}
