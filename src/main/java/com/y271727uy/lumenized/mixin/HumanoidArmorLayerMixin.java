package com.y271727uy.lumenized.mixin;

import com.y271727uy.lumenized.client.postprocessing.PostProcessing;
import com.y271727uy.lumenized.client.shader.RenderUtils;
import com.y271727uy.lumenized.client.ResourceUtils;
import com.y271727uy.lumenized.client.LumenizedRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

/**
 * @author KilaBash
 * @date 2022/05/02
 * @implNote HumanoidArmorLayerMixin, used to inject emissive + bloom armor via custom resource pack.
 */
@Mixin(HumanoidArmorLayer.class)//FIXME
public abstract class HumanoidArmorLayerMixin {

    @Inject(method = "renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/Model;ILnet/minecraft/resources/ResourceLocation;)V", at = @At(value = "RETURN"))
    private void injectRenderModel(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, Model model, int dyeColor, ResourceLocation armorResource, CallbackInfo ci) {
        ResourceLocation bloomResource = Objects.requireNonNull(ResourceLocation.tryParse(armorResource.getNamespace() + ":" + armorResource.getPath().replace(".png", "_bloom.png")));
        if (ResourceUtils.isResourceExist(bloomResource)) {
            PoseStack finalStack = RenderUtils.copyPoseStack(poseStack);
            PostProcessing.BLOOM_UNITY.postEntity(sourceConsumer -> model.renderToBuffer(finalStack, sourceConsumer.getBuffer(LumenizedRenderTypes.emissiveArmor(bloomResource)), 0xF000F0, OverlayTexture.NO_OVERLAY));
        }
    }
}
