package com.y271727uy.lumenized.mixin;

import com.y271727uy.lumenized.client.model.LumenizedMetadataSection;
import com.y271727uy.lumenized.core.IBakedQuad;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BakedQuad.class)
public abstract class BakedQuadMixin implements IBakedQuad {
	private boolean bloom;

	@Inject(method = "<init>([IILnet/minecraft/core/Direction;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;ZZ)V", at = @At("RETURN"))
	private void pTintIndex(int[] pVertices, int pTintIndex, Direction pDirection, TextureAtlasSprite pSprite, boolean pShade, boolean hasAmbientOcclusion, CallbackInfo ci) {
		bloom = pTintIndex < -100 || LumenizedMetadataSection.isBloom(pSprite);
	}

	@Override
	public boolean isBloom() {
		return bloom;
	}

}