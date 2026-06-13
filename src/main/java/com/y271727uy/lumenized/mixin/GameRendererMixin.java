package com.y271727uy.lumenized.mixin;

import com.y271727uy.lumenized.client.light.LightManager;
import com.y271727uy.lumenized.client.postprocessing.PostProcessing;
import com.y271727uy.lumenized.platform.Services;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author KilaBash
 * @date 2022/05/02
 * @implNote GameRendererMixin, used to refresh shader and fbo size.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	@Inject(method = "resize", at = @At(value = "RETURN"))
    private void injectResize(int width, int height, CallbackInfo ci) {
        PostProcessing.resize(width, height);
    }

    @Inject(method = "reloadShaders", at = @At(value = "RETURN"))
    private void injectReloadShaders(ResourceProvider pResourceManager, CallbackInfo ci) {
        if (Services.PLATFORM.isLoadingStateValid()) {
            LightManager.INSTANCE.reloadShaders();
        }
    }
}
